package anarchy.minesky.core.listener;

import anarchy.minesky.core.impl.stats.PlayerStatsService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerStatsListener implements Listener {

    private final PlayerStatsService stats;

    public PlayerStatsListener(PlayerStatsService stats) {
        this.stats = stats;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        stats.onJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        stats.onQuit(event.getPlayer());
    }
}