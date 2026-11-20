package anarchy.minesky.core.listener;

import anarchy.minesky.core.impl.scheduling.PlayerTickHub;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerTickHubListener implements Listener {

    private final PlayerTickHub hub;

    public PlayerTickHubListener(PlayerTickHub hub) {
        this.hub = hub;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        hub.handleQuit(event.getPlayer().getUniqueId());
    }
}