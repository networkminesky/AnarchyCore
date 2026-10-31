package anarchy.minesky.core.impl.guard;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class GuardJoinListener implements Listener {

    private final GuardManager guardManager;

    public GuardJoinListener(GuardManager guardManager) {
        this.guardManager = guardManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        guardManager.snapshots().refreshFromPlayer(player);
        guardManager.scheduleJoinCheck(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        guardManager.snapshots().remove(event.getPlayer().getUniqueId());
        guardManager.shutdownPartial(event.getPlayer().getUniqueId());
    }
}
