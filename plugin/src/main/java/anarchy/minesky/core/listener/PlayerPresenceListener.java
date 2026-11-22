package anarchy.minesky.core.listener;

import anarchy.minesky.core.impl.presence.PlayerPresenceTracker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerPresenceListener implements Listener {

    private final PlayerPresenceTracker tracker;

    public PlayerPresenceListener(PlayerPresenceTracker tracker) {
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        tracker.onJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        tracker.onQuit(event.getPlayer());
    }
}