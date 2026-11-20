package anarchy.minesky.core.impl.scheduling;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Um único {@code runAtFixedRate} por jogador; vários consumidores com intervalos independentes.
 */
public final class PlayerTickHub {

    private final Plugin plugin;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public PlayerTickHub(Plugin plugin) {
        this.plugin = plugin;
    }

    public void subscribe(Player player, String subscriptionId, long intervalTicks, Consumer<Player> callback) {
        if (player == null || subscriptionId == null || subscriptionId.isBlank() || callback == null) {
            return;
        }
        long interval = Math.max(1L, intervalTicks);
        UUID uuid = player.getUniqueId();
        Session session = sessions.computeIfAbsent(uuid, id -> new Session());
        session.subscriptions.put(subscriptionId, new Subscription(interval, callback));
        ensureTask(player, session);
    }

    public void unsubscribe(Player player, String subscriptionId) {
        if (player == null || subscriptionId == null) {
            return;
        }
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        session.subscriptions.remove(subscriptionId);
        if (session.subscriptions.isEmpty()) {
            cancelSession(player.getUniqueId(), session);
        }
    }

    public void handleQuit(UUID playerId) {
        Session session = sessions.remove(playerId);
        if (session != null && session.task != null) {
            session.task.cancel();
        }
    }

    public void shutdown() {
        for (Session session : sessions.values()) {
            if (session.task != null) {
                session.task.cancel();
            }
        }
        sessions.clear();
    }

    private void ensureTask(Player player, Session session) {
        if (session.task != null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        session.task = player.getScheduler().runAtFixedRate(plugin, scheduled -> {
            if (!player.isOnline()) {
                scheduled.cancel();
                sessions.remove(uuid, session);
                return;
            }
            Session current = sessions.get(uuid);
            if (current == null || current != session || current.subscriptions.isEmpty()) {
                scheduled.cancel();
                if (current == session) {
                    sessions.remove(uuid, session);
                }
                return;
            }
            current.globalTick++;
            for (Subscription sub : current.subscriptions.values()) {
                if (current.globalTick - sub.lastRunTick < sub.intervalTicks) {
                    continue;
                }
                sub.lastRunTick = current.globalTick;
                try {
                    sub.callback.accept(player);
                } catch (Throwable ignored) {
                }
            }
        }, null, 1L, 1L);
    }

    private void cancelSession(UUID uuid, Session session) {
        if (session.task != null) {
            session.task.cancel();
            session.task = null;
        }
        sessions.remove(uuid, session);
    }

    private static final class Session {
        private ScheduledTask task;
        private long globalTick;
        private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    }

    private static final class Subscription {
        private final long intervalTicks;
        private final Consumer<Player> callback;
        private long lastRunTick;

        private Subscription(long intervalTicks, Consumer<Player> callback) {
            this.intervalTicks = intervalTicks;
            this.callback = callback;
        }
    }
}