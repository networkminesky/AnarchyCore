package anarchy.minesky.core.impl.stats;

import anarchy.minesky.core.impl.clans.AnarchyClansQueryProvider;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mantém {@code ac_player_stats} como cache de ranking (progression vanilla + abates/mortes de {@code ac_player_data}).
 */
public final class PlayerStatsService {

    private static final long FLUSH_INTERVAL_TICKS = 20L * 60L;

    private final Plugin plugin;
    private final PlayerStatsRepository repository;
    private final AnarchyClansQueryProvider combat;
    private final WeeklyStatsSyncService weeklySync;
    private final Map<UUID, PlayerStatsRecord> onlineCache = new ConcurrentHashMap<>();
    private ScheduledTask flushTask;

    public PlayerStatsService(
            Plugin plugin,
            PlayerStatsRepository repository,
            AnarchyClansQueryProvider combat,
            WeeklyStatsSyncService weeklySync) {
        this.plugin = plugin;
        this.repository = repository;
        this.combat = combat;
        this.weeklySync = weeklySync;
    }

    public PlayerStatsRepository repository() {
        return repository;
    }

    public boolean isActive() {
        return repository.isActive();
    }

    public void start() {
        if (!isActive()) {
            return;
        }
        Bukkit.getAsyncScheduler()
                .runNow(plugin, task -> PlayerStatsMaterialIndex.warmup());
        flushTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(
                plugin, task -> refreshAllOnline(), FLUSH_INTERVAL_TICKS, FLUSH_INTERVAL_TICKS);
    }

    public void shutdown() {
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            player.getScheduler().run(plugin, task -> {
                if (!player.isOnline()) {
                    return;
                }
                persistVanilla(player);
            }, null);
        }
        onlineCache.clear();
    }

    public void onJoin(Player player) {
        if (!isActive() || player == null) {
            return;
        }
        player.getScheduler().run(plugin, task -> persistVanilla(player), null);
    }

    public void onQuit(Player player) {
        if (!isActive() || player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        player.getScheduler().run(plugin, task -> {
            persistVanilla(player);
            onlineCache.remove(uuid);
        }, null);
    }

    public PlayerStatsRecord snapshot(UUID uuid) {
        PlayerStatsRecord cached = onlineCache.get(uuid);
        if (cached != null) {
            return cached;
        }
        PlayerStatsRecord stored = repository.find(uuid).orElse(null);
        if (stored == null || !combat.isActive()) {
            return stored;
        }
        return stored.withCombat(combat.killsSync(uuid), combat.deathsSync(uuid));
    }

    private void refreshAllOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getScheduler().run(plugin, task -> syncOnlinePlayer(player), null);
        }
    }

    private void syncOnlinePlayer(Player player) {
        if (!isActive() || player == null || !player.isOnline()) {
            return;
        }
        persistVanilla(player);
    }

    private void persistVanilla(Player player) {
        if (!isActive() || player == null) {
            return;
        }
        PlayerStatsRecord progression = PlayerStatsSeed.fromVanillaProgression(player);
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            PlayerStatsRecord fresh = progression.withCombat(combat.killsSync(uuid), combat.deathsSync(uuid));
            PlayerStatsRecord previous = onlineCache.get(uuid);
            onlineCache.put(uuid, fresh);
            if (weeklySync != null) {
                weeklySync.sync(uuid, playerName, fresh, fresh.kills(), fresh.deaths());
            }
            if (previous != null && PlayerStatsRecord.sameValues(previous, fresh)) {
                return;
            }
            repository.save(fresh);
        });
    }
}