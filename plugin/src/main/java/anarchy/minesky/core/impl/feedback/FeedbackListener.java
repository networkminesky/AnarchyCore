package anarchy.minesky.core.impl.feedback;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class FeedbackListener implements Listener {

    private final FeedbackManager manager;

    public FeedbackListener(FeedbackManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        manager.handleQuit(event.getPlayer().getUniqueId());
    }
}
