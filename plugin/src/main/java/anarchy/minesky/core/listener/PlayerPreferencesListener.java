package anarchy.minesky.core.listener;

import anarchy.minesky.core.api.playerdata.PlayerDataAPI;
import anarchy.minesky.core.impl.escmenu.EscMenuKeys;
import anarchy.minesky.core.impl.escmenu.EscMenuRuntime;
import anarchy.minesky.core.impl.playerprefs.PlayerPreferencesService;
import anarchy.minesky.core.impl.tpa.TpaManager;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class PlayerPreferencesListener implements Listener {

    private final Plugin plugin;
    private final PlayerPreferencesService prefs;
    private final EscMenuRuntime escMenu;
    private final PlayerDataAPI playerData;
    private final TpaManager tpaManager;

    public PlayerPreferencesListener(
            Plugin plugin,
            PlayerPreferencesService prefs,
            EscMenuRuntime escMenu,
            PlayerDataAPI playerData,
            TpaManager tpaManager) {
        this.plugin = plugin;
        this.prefs = prefs;
        this.escMenu = escMenu;
        this.playerData = playerData;
        this.tpaManager = tpaManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        prefs.loadPlayer(event.getPlayer().getUniqueId());
        if (playerData != null) {
            playerData.touchPlayerAsync(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        }
        event.getPlayer().getScheduler().run(plugin, task -> tpaManager.applyTpAutoState(event.getPlayer()), null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        prefs.unloadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onCustomClick(PlayerCustomClickEvent event) {
        if (!EscMenuKeys.isEscCustomClick(event.getIdentifier())) {
            return;
        }
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) {
            return;
        }
        Player player = connection.getPlayer();
        if (player == null || !player.isOnline()) {
            return;
        }
        player.getScheduler().run(plugin, task -> {
            if (!plugin.isEnabled() || !player.isOnline()) {
                return;
            }
            escMenu.handleCustomClick(event.getIdentifier(), player);
        }, null);
    }
}
