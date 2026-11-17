package anarchy.minesky.core.impl.referral;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ReferralQualificationTracker {

    private static final double MIN_MOVE = 0.05;
    private static final double MAX_MOVE = 12.0;

    private static final class Session {
        long referralId;
        long activePlaytimeMs;
        double distanceTraveled;
        int blocksMined;
        int blocksPlaced;
        long lastActivityMs;
        int ticks;
    }

    private final Plugin plugin;
    private final ReferralSettings settings;
    private final ReferralRepository repository;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private volatile Consumer<Player> onTick;

    public ReferralQualificationTracker(Plugin plugin, ReferralSettings settings, ReferralRepository repository) {
        this.plugin = plugin;
        this.settings = settings;
        this.repository = repository;
    }

    public void setOnTick(Consumer<Player> onTick) {
        this.onTick = onTick;
    }

    public void bindPending(Player player, ReferralRecord record) {
        if (record == null || record.status() != ReferralStatus.PENDING || !player.isOnline()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        Session session = new Session();
        session.referralId = record.id();
        session.activePlaytimeMs = record.activePlaytimeMs();
        session.distanceTraveled = record.distanceTraveled();
        session.blocksMined = record.blocksMined();
        session.blocksPlaced = record.blocksPlaced();
        session.lastActivityMs = System.currentTimeMillis();
        sessions.put(uuid, session);
        ScheduledTask previous = tasks.remove(uuid);
        if (previous != null) {
            previous.cancel();
        }
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, scheduled -> {
            if (!player.isOnline() || sessions.get(uuid) == null) {
                scheduled.cancel();
                tasks.remove(uuid, scheduled);
                return;
            }
            tickPlaytime(uuid);
            Consumer<Player> listener = onTick;
            if (listener != null) {
                listener.accept(player);
            }
        }, null, 20L, 20L);
        tasks.put(uuid, task);
    }

    public void unbind(UUID uuid) {
        flush(uuid);
        ScheduledTask task = tasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        sessions.remove(uuid);
    }

    public void onMove(Player player, Location from, Location to) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null || from == null || to == null || from.getWorld() == null) {
            return;
        }
        if (!from.getWorld().equals(to.getWorld())) {
            return;
        }
        double distance = from.distance(to);
        if (distance < MIN_MOVE) {
            return;
        }
        session.lastActivityMs = System.currentTimeMillis();
        if (distance <= MAX_MOVE) {
            session.distanceTraveled += distance;
        }
    }

    public void onBlockBreak(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        session.blocksMined++;
        session.lastActivityMs = System.currentTimeMillis();
    }

    public void onBlockPlace(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        session.blocksPlaced++;
        session.lastActivityMs = System.currentTimeMillis();
    }

    public void onActivity(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        session.lastActivityMs = System.currentTimeMillis();
    }

    public boolean isQualified(UUID uuid) {
        Session session = sessions.get(uuid);
        if (session == null) {
            ReferralRecord row = repository.getByInvitee(uuid);
            if (row == null || row.status() != ReferralStatus.PENDING) {
                return false;
            }
            return meets(row.activePlaytimeMs(), row.distanceTraveled(), row.blocksMined(), row.blocksPlaced());
        }
        return meets(session.activePlaytimeMs, session.distanceTraveled, session.blocksMined, session.blocksPlaced);
    }

    public void flush(UUID uuid) {
        Session session = sessions.get(uuid);
        if (session == null) {
            return;
        }
        repository.updateQualificationProgress(
                session.referralId,
                session.activePlaytimeMs,
                session.distanceTraveled,
                session.blocksMined,
                session.blocksPlaced);
    }

    public void flushAll() {
        sessions.keySet().forEach(this::flush);
    }

    private void tickPlaytime(UUID uuid) {
        Session session = sessions.get(uuid);
        if (session == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - session.lastActivityMs <= settings.afkTimeoutMs()) {
            session.activePlaytimeMs += 1000L;
        }
        session.ticks++;
        if (session.ticks % 30 == 0) {
            flush(uuid);
        }
    }

    private boolean meets(long playtimeMs, double distance, int mined, int placed) {
        return playtimeMs >= settings.minActivePlaytimeMs()
                && distance >= settings.minDistanceTraveled()
                && mined >= settings.minBlocksMined()
                && placed >= settings.minBlocksPlaced();
    }
}
