package anarchy.minesky.core.impl.blocks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockManager {

    private final Plugin plugin;
    private final BlockSettings settings;
    private final BlockStorage storage;

    private final Map<String, BlockDefinition> byPosition = new HashMap<>();
    private final Map<String, Map<Long, List<BlockDefinition>>> byWorldChunk = new HashMap<>();

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastPositionCell = new ConcurrentHashMap<>();

    private ScheduledTask globalParticleTask;

    public BlockManager(Plugin plugin, BlockSettings settings, BlockStorage storage) {
        this.plugin = plugin;
        this.settings = settings;
        this.storage = storage;
        rebuildIndex();
    }

    public BlockStorage storage() {
        return storage;
    }

    public BlockSettings settings() {
        return settings;
    }

    public void reload() {
        settings.reload();
        storage.load();
        rebuildIndex();
        restartParticleTask();
    }

    public void shutdown() {
        if (globalParticleTask != null) {
            globalParticleTask.cancel();
            globalParticleTask = null;
        }
        byPosition.clear();
        byWorldChunk.clear();
        cooldowns.clear();
        lastPositionCell.clear();
    }

    public void handleQuit(UUID playerId) {
        cooldowns.remove(playerId);
        lastPositionCell.remove(playerId);
    }

    public List<BlockDefinition> atPosition(String world, int x, int y, int z) {
        BlockDefinition single = byPosition.get(positionKey(world, x, y, z));
        if (single == null) {
            return List.of();
        }
        return List.of(single);
    }

    public void onPlayerBlockCellChange(Player player, String world, int x, int y, int z) {
        UUID uuid = player.getUniqueId();
        String cell = positionKey(world, x, y, z);
        String previous = lastPositionCell.put(uuid, cell);
        if (cell.equals(previous)) {
            return;
        }
        BlockDefinition block = byPosition.get(cell);
        if (block == null || block.type() != BlockType.POSITION) {
            return;
        }
        tryTrigger(player, block);
    }

    public void onBlockClick(Player player, String world, int x, int y, int z) {
        BlockDefinition block = byPosition.get(positionKey(world, x, y, z));
        if (block == null || block.type() != BlockType.CLICK) {
            return;
        }
        tryTrigger(player, block);
    }

    public boolean createAtPlayer(Player player, String id, BlockType type, List<String> commands) {
        Location blockLoc = player.getLocation().getBlock().getLocation();
        World world = blockLoc.getWorld();
        if (world == null) {
            return false;
        }
        BlockDefinition definition = new BlockDefinition(
                BlockStorage.normalizeId(id),
                world.getName(),
                blockLoc.getBlockX(),
                blockLoc.getBlockY(),
                blockLoc.getBlockZ(),
                type,
                commands == null ? List.of() : commands,
                null,
                settings.defaultCooldownMs());
        storage.put(definition);
        rebuildIndex();
        return true;
    }

    public boolean updateCommands(String id, List<String> commands) {
        return storage.get(id).map(existing -> {
            storage.put(existing.withCommands(commands == null ? List.of() : commands));
            rebuildIndex();
            return true;
        }).orElse(false);
    }

    public boolean updateLocation(Player player, String id) {
        return storage.get(id).map(existing -> {
            Location blockLoc = player.getLocation().getBlock().getLocation();
            World world = blockLoc.getWorld();
            if (world == null) {
                return false;
            }
            storage.put(existing.withLocation(
                    world.getName(),
                    blockLoc.getBlockX(),
                    blockLoc.getBlockY(),
                    blockLoc.getBlockZ()));
            rebuildIndex();
            return true;
        }).orElse(false);
    }

    public boolean updateType(String id, BlockType type) {
        if (type == null) {
            return false;
        }
        return storage.get(id).map(existing -> {
            storage.put(existing.withType(type));
            rebuildIndex();
            return true;
        }).orElse(false);
    }

    public boolean remove(String id) {
        if (!storage.remove(id)) {
            return false;
        }
        rebuildIndex();
        return true;
    }

    private void tryTrigger(Player player, BlockDefinition block) {
        if (block.commands().isEmpty()) {
            return;
        }
        if (!canTrigger(player.getUniqueId(), block)) {
            return;
        }
        runCommands(player, block);
        markTriggered(player.getUniqueId(), block);
    }

    private boolean canTrigger(UUID playerId, BlockDefinition block) {
        long cooldown = block.cooldownMs();
        if (cooldown <= 0L) {
            return true;
        }
        Map<String, Long> map = cooldowns.get(playerId);
        if (map == null) {
            return true;
        }
        Long last = map.get(block.id());
        return last == null || System.currentTimeMillis() - last >= cooldown;
    }

    private void markTriggered(UUID playerId, BlockDefinition block) {
        cooldowns.computeIfAbsent(playerId, id -> new ConcurrentHashMap<>())
                .put(block.id(), System.currentTimeMillis());
    }

    private void runCommands(Player player, BlockDefinition block) {
        if (Bukkit.isOwnedByCurrentRegion(player)) {
            dispatchCommands(player, block);
            return;
        }
        player.getScheduler().run(plugin, task -> dispatchCommands(player, block), null);
    }

    private static void dispatchCommands(Player player, BlockDefinition block) {
        for (String raw : block.commands()) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String command = raw
                    .replace("{player}", player.getName())
                    .replace("%player%", player.getName())
                    .trim();
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            Bukkit.dispatchCommand(player, command);
        }
    }

    private void rebuildIndex() {
        byPosition.clear();
        byWorldChunk.clear();
        for (BlockDefinition block : storage.all()) {
            byPosition.put(block.positionKey(), block);
            byWorldChunk
                    .computeIfAbsent(block.worldName(), key -> new HashMap<>())
                    .computeIfAbsent(block.chunkKey(), key -> new ArrayList<>())
                    .add(block);
        }
        restartParticleTask();
    }

    private void restartParticleTask() {
        if (globalParticleTask != null) {
            globalParticleTask.cancel();
            globalParticleTask = null;
        }
        boolean anyParticles = storage.all().stream()
                .anyMatch(block -> block.particles() != null && block.particles().enabled());
        if (!anyParticles) {
            return;
        }
        long period = settings.particlePeriodTicks();
        globalParticleTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            if (!plugin.isEnabled()) {
                task.cancel();
                return;
            }
            tickParticles();
        }, period, period);
    }

    private void tickParticles() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            return;
        }
        double viewDistSq = settings.particleViewDistance() * settings.particleViewDistance();

        for (BlockDefinition block : storage.all()) {
            BlockParticleSettings particles = block.particles();
            if (particles == null || !particles.enabled()) {
                continue;
            }
            Location center = block.centerLocation();
            if (center == null || center.getWorld() == null) {
                continue;
            }
            boolean nearby = false;
            for (Player player : players) {
                if (!player.getWorld().equals(center.getWorld())) {
                    continue;
                }
                if (player.getLocation().distanceSquared(center) <= viewDistSq) {
                    nearby = true;
                    break;
                }
            }
            if (!nearby) {
                continue;
            }
            spawnParticles(center, particles);
        }
    }

    private void spawnParticles(Location center, BlockParticleSettings settings) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        Bukkit.getRegionScheduler().execute(plugin, world, center.getBlockX() >> 4, center.getBlockZ() >> 4, () -> {
            if (!world.isChunkLoaded(center.getBlockX() >> 4, center.getBlockZ() >> 4)) {
                return;
            }
            world.spawnParticle(
                    settings.particle(),
                    center,
                    settings.amount(),
                    settings.spreadX(),
                    settings.spreadY(),
                    settings.spreadZ(),
                    settings.speed());
        });
    }

    private static String positionKey(String world, int x, int y, int z) {
        return world + ':' + x + ':' + y + ':' + z;
    }
}
