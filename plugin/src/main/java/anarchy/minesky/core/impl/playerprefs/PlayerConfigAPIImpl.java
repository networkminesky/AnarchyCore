package anarchy.minesky.core.impl.playerprefs;

import anarchy.minesky.core.api.playerconfig.PlayerConfigAPI;
import anarchy.minesky.core.api.playerconfig.PlayerConfigValues;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.util.UUID;

public final class PlayerConfigAPIImpl implements PlayerConfigAPI {

    private final Plugin plugin;
    private final PlayerPreferencesService prefs;

    public PlayerConfigAPIImpl(Plugin plugin, PlayerPreferencesService prefs) {
        this.plugin = plugin;
        this.prefs = prefs;
    }

    public void register() {
        plugin.getServer().getServicesManager().register(PlayerConfigAPI.class, this, plugin, ServicePriority.Normal);
    }

    public void unregister() {
        plugin.getServer().getServicesManager().unregister(PlayerConfigAPI.class, this);
    }

    @Override
    public PlayerConfigValues values(Player player) {
        return values(player.getUniqueId());
    }

    @Override
    public PlayerConfigValues values(UUID playerId) {
        return fromPrefs(prefs.get(playerId));
    }

    @Override
    public boolean canReceiveTpaFrom(Player target, Player requester) {
        return prefs.canReceiveTpaFrom(target, requester);
    }

    @Override
    public boolean canReceiveTpaHereFrom(Player target, Player requester) {
        return prefs.canReceiveTpaHereFrom(target, requester);
    }

    @Override
    public boolean canReceiveTellFrom(Player target, Player sender) {
        return prefs.canReceiveTellFrom(target, sender);
    }

    @Override
    public boolean canReceivePayFrom(Player target, Player sender) {
        return prefs.canReceivePayFrom(target, sender);
    }

    static PlayerConfigValues fromPrefs(PlayerPreferences p) {
        return new PlayerConfigValues(
                p.allowTpaFromStrangers(),
                p.allowTpaHereFromStrangers(),
                p.allowPayFromStrangers(),
                p.paymentSounds(),
                p.allowTellFromStrangers(),
                p.tellNotificationSounds(),
                p.scoreboardEnabled());
    }
}
