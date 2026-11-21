package anarchy.minesky.core.impl.leaderboards;

import anarchy.minesky.core.impl.clans.AnarchyClansQueryProvider;
import anarchy.minesky.core.impl.economy.AnarchyEconomyQueryProvider;
import anarchy.minesky.core.impl.stats.LeaderboardSettings;
import anarchy.minesky.core.impl.stats.PlayerStatsRecord;
import anarchy.minesky.core.impl.stats.PlayerStatsRepository;
import anarchy.minesky.core.impl.stats.PlayerStatsService;
import anarchy.minesky.core.impl.stats.PlayerStatsWeeklyRepository;
import anarchy.minesky.core.impl.stats.PlayerStatsWeeklyRepository.ScoreColumn;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class LeaderboardService {

    public static final int TOP_PREVIEW = 10;
    public static final int TOP_FULL = 200;

    /** @deprecated use {@link #TOP_PREVIEW} */
    @Deprecated
    public static final int TOP_LIMIT = TOP_PREVIEW;
    public static final long CACHE_TTL_MS = 30_000L;

    private final Plugin plugin;
    private final PlayerStatsService stats;
    private final AnarchyClansQueryProvider combat;
    private final AnarchyEconomyQueryProvider economy;
    private final PlayerStatsWeeklyRepository weekly;
    private final LeaderboardSettings settings;
    private final ConcurrentHashMap<LeaderboardCacheKey, CachedBoard> cache = new ConcurrentHashMap<>();

    public LeaderboardService(
            Plugin plugin,
            PlayerStatsService stats,
            AnarchyClansQueryProvider combat,
            AnarchyEconomyQueryProvider economy,
            PlayerStatsWeeklyRepository weekly,
            LeaderboardSettings settings) {
        this.plugin = plugin;
        this.stats = stats;
        this.combat = combat;
        this.economy = economy;
        this.weekly = weekly;
        this.settings = settings;
    }

    public void fetchCategoryPreview(
            LeaderboardCategory category, LeaderboardPeriod period, Player viewer, Consumer<LeaderboardResult> callback) {
        fetchCategory(category, period, viewer, TOP_PREVIEW, callback);
    }

    public void fetchCategoryFull(
            LeaderboardCategory category, LeaderboardPeriod period, Player viewer, Consumer<LeaderboardResult> callback) {
        fetchCategory(category, period, viewer, TOP_FULL, callback);
    }

    public void fetchCategory(
            LeaderboardCategory category,
            LeaderboardPeriod period,
            Player viewer,
            int limit,
            Consumer<LeaderboardResult> callback) {
        if (viewer == null || category == null || callback == null) {
            return;
        }
        LeaderboardPeriod safePeriod = normalizePeriod(category, period);
        int safeLimit = Math.min(Math.max(limit, 1), TOP_FULL);
        UUID viewerId = viewer.getUniqueId();
        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
            LeaderboardResult result = loadSync(category, safePeriod, viewerId, safeLimit);
            viewer.getScheduler().run(plugin, entity -> {
                if (!viewer.isOnline()) {
                    return;
                }
                callback.accept(result);
            }, null);
        });
    }

    private static LeaderboardPeriod normalizePeriod(LeaderboardCategory category, LeaderboardPeriod period) {
        LeaderboardPeriod resolved = period != null ? period : LeaderboardPeriod.ALL_TIME;
        if (resolved == LeaderboardPeriod.WEEKLY && !category.supportsWeekly()) {
            return LeaderboardPeriod.ALL_TIME;
        }
        return resolved;
    }

    private LeaderboardResult loadSync(
            LeaderboardCategory category, LeaderboardPeriod period, UUID viewerId, int limit) {
        period = normalizePeriod(category, period);
        long now = System.currentTimeMillis();
        LeaderboardCacheKey key = new LeaderboardCacheKey(category, period);
        CachedBoard cached = cache.get(key);
        if (cached != null
                && cached.expiresAtMillis() > now
                && cached.result().ok()
                && cached.result().top().size() >= Math.min(limit, cached.result().top().size())) {
            return slice(cached.result(), limit);
        }
        LeaderboardResult loaded;
        if (period == LeaderboardPeriod.WEEKLY) {
            loaded = loadWeekly(category, viewerId, limit);
        } else if (category.economy()) {
            loaded = loadEconomy(category, viewerId, limit);
        } else if (category == LeaderboardCategory.KILLS || category == LeaderboardCategory.DEATHS) {
            loaded = loadCombat(category, viewerId, limit);
        } else {
            loaded = loadStats(category, viewerId, limit);
        }
        loaded = withPeriod(loaded, period);
        if (loaded.ok() && limit >= TOP_FULL && !loaded.top().isEmpty()) {
            cache.put(key, new CachedBoard(loaded, now + CACHE_TTL_MS));
        }
        return loaded;
    }

    private static LeaderboardResult withPeriod(LeaderboardResult loaded, LeaderboardPeriod period) {
        if (loaded.period() == period) {
            return loaded;
        }
        return new LeaderboardResult(
                loaded.category(), period, loaded.top(), loaded.viewerRank(), loaded.viewerValue(), loaded.error());
    }

    private static LeaderboardResult slice(LeaderboardResult source, int limit) {
        int max = Math.min(limit, source.top().size());
        return new LeaderboardResult(
                source.category(),
                source.period(),
                source.top().subList(0, max),
                source.viewerRank(),
                source.viewerValue(),
                source.error());
    }

    private LeaderboardResult loadWeekly(LeaderboardCategory category, UUID viewerId, int limit) {
        if (!category.supportsWeekly()) {
            if (category.economy()) {
                return loadEconomy(category, viewerId, limit);
            }
            return LeaderboardResult.unavailable(
                    category, LeaderboardPeriod.ALL_TIME, "Ranking semanal indisponível para esta categoria");
        }
        if (!weekly.isActive()) {
            return LeaderboardResult.unavailable(category, LeaderboardPeriod.WEEKLY, "Ranking semanal indisponível (MariaDB)");
        }
        String weekKey = settings.currentWeekKey();
        ScoreColumn column = ScoreColumn.forCategory(category);
        List<LeaderboardLine> top = weekly.top(weekKey, column, limit);
        if (top.isEmpty() && (category == LeaderboardCategory.KILLS || category == LeaderboardCategory.DEATHS)) {
            boolean kills = category == LeaderboardCategory.KILLS;
            List<LeaderboardLine> clans = kills ? combat.topWeeklyKills(limit) : combat.topWeeklyDeaths(limit);
            if (!clans.isEmpty()) {
                top = clans;
            }
        }
        double selfValue = 0;
        int rank = -1;
        var row = weekly.find(viewerId, weekKey).orElse(null);
        if (row != null) {
            selfValue = weekly.scoreFor(row, column);
            rank = weekly.rank(weekKey, column, viewerId, selfValue);
            if (rank < 0) {
                rank = LeaderboardFormatter.viewerRankOrMinusOne(top, viewerId);
            }
        }
        return new LeaderboardResult(category, LeaderboardPeriod.WEEKLY, top, rank, selfValue, null);
    }

    private LeaderboardResult loadEconomy(LeaderboardCategory category, UUID viewerId, int limit) {
        if (!economy.isAvailable()) {
            return LeaderboardResult.unavailable(category, LeaderboardPeriod.ALL_TIME, "AnarchyEconomy offline");
        }
        CompletableFuture<List<AnarchyEconomyQueryProvider.EconomyRankEntry>> topFuture =
                category.shards() ? economy.topShards(limit) : economy.topMoney(limit);
        CompletableFuture<Double> selfFuture = category.shards()
                ? economy.balanceShards(viewerId)
                : economy.balanceMoney(viewerId);
        List<AnarchyEconomyQueryProvider.EconomyRankEntry> top = topFuture.join();
        double selfValue = selfFuture.join();
        List<LeaderboardLine> lines = new ArrayList<>();
        for (AnarchyEconomyQueryProvider.EconomyRankEntry row : top) {
            lines.add(new LeaderboardLine(row.position(), row.uuid(), row.name(), row.value()));
        }
        int selfRank = LeaderboardFormatter.viewerRankOrMinusOne(lines, viewerId);
        if (selfRank < 0) {
            selfRank = findEconomyRank(viewerId, selfValue, top);
        }
        return new LeaderboardResult(category, LeaderboardPeriod.ALL_TIME, lines, selfRank, selfValue, null);
    }

    private int findEconomyRank(
            UUID viewerId, double selfValue, List<AnarchyEconomyQueryProvider.EconomyRankEntry> top) {
        if (selfValue <= 0) {
            return -1;
        }
        for (AnarchyEconomyQueryProvider.EconomyRankEntry row : top) {
            if (viewerId.equals(row.uuid())) {
                return row.position();
            }
        }
        return -1;
    }

    private LeaderboardResult loadCombat(LeaderboardCategory category, UUID viewerId, int limit) {
        if (!combat.isActive()) {
            return LeaderboardResult.unavailable(
                    category, LeaderboardPeriod.ALL_TIME, "Abates/Mortes indisponíveis (AnarchyClans)");
        }
        boolean kills = category == LeaderboardCategory.KILLS;
        List<LeaderboardLine> top = kills ? combat.topKills(limit) : combat.topDeaths(limit);
        int selfValue = kills ? combat.killsSync(viewerId) : combat.deathsSync(viewerId);
        int rank = kills ? combat.rankKills(viewerId, selfValue) : combat.rankDeaths(viewerId, selfValue);
        return new LeaderboardResult(category, LeaderboardPeriod.ALL_TIME, top, rank, selfValue, null);
    }

    private LeaderboardResult loadStats(LeaderboardCategory category, UUID viewerId, int limit) {
        PlayerStatsRepository.SortColumn column = category.statsColumn();
        if (column == null || !stats.isActive()) {
            return LeaderboardResult.unavailable(
                    category, LeaderboardPeriod.ALL_TIME, "Estatísticas indisponíveis (MariaDB)");
        }
        List<PlayerStatsRecord> top = stats.repository().top(column, limit);
        List<LeaderboardLine> lines = new ArrayList<>();
        int position = 1;
        for (PlayerStatsRecord row : top) {
            lines.add(new LeaderboardLine(
                    position++, row.uuid(), row.playerName(), column.readValue(row)));
        }
        PlayerStatsRecord self = stats.snapshot(viewerId);
        long selfValue = self != null ? column.readValue(self) : 0L;
        int rank = self != null ? stats.repository().rank(column, selfValue) : -1;
        return new LeaderboardResult(category, LeaderboardPeriod.ALL_TIME, lines, rank, selfValue, null);
    }

    public void clearCacheForTests() {
        cache.clear();
    }

    public void seedCacheForTests(LeaderboardCategory category, LeaderboardPeriod period, LeaderboardResult result) {
        cache.put(
                new LeaderboardCacheKey(category, period),
                new CachedBoard(result, System.currentTimeMillis() + CACHE_TTL_MS));
    }

    public int cacheSizeForTests() {
        return cache.size();
    }

    public boolean cacheContainsForTests(LeaderboardCategory category, LeaderboardPeriod period) {
        return cache.containsKey(new LeaderboardCacheKey(category, period));
    }

    record LeaderboardCacheKey(LeaderboardCategory category, LeaderboardPeriod period) {
        LeaderboardCacheKey {
            Objects.requireNonNull(category);
            Objects.requireNonNull(period);
        }
    }

    record CachedBoard(LeaderboardResult result, long expiresAtMillis) {}

    public record LeaderboardLine(int position, UUID uuid, String name, double value) {}

    public record LeaderboardResult(
            LeaderboardCategory category,
            LeaderboardPeriod period,
            List<LeaderboardLine> top,
            int viewerRank,
            double viewerValue,
            String error) {

        public static LeaderboardResult unavailable(
                LeaderboardCategory category, LeaderboardPeriod period, String error) {
            return new LeaderboardResult(category, period, List.of(), -1, 0, error);
        }

        public boolean ok() {
            return error == null || error.isBlank();
        }
    }
}