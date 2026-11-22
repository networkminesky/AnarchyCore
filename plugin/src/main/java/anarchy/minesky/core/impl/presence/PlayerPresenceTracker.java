package anarchy.minesky.core.impl.presence;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sessão online em memória + persistência assíncrona em {@code ac_player_presence}.
 */
public final class PlayerPresenceTracker {

    private final Plugin plugin;
    private final PlayerPresenceRepository repository;
    private final ConcurrentHashMap<UUID, Long> sessionStartMillis = new ConcurrentHashMap<>();

    public PlayerPresenceTracker(Plugin plugin, PlayerPresenceRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public void trackExistingOnline(Player player) {
        if (player == null) {
            return;
        }
        sessionStartMillis.putIfAbsent(player.getUniqueId(), System.currentTimeMillis());
    }

    public void onJoin(Player player) {
        if (player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        sessionStartMillis.put(player.getUniqueId(), now);
        repository.upsertJoinAsync(player.getUniqueId(), player.getName(), now);
    }

    public void onQuit(Player player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        sessionStartMillis.remove(uuid);
        long now = System.currentTimeMillis();
        Location location = player.getLocation();
        repository.saveQuitAsync(uuid, player.getName(), now, location);
    }

    public long sessionStartMillis(UUID uuid) {
        return sessionStartMillis.getOrDefault(uuid, 0L);
    }

    public Map<UUID, Long> sessionStartsView() {
        return Map.copyOf(sessionStartMillis);
    }

    public void flushOnlineSync() {
        long now = System.currentTimeMillis();
        var players = plugin.getServer().getOnlinePlayers();
        if (players.isEmpty()) {
            return;
        }
        CompletableFuture<?>[] waits = players.stream()
                .map(player -> {
                    CompletableFuture<Void> done = new CompletableFuture<>();
                    player.getScheduler().run(plugin, task -> {
                        try {
                            if (!player.isOnline()) {
                                return;
                            }
                            Location location = player.getLocation();
                            repository.saveQuitSync(player.getUniqueId(), player.getName(), now, location);
                            sessionStartMillis.remove(player.getUniqueId());
                        } finally {
                            done.complete(null);
                        }
                    }, null);
                    return done;
                })
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(waits).join();
    }
}