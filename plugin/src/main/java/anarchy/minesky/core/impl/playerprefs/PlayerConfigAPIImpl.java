package anarchy.minesky.core.impl.playerprefs;

import anarchy.minesky.core.api.playerconfig.PlayerConfigBooleanOption;
import anarchy.minesky.core.api.playerconfig.PlayerConfigKeys;
import anarchy.minesky.core.api.playerconfig.PlayerConfigRegistry;
import anarchy.minesky.core.api.playerconfig.PlayerConfigValues;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerConfigAPIImpl implements anarchy.minesky.core.api.playerconfig.PlayerConfigAPI {

    private final Plugin plugin;
    private final PlayerPreferencesService prefs;
    private final PlayerConfigRegistryImpl registry;

    public PlayerConfigAPIImpl(Plugin plugin, PlayerPreferencesService prefs) {
        this.plugin = plugin;
        this.prefs = prefs;
        this.registry = new PlayerConfigRegistryImpl(prefs, this);
        prefs.attachRegistry(registry);
    }

    public void registerCoreDefaults(anarchy.minesky.core.impl.config.MessagesManager messages) {
        CorePlayerConfigBootstrap.register(plugin, registry, messages);
    }

    public void register() {
        plugin.getServer().getServicesManager().register(
                anarchy.minesky.core.api.playerconfig.PlayerConfigAPI.class, this, plugin, ServicePriority.Normal);
    }

    public void unregister() {
        plugin.getServer().getServicesManager().unregister(
                anarchy.minesky.core.api.playerconfig.PlayerConfigAPI.class, this);
    }

    @Override
    public PlayerConfigRegistry registry() {
        return registry;
    }

    @Override
    public boolean getBoolean(PlayerConfigBooleanOption option, Player player) {
        return getBoolean(option, player.getUniqueId());
    }

    @Override
    public boolean getBoolean(PlayerConfigBooleanOption option, UUID playerId) {
        if (!(option instanceof PlayerConfigRegistryImpl.PlayerConfigBooleanOptionImpl impl)) {
            return getBoolean(option.key(), playerId);
        }
        return prefs.getBooleanFlag(playerId, impl.index(), impl.defaultValue());
    }

    @Override
    public boolean getBoolean(String key, Player player) {
        return getBoolean(key, player.getUniqueId());
    }

    @Override
    public boolean getBoolean(String key, UUID playerId) {
        return registry.booleanOption(key)
                .map(opt -> getBoolean(opt, playerId))
                .orElseGet(() -> {
                    Boolean stored = prefs.get(playerId).configBooleans().get(key);
                    return stored != null ? stored : false;
                });
    }

    @Override
    public PlayerConfigValues values(Player player) {
        return values(player.getUniqueId());
    }

    @Override
    public PlayerConfigValues values(UUID playerId) {
        Map<String, Boolean> snapshot = new LinkedHashMap<>();
        for (PlayerConfigRegistryImpl.PlayerConfigBooleanOptionImpl option : registry.optionsByIndex()) {
            snapshot.put(option.key(), getBoolean(option, playerId));
        }
        snapshot.put(PlayerConfigKeys.TPA_AUTO, prefs.get(playerId).tpaAuto());
        return new PlayerConfigValues(Map.copyOf(snapshot));
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

    PlayerConfigRegistryImpl registryImpl() {
        return registry;
    }
}
