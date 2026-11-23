package anarchy.minesky.core.impl.warps;

import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
        Runnable apply = () -> {
            if (!player.isOnline()) {
                return;
            }
            if (hasStoredPersonalRespawn(player)) {
                return;
            }
            Optional<Location> spawn = defaultSpawnLocation();
            if (spawn.isEmpty()) {
                return;
            }
            Location destination = spawn.get().clone();
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
     * Lê só o ponto salvo no jogador, sem carregar chunk/bloco (seguro no Folia cross-world).
     * Cama/anchor quebrados o servidor trata na hora do respawn; {@link PlayerRespawnEvent} cobre o Paper.
     */
    private static boolean hasStoredPersonalRespawn(Player player) {
        Location stored = player.getRespawnLocation(false);
        return stored != null && stored.getWorld() != null;
    }

    private static boolean hasValidPersonalRespawn(PlayerRespawnEvent event) {
        boolean hasBlockSpawn = event.isBedSpawn() || event.isAnchorSpawn();
        return hasBlockSpawn && !event.isMissingRespawnBlock();
    }

    private Optional<Location> defaultSpawnLocation() {
        return warps.warpLocation(DEFAULT_SPAWN_WARP).map(Location::clone);
    }
}