package anarchy.minesky.core.listener;

import anarchy.minesky.core.impl.homes.HomeManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class HomeListener implements Listener {

    private final HomeManager homeManager;

    public HomeListener(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        homeManager.onPlayerMoved(event.getPlayer(), event.getFrom(), event.getTo());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        homeManager.refreshHomeIdCache(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        homeManager.handleQuit(event.getPlayer().getUniqueId());
    }
}
