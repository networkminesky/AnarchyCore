package anarchy.minesky.core.impl.guard.fog;

import anarchy.minesky.core.impl.locator.LocatorManager;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import com.destroystokyo.paper.ClientOption;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FogOfWarManager {

    private final Plugin plugin;
    private final FogSettings settings;
    private final LocatorManager locator;
    private final Map<UUID, FogSnapshot> snapshots = new ConcurrentHashMap<>();
    private final Map<PairKey, FogPairState> pairs = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Integer>> hidden = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private final Set<PairKey> tagHold = ConcurrentHashMap.newKeySet();
    private final Map<PairKey, Integer> tagEpoch = new ConcurrentHashMap<>();
    private final FogNametagIntegration tagsNone = FogNametagIntegration.NONE;
    private volatile FogNametagIntegration tags = tagsNone;
    private volatile boolean tagsAttached;
    private FogPacketListener packetListener;

    public FogOfWarManager(Plugin plugin, FogSettings settings, LocatorManager locator) {
        this.plugin = plugin;
        this.settings = settings;
        this.locator = locator;
    }

    public FogSettings settings() {
        return settings;
    }

    public void start() {
        if (!settings.enabled() || PacketEvents.getAPI() == null) {
            return;
        }
        packetListener = new FogPacketListener(this);
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);
        attachTags();
        for (Player player : Bukkit.getOnlinePlayers()) {
            track(player);
        }
        plugin.getLogger().info("[AnarchyGuard] Fog of War ativo (" + settings.strategy() + ").");
    }

    public void reload() {
        stop();
        settings.reload();
        start();
    }

    public void stop() {
        if (packetListener != null && PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
            packetListener = null;
        }
        for (ScheduledTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearObserver(player);
        }
        detachTags();
        snapshots.clear();
        pairs.clear();
        hidden.clear();
    }

    public void attachTags() {
        if (tagsAttached || !settings.enabled()) {
            return;
        }
        FogNametagIntegration opened = FogNametagIntegration.open(plugin, this);
        if (opened == tagsNone) {
            return;
        }
        tags = opened;
        tags.start();
        tagsAttached = true;
    }

    public void detachTags() {
        tags.stop();
        tags = tagsNone;
        tagsAttached = false;
        tagHold.clear();
        tagEpoch.clear();
    }

    /**
     * Filtro do AnarchyTags. {@code tagHold} continua verdadeiro por alguns ticks
     * depois do respawn para o AnarchyTags despawnar o text display (o DESTROY do
     * jogador leva o passageiro junto) antes de montar a tag de novo.
     */
    public boolean allowsExternalNametag(Player viewer, Player target) {
        if (viewer == null || target == null) {
            return false;
        }
        if (isHidden(viewer.getUniqueId(), target.getEntityId())) {
            return false;
        }
        return !tagHold.contains(new PairKey(viewer.getUniqueId(), target.getUniqueId()));
    }

    public void track(Player player) {
        if (!settings.enabled() || player == null) {
            return;
        }
        UUID id = player.getUniqueId();
        FogSnapshot snapshot = snapshots.computeIfAbsent(id, FogSnapshot::new);
        writeSnapshot(player, snapshot);

        ScheduledTask existing = tasks.remove(id);
        if (existing != null) {
            existing.cancel();
        }

        if (settings.worldDisabled(snapshot.worldName)) {
            revealAll(player);
            return;
        }

        tasks.put(
                id,
                player.getScheduler().runAtFixedRate(
                        plugin,
                        task -> {
                            if (!player.isOnline()) {
                                task.cancel();
                                tasks.remove(id);
                                return;
                            }
                            tick(player);
                        },
                        null,
                        1L,
                        settings.checkIntervalTicks()));
    }

    public void forget(Player player) {
        if (player == null) {
            return;
        }
        UUID id = player.getUniqueId();
        ScheduledTask task = tasks.remove(id);
        if (task != null) {
            task.cancel();
        }
        int entityId = player.getEntityId();
        int nametagId = nametagId(entityId);
        snapshots.remove(id);
        hidden.remove(id);
        pairs.keySet().removeIf(key -> key.observer().equals(id) || key.target().equals(id));
        tagHold.removeIf(key -> key.observer().equals(id) || key.target().equals(id));
        tagEpoch.keySet().removeIf(key -> key.observer().equals(id) || key.target().equals(id));
        for (Map.Entry<UUID, Set<Integer>> entry : hidden.entrySet()) {
            if (!entry.getValue().remove(entityId)) {
                continue;
            }
            Player observer = Bukkit.getPlayer(entry.getKey());
            if (observer != null && observer.isOnline()) {
                observer.getScheduler().run(plugin, ignored -> destroyNametag(observer, nametagId), null);
            }
        }
    }

    public boolean isHidden(UUID observerId, int targetEntityId) {
        Set<Integer> set = hidden.get(observerId);
        return set != null && set.contains(targetEntityId);
    }

    public int hiddenPairs() {
        int count = 0;
        for (Set<Integer> set : hidden.values()) {
            count += set.size();
        }
        return count;
    }

    public int trackedPlayers() {
        return snapshots.size();
    }

    private void tick(Player observer) {
        FogSnapshot self = snapshots.get(observer.getUniqueId());
        if (self == null) {
            return;
        }
        writeSnapshot(observer, self);
        if (settings.worldDisabled(self.worldName)
                || observer.hasPermission(settings.bypassPermission())
                || self.vanished) {
            revealAll(observer);
            return;
        }

        double maxSq = settings.maxDistanceSq();
        for (FogSnapshot target : snapshots.values()) {
            if (target == self || !target.valid || target.worldId == null || !target.worldId.equals(self.worldId)) {
                continue;
            }
            if (fogPairExcluded(target)) {
                restoreNaturalVisibility(observer, target);
                continue;
            }
            double dx = target.x - self.x;
            double dy = target.y - self.y;
            double dz = target.z - self.z;
            if (dx * dx + dy * dy + dz * dz > maxSq) {
                ensureHidden(observer, self, target);
                continue;
            }
            processPairVisibility(observer, self, target);
        }
    }

    private void writeSnapshot(Player player, FogSnapshot snapshot) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        snapshot.worldId = world != null ? world.getUID() : null;
        snapshot.worldName = world != null ? world.getName() : "";
        snapshot.entityId = player.getEntityId();
        snapshot.x = loc.getX();
        snapshot.y = loc.getY();
        snapshot.z = loc.getZ();
        snapshot.eyeY = player.getEyeLocation().getY();
        snapshot.height = player.getHeight();
        Vector velocity = player.getVelocity();
        snapshot.vx = velocity.getX();
        snapshot.vy = velocity.getY();
        snapshot.vz = velocity.getZ();
        snapshot.yaw = loc.getYaw();
        snapshot.pitch = loc.getPitch();
        snapshot.bodyYaw = player.getBodyYaw();
        snapshot.onGround = player.isOnGround();
        var skinParts = player.getClientOption(ClientOption.SKIN_PARTS);
        snapshot.skinParts = skinParts != null ? (byte) skinParts.getRaw() : (byte) 0x7F;
        snapshot.sneaking = player.isSneaking();
        snapshot.invisible = player.isInvisible() || player.hasPotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
        snapshot.helmetStealth = helmetStealth(player);
        snapshot.vanished = settings.isVanished(player);
        Component name = player.displayName();
        snapshot.displayName = name != null ? name : Component.text(player.getName());
        snapshot.equipment = readEquipment(player);
        snapshot.valid = true;
    }

    private static boolean helmetStealth(Player player) {
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null || equipment.getHelmet() == null) {
            return false;
        }
        Material type = equipment.getHelmet().getType();
        String name = type.name();
        return type == Material.CARVED_PUMPKIN || name.endsWith("_HEAD") || name.endsWith("_SKULL");
    }

    private static ItemStack[] readEquipment(Player player) {
        EntityEquipment equipment = player.getEquipment();
        ItemStack[] slots = new ItemStack[6];
        if (equipment == null) {
            return slots;
        }
        slots[0] = convert(equipment.getItemInMainHand());
        slots[1] = convert(equipment.getItemInOffHand());
        slots[2] = convert(equipment.getHelmet());
        slots[3] = convert(equipment.getChestplate());
        slots[4] = convert(equipment.getLeggings());
        slots[5] = convert(equipment.getBoots());
        return slots;
    }

    private static ItemStack convert(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return ItemStack.EMPTY;
        }
        return SpigotConversionUtil.fromBukkitItemStack(item);
    }

    private boolean fogPairExcluded(FogSnapshot target) {
        if (settings.worldDisabled(target.worldName)) {
            return true;
        }
        Player live = Bukkit.getPlayer(target.playerId);
        if (live != null && live.isOnline()) {
            return settings.isVanished(live);
        }
        return target.vanished;
    }

    private void restoreNaturalVisibility(Player observer, FogSnapshot target) {
        PairKey key = new PairKey(observer.getUniqueId(), target.playerId);
        FogPairState state = pairs.get(key);
        Set<Integer> hiddenSet = hidden.get(observer.getUniqueId());
        if (hiddenSet == null || !hiddenSet.contains(target.entityId)) {
            if (state != null && state.nametagSpawned) {
                destroyNametag(observer, nametagId(target.entityId));
                state.nametagSpawned = false;
                clearLocator(observer, target, state);
            }
            return;
        }
        if (state == null) {
            state = new FogPairState();
            pairs.put(key, state);
        }
        state.visible = true;
        state.ticksOutOfSight = 0;
        show(observer, target, state);
    }

    private void processPairVisibility(Player observer, FogSnapshot self, FogSnapshot target) {
        PairKey key = new PairKey(self.playerId, target.playerId);
        FogPairState state = pairs.computeIfAbsent(key, ignored -> new FogPairState());
        boolean sight = hasLineOfSight(observer, self, target, state);

        if (sight) {
            state.ticksOutOfSight = 0;
            if (!state.visible) {
                state.visible = true;
                show(observer, target, state);
            }
            return;
        }

        if (state.visible) {
            state.ticksOutOfSight++;
            if (state.ticksOutOfSight >= settings.outOfSightThreshold()) {
                state.visible = false;
                hide(observer, target, state);
            }
            return;
        }

        syncHiddenExtras(observer, target, state);
    }

    private void ensureHidden(Player observer, FogSnapshot self, FogSnapshot target) {
        FogPairState state = pairs.computeIfAbsent(new PairKey(self.playerId, target.playerId), ignored -> new FogPairState());
        if (state.visible) {
            state.visible = false;
            state.ticksOutOfSight = settings.outOfSightThreshold();
            hide(observer, target, state);
        }
    }

    private void revealAll(Player observer) {
        Set<Integer> hiddenIds = hidden.get(observer.getUniqueId());
        if (hiddenIds == null || hiddenIds.isEmpty()) {
            return;
        }
        for (FogSnapshot target : snapshots.values()) {
            if (!hiddenIds.contains(target.entityId)) {
                continue;
            }
            FogPairState state = pairs.computeIfAbsent(new PairKey(observer.getUniqueId(), target.playerId), ignored -> new FogPairState());
            state.visible = true;
            state.ticksOutOfSight = 0;
            show(observer, target, state);
        }
    }

    private boolean hasLineOfSight(Player observer, FogSnapshot self, FogSnapshot target, FogPairState state) {
        double dx = target.x - self.x;
        double dy = (target.y + target.height * 0.5) - self.eyeY;
        double dz = target.z - self.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq <= settings.minDistanceSq()) {
            return true;
        }
        if (distSq > settings.maxDistanceSq()) {
            return false;
        }
        if (!inFov(observer, dx, dy, dz, Math.sqrt(distSq), state)) {
            return false;
        }

        World world = observer.getWorld();
        Location eye = observer.getEyeLocation();
        if (clear(world, eye, target.x, target.y + 0.1, target.z, state)
                || clear(world, eye, target.x, target.y + target.height * 0.5, target.z, state)
                || clear(world, eye, target.x, target.eyeY, target.z, state)) {
            return true;
        }

        double radius = settings.anticipation();
        double midY = target.y + target.height * 0.5;
        if (clear(world, eye, target.x + radius, midY, target.z, state)
                || clear(world, eye, target.x - radius, midY, target.z, state)
                || clear(world, eye, target.x, midY, target.z + radius, state)
                || clear(world, eye, target.x, midY, target.z - radius, state)) {
            return true;
        }

        double speedSq = target.vx * target.vx + target.vy * target.vy + target.vz * target.vz;
        if (speedSq > 0.01) {
            double scale = settings.velocityExtrapolate();
            return clear(world, eye, target.x + target.vx * scale, midY + target.vy * scale, target.z + target.vz * scale, state);
        }
        return false;
    }

    /**
     * Ângulo entre o olhar do observador e o vetor olho→alvo (3D). Só X/Z quebrava visão inclinada (ex.: de cima).
     */
    private boolean inFov(Player observer, double dx, double dy, double dz, double distance, FogPairState state) {
        if (distance < 0.001) {
            return true;
        }
        Vector facing = observer.getLocation().getDirection();
        double inv = 1.0 / distance;
        double toX = dx * inv;
        double toY = dy * inv;
        double toZ = dz * inv;
        double dot = facing.getX() * toX + facing.getY() * toY + facing.getZ() * toZ;
        double threshold = settings.fovCosine();
        if (state.visible) {
            threshold -= settings.fovHysteresis();
        }
        return dot >= threshold;
    }

    private boolean clear(World world, Location eye, double x, double y, double z, FogPairState state) {
        double dx = x - eye.getX();
        double dy = y - eye.getY();
        double dz = z - eye.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 0.1) {
            return true;
        }
        try {
            boolean open = world.rayTraceBlocks(eye, new Vector(dx / distance, dy / distance, dz / distance), distance, FluidCollisionMode.NEVER, true) == null;
            state.lastRayClear = open;
            return open;
        } catch (IllegalStateException crossedRegion) {
            return state.lastRayClear == null || state.lastRayClear;
        }
    }

    private void hide(Player observer, FogSnapshot target, FogPairState state) {
        hidden.computeIfAbsent(observer.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet()).add(target.entityId);
        holdExternalTag(observer, target);
        if (settings.strategy() == FogSettings.Strategy.TELEPORT) {
            teleport(observer, target.entityId, target.x, -64.0, target.z, target.yaw, target.pitch, false);
        } else {
            send(observer, new WrapperPlayServerDestroyEntities(target.entityId));
        }
        syncHiddenExtras(observer, target, state);
    }

    private void show(Player observer, FogSnapshot target, FogPairState state) {
        Set<Integer> set = hidden.get(observer.getUniqueId());
        if (set != null) {
            set.remove(target.entityId);
        }
        destroyNametag(observer, nametagId(target.entityId));
        state.nametagSpawned = false;
        clearLocator(observer, target, state);

        if (settings.strategy() == FogSettings.Strategy.TELEPORT) {
            teleport(observer, target.entityId, target.x, target.y, target.z, target.yaw, target.pitch, target.onGround);
        } else {
            send(observer, new WrapperPlayServerSpawnEntity(
                    target.entityId,
                    Optional.of(target.playerId),
                    EntityTypes.PLAYER,
                    new Vector3d(target.x, target.y, target.z),
                    target.pitch,
                    target.bodyYaw,
                    target.yaw,
                    0,
                    Optional.of(new Vector3d(0, 0, 0))));
            send(observer, new WrapperPlayServerEntityHeadLook(target.entityId, target.yaw));
            sendSkinParts(observer, target);
            sendEquipment(observer, target.entityId, target.equipment);
        }
        scheduleExternalTagRelease(observer, target);
    }

    private void holdExternalTag(Player observer, FogSnapshot target) {
        if (!tagsAttached) {
            return;
        }
        PairKey key = new PairKey(observer.getUniqueId(), target.playerId);
        tagEpoch.merge(key, 1, Integer::sum);
        tagHold.add(key);
        Player live = Bukkit.getPlayer(target.playerId);
        if (live != null && live.isOnline()) {
            tags.onVisibilityChanged(live);
        }
    }

    /**
     * O destroy do jogador também apaga o text display no cliente, mas o AnarchyTags
     * ainda acha que ele existe. Segura o filtro, pede o despawn e só então mostra de novo.
     */
    private void scheduleExternalTagRelease(Player observer, FogSnapshot target) {
        if (!tagsAttached) {
            return;
        }
        PairKey key = new PairKey(observer.getUniqueId(), target.playerId);
        tagHold.add(key);
        int epoch = tagEpoch.getOrDefault(key, 0);
        Player live = Bukkit.getPlayer(target.playerId);
        if (live == null || !live.isOnline()) {
            return;
        }
        live.getScheduler().run(plugin, task -> {
            if (!live.isOnline() || tagEpoch.getOrDefault(key, -1) != epoch) {
                return;
            }
            tags.onVisibilityChanged(live);
            live.getScheduler().runDelayed(plugin, later -> {
                if (!live.isOnline() || tagEpoch.getOrDefault(key, -1) != epoch) {
                    return;
                }
                tagHold.remove(key);
                tags.onVisibilityChanged(live);
            }, null, 2L);
        }, null);
    }

    private void syncHiddenExtras(Player observer, FogSnapshot target, FogPairState state) {
        boolean stealthed = target.stealthed();
        if (settings.nametagEnabled() && !tagsAttached && !stealthed) {
            int nametag = nametagId(target.entityId);
            if (!state.nametagSpawned) {
                spawnNametag(observer, target, nametag);
                state.nametagSpawned = true;
                state.nametagCooldown = settings.nametagMoveIntervalTicks();
            } else if (--state.nametagCooldown <= 0) {
                state.nametagCooldown = settings.nametagMoveIntervalTicks();
                double nx = target.x;
                double ny = target.y + target.height;
                double nz = target.z;
                double dx = nx - state.nametagX;
                double dy = ny - state.nametagY;
                double dz = nz - state.nametagZ;
                if (dx * dx + dy * dy + dz * dz >= settings.nametagMoveEpsilonSq()) {
                    teleport(observer, nametag, nx, ny, nz, 0f, 0f, false);
                    state.nametagX = nx;
                    state.nametagY = ny;
                    state.nametagZ = nz;
                }
            }
        } else if (state.nametagSpawned) {
            destroyNametag(observer, nametagId(target.entityId));
            state.nametagSpawned = false;
        }

        if (locator != null && settings.locatorWhenStealthed()) {
            Player targetPlayer = Bukkit.getPlayer(target.playerId);
            if (targetPlayer == null) {
                return;
            }
            if (stealthed && !state.locatorHidden) {
                locator.hidePlayer(observer, targetPlayer);
                state.locatorHidden = true;
            } else if (!stealthed && state.locatorHidden) {
                clearLocator(observer, target, state);
            }
        }
    }

    private void clearLocator(Player observer, FogSnapshot target, FogPairState state) {
        if (!state.locatorHidden || locator == null) {
            state.locatorHidden = false;
            return;
        }
        Player targetPlayer = Bukkit.getPlayer(target.playerId);
        if (targetPlayer != null) {
            locator.resetPlayerVisibility(observer, targetPlayer);
        }
        state.locatorHidden = false;
    }

    private void spawnNametag(Player observer, FogSnapshot target, int nametagId) {
        double y = target.y + target.height;
        send(observer, new WrapperPlayServerSpawnEntity(
                nametagId,
                Optional.of(UUID.randomUUID()),
                EntityTypes.ARMOR_STAND,
                new Vector3d(target.x, y, target.z),
                0f,
                0f,
                0f,
                0,
                Optional.of(new Vector3d(0, 0, 0))));
        List<EntityData<?>> metadata = new ArrayList<>(4);
        metadata.add(new EntityData(0, EntityDataTypes.BYTE, (byte) 0x20));
        metadata.add(new EntityData(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(target.displayName)));
        metadata.add(new EntityData(3, EntityDataTypes.BOOLEAN, true));
        metadata.add(new EntityData(15, EntityDataTypes.BYTE, (byte) 25));
        send(observer, new WrapperPlayServerEntityMetadata(nametagId, metadata));
        stateRememberNametag(observer, target, y);
    }

    private void stateRememberNametag(Player observer, FogSnapshot target, double y) {
        FogPairState state = pairs.get(new PairKey(observer.getUniqueId(), target.playerId));
        if (state != null) {
            state.nametagX = target.x;
            state.nametagY = y;
            state.nametagZ = target.z;
        }
    }

    private void destroyNametag(Player observer, int nametagId) {
        send(observer, new WrapperPlayServerDestroyEntities(nametagId));
    }

    private void teleport(Player observer, int entityId, double x, double y, double z, float yaw, float pitch, boolean onGround) {
        send(observer, new WrapperPlayServerEntityTeleport(entityId, new Vector3d(x, y, z), yaw, pitch, onGround));
    }

    private static void sendSkinParts(Player observer, FogSnapshot target) {
        send(observer, new WrapperPlayServerEntityMetadata(
                target.entityId,
                List.of(new EntityData<>(16, EntityDataTypes.BYTE, target.skinParts))));
    }

    private static void sendEquipment(Player observer, int entityId, ItemStack[] slots) {
        if (slots == null) {
            return;
        }
        EquipmentSlot[] order = {
                EquipmentSlot.MAIN_HAND,
                EquipmentSlot.OFF_HAND,
                EquipmentSlot.HELMET,
                EquipmentSlot.CHEST_PLATE,
                EquipmentSlot.LEGGINGS,
                EquipmentSlot.BOOTS
        };
        List<Equipment> list = new ArrayList<>(6);
        for (int i = 0; i < order.length && i < slots.length; i++) {
            ItemStack item = slots[i];
            if (item != null && !item.isEmpty()) {
                list.add(new Equipment(order[i], item));
            }
        }
        if (!list.isEmpty()) {
            send(observer, new WrapperPlayServerEntityEquipment(entityId, list));
        }
    }

    private void clearObserver(Player observer) {
        Set<Integer> ids = hidden.remove(observer.getUniqueId());
        if (ids == null) {
            return;
        }
        for (int entityId : ids) {
            destroyNametag(observer, nametagId(entityId));
        }
    }

    private static int nametagId(int targetEntityId) {
        return 1_000_000_000 + targetEntityId;
    }

    private static void send(Player player, com.github.retrooper.packetevents.wrapper.PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    private record PairKey(UUID observer, UUID target) {}
}
