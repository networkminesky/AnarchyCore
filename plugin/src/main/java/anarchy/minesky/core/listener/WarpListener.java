package anarchy.minesky.core.listener;

import anarchy.minesky.core.impl.warps.WarpManager;
import anarchy.minesky.core.impl.warps.WarpSpawnHandler;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class WarpListener implements Listener {

    private final WarpManager warpManager;
    private final WarpSpawnHandler spawnHandler;

    public WarpListener(WarpManager warpManager, WarpSpawnHandler spawnHandler) {
        this.warpManager = warpManager;
        this.spawnHandler = spawnHandler;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onFirstJoinSpawn(AsyncPlayerSpawnLocationEvent event) {
        spawnHandler.handleFirstJoin(event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        spawnHandler.handleRespawn(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        spawnHandler.handleDeath(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        warpManager.onPlayerMoved(event.getPlayer(), event.getFrom(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        warpManager.handleQuit(event.getPlayer().getUniqueId());
    }
}
