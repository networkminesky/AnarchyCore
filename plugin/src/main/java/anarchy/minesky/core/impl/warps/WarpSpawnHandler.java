package anarchy.minesky.core.impl.warps;

import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spawn/respawn na warp {@code spawn}. No Folia, {@link PlayerRespawnEvent} pode não disparar;
 * o fallback usa {@link Player#setRespawnLocation} na morte e teleporte após renascer.
 */
public final class WarpSpawnHandler {

    private static final String DEFAULT_SPAWN_WARP = "spawn";

    private final Plugin plugin;
    private final WarpManager warps;

    private final Map<UUID, ScheduledTask> foliaRespawnWatchers = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pendingSpawnTeleports = new ConcurrentHashMap<>();

    public WarpSpawnHandler(Plugin plugin, WarpManager warps) {
        this.plugin = plugin;
        this.warps = warps;
    }

    public void handleFirstJoin(AsyncPlayerSpawnLocationEvent event) {
        if (!event.isNewPlayer()) {
            return;
        }
        defaultSpawnLocation().ifPresent(event::setSpawnLocation);
    }

  /** Caminho Paper quando o evento dispara. */
    public void handleRespawn(PlayerRespawnEvent event) {
        if (hasValidPersonalRespawn(event)) {
            return;
        }
        defaultSpawnLocation().ifPresent(event::setRespawnLocation);
    }

  /** Caminho Folia (e reforço no Paper): definir respawn antes da tela de morte fechar. */
    public void handleDeath(Player player) {
        if (hasValidPersonalRespawn(player)) {
            return;
        }
        Optional<Location> spawn = defaultSpawnLocation();
        if (spawn.isEmpty()) {
            return;
        }
        Location destination = spawn.get().clone();
        Runnable apply = () -> {
            if (!player.isOnline()) {
                return;
            }
            player.setRespawnLocation(destination, true);
            pendingSpawnTeleports.put(player.getUniqueId(), destination);
            scheduleFoliaRespawnFallback(player);
        };
        if (Bukkit.isOwnedByCurrentRegion(player)) {
            apply.run();
        } else {
            player.getScheduler().run(plugin, task -> apply.run(), null);
        }
    }

    public void handleQuit(UUID playerId) {
        cancelFoliaWatcher(playerId);
        pendingSpawnTeleports.remove(playerId);
    }

    private void scheduleFoliaRespawnFallback(Player player) {
        UUID uuid = player.getUniqueId();
        cancelFoliaWatcher(uuid);
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, scheduled -> {
            if (!player.isOnline()) {
                scheduled.cancel();
                foliaRespawnWatchers.remove(uuid, scheduled);
                pendingSpawnTeleports.remove(uuid);
                return;
            }
            if (!player.isDead()) {
                scheduled.cancel();
                foliaRespawnWatchers.remove(uuid, scheduled);
                Location destination = pendingSpawnTeleports.remove(uuid);
                if (destination == null) {
                    return;
                }
                Location current = player.getLocation();
                if (current.getWorld() == null || destination.getWorld() == null) {
                    return;
                }
                if (!current.getWorld().equals(destination.getWorld())
                        || current.distanceSquared(destination) > 1.0D) {
                    player.teleportAsync(destination);
                }
            }
        }, null, 1L, 5L);
        foliaRespawnWatchers.put(uuid, task);
    }

    private void cancelFoliaWatcher(UUID playerId) {
        ScheduledTask task = foliaRespawnWatchers.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * {@link Player#getRespawnLocation()} devolve a posição onde o jogador aparece (geralmente no ar),
     * não o bloco da cama/anchor — por isso procuramos o bloco num raio pequeno.
     */
    private boolean hasValidPersonalRespawn(Player player) {
        Location validated = player.getRespawnLocation();
        if (validated == null || validated.getWorld() == null) {
            return false;
        }
        Location blockHint = player.getRespawnLocation(false);
        if (blockHint == null) {
            blockHint = validated;
        }
        return containsBedOrAnchor(blockHint);
    }

    private boolean hasValidPersonalRespawn(PlayerRespawnEvent event) {
        boolean hasBlockSpawn = event.isBedSpawn() || event.isAnchorSpawn();
        return hasBlockSpawn && !event.isMissingRespawnBlock();
    }

    private static boolean containsBedOrAnchor(Location origin) {
        World world = origin.getWorld();
        if (world == null) {
            return false;
        }
        int bx = origin.getBlockX();
        int by = origin.getBlockY();
        int bz = origin.getBlockZ();
        if (!world.isChunkLoaded(bx >> 4, bz >> 4)) {
            return true;
        }
        for (int dy = -1; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Material type = world.getBlockAt(bx + dx, by + dy, bz + dz).getType();
                    if (isBedOrAnchor(type)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isBedOrAnchor(Material type) {
        if (type.isAir()) {
            return false;
        }
        String name = type.name();
        return name.endsWith("_BED") || name.equals("RESPAWN_ANCHOR");
    }

    private Optional<Location> defaultSpawnLocation() {
        return warps.warpLocation(DEFAULT_SPAWN_WARP).map(Location::clone);
    }
}