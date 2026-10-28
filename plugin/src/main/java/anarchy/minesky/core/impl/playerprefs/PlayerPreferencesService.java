package anarchy.minesky.core.impl.playerprefs;

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

    public PlayerPreferencesService(PlayerPreferencesRepository repository) {
        this.repository = repository;
    }

    public void loadPlayer(UUID playerId) {
        cache.put(playerId, repository.load(playerId));
        knownCache.put(playerId, repository.loadKnown(playerId));
    }

    public void unloadPlayer(UUID playerId) {
        PlayerPreferences prefs = cache.remove(playerId);
        if (prefs != null) {
            repository.save(playerId, prefs);
        }
        knownCache.remove(playerId);
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
