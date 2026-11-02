package anarchy.minesky.core.impl.guard.fog;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

public final class FogListener implements Listener {

    private final FogOfWarManager fog;

    public FogListener(FogOfWarManager fog) {
        this.fog = fog;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        fog.track(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        fog.forget(event.getPlayer());
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent event) {
        fog.forget(event.getPlayer());
        fog.track(event.getPlayer());
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getName().equals("AnarchyTags")) {
            fog.attachTags();
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().getName().equals("AnarchyTags")) {
            fog.detachTags();
        }
    }
}
