package anarchy.minesky.core.impl.playerprefs;

import anarchy.minesky.core.api.playerconfig.PlayerConfigBooleanOption;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PlayerPreferencesService {

    private final PlayerPreferencesRepository repository;
    private final Map<UUID, PlayerPreferences> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> knownCache = new ConcurrentHashMap<>();
    private PlayerConfigRegistryImpl registry;

    public PlayerPreferencesService(PlayerPreferencesRepository repository) {
        this.repository = repository;
    }

    void attachRegistry(PlayerConfigRegistryImpl registry) {
        this.registry = registry;
    }

    void onBooleanOptionRegistered(int index, boolean defaultValue) {
        if (registry == null) {
            return;
        }
        PlayerConfigRegistryImpl.PlayerConfigBooleanOptionImpl option = registry.optionAt(index);
        if (option == null) {
            return;
        }
        String key = option.key();
        for (PlayerPreferences prefs : cache.values()) {
            prefs.putConfigBooleanIfAbsent(key, defaultValue);
        }
    }

    public void loadPlayer(UUID playerId) {
        cache.put(playerId, repository.load(playerId));
        knownCache.put(playerId, repository.loadKnown(playerId));
    }

    public void unloadPlayer(UUID playerId) {
        PlayerPreferences prefs = cache.remove(playerId);
        if (prefs != null) {
            repository.saveSync(playerId, prefs);
        }
        knownCache.remove(playerId);
    }

    public void flushAllCachedSync() {
        for (Map.Entry<UUID, PlayerPreferences> entry : cache.entrySet()) {
            repository.saveSync(entry.getKey(), entry.getValue());
        }
    }

    public PlayerPreferences get(UUID playerId) {
        return cache.computeIfAbsent(playerId, repository::load);
    }

    public PlayerPreferences get(Player player) {
        return get(player.getUniqueId());
    }

    public void update(Player player, Consumer<PlayerPreferences> editor) {
        PlayerPreferences prefs = get(player).copy();
        editor.accept(prefs);
        cache.put(player.getUniqueId(), prefs);
        repository.save(player.getUniqueId(), prefs);
    }

    boolean getBooleanFlag(UUID playerId, int index, boolean defaultValue) {
        if (registry == null) {
            return defaultValue;
        }
        PlayerConfigRegistryImpl.PlayerConfigBooleanOptionImpl option = registry.optionAt(index);
        if (option == null) {
            return defaultValue;
        }
        return get(playerId).getConfigBoolean(option.key(), defaultValue);
    }

    boolean getExtraBoolean(UUID playerId, String key) {
        return get(playerId).getConfigBoolean(key, false);
    }

    void setConfigBoolean(Player player, PlayerConfigBooleanOption option, boolean value) {
        update(player, prefs -> prefs.setConfigBoolean(option.key(), value));
    }

    public boolean isStranger(UUID viewer, UUID other) {
        if (viewer.equals(other)) {
            return false;
        }
        Set<UUID> known = knownCache.computeIfAbsent(viewer, repository::loadKnown);
        return !known.contains(other);
    }

    public void markKnown(UUID a, UUID b) {
        if (a.equals(b)) {
            return;
        }
        knownCache.computeIfAbsent(a, repository::loadKnown).add(b);
        knownCache.computeIfAbsent(b, repository::loadKnown).add(a);
        repository.markKnown(a, b);
    }

    public boolean canReceiveTpaFrom(Player target, Player requester) {
        if (requester.hasPermission("anarchycore.admin")) {
            return true;
        }
        PlayerPreferences prefs = get(target);
        if (!isStranger(target.getUniqueId(), requester.getUniqueId())) {
            return true;
        }
        return prefs.allowTpaFromStrangers();
    }

    public boolean canReceiveTpaHereFrom(Player target, Player requester) {
        if (requester.hasPermission("anarchycore.admin")) {
            return true;
        }
        PlayerPreferences prefs = get(target);
        if (!isStranger(target.getUniqueId(), requester.getUniqueId())) {
            return true;
        }
        return prefs.allowTpaHereFromStrangers();
    }

    public boolean canReceiveTellFrom(Player target, Player sender) {
        if (sender.hasPermission("anarchycore.admin")) {
            return true;
        }
        PlayerPreferences prefs = get(target);
        if (!isStranger(target.getUniqueId(), sender.getUniqueId())) {
            return true;
        }
        return prefs.allowTellFromStrangers();
    }

    public boolean canReceivePayFrom(Player target, Player sender) {
        if (sender.hasPermission("anarchycore.admin")) {
            return true;
        }
        PlayerPreferences prefs = get(target);
        if (!isStranger(target.getUniqueId(), sender.getUniqueId())) {
            return true;
        }
        return prefs.allowPayFromStrangers();
    }
}
