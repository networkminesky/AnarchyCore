package anarchy.minesky.core.leaderboards;

import anarchy.minesky.core.impl.clans.AnarchyClansQueryProvider;
import anarchy.minesky.core.impl.combat.CombatStatsRepository;
import anarchy.minesky.core.impl.database.DatabaseManager;
import anarchy.minesky.core.impl.economy.AnarchyEconomyQueryProvider;
import anarchy.minesky.core.impl.leaderboards.LeaderboardCategory;
import anarchy.minesky.core.impl.leaderboards.LeaderboardPeriod;
import anarchy.minesky.core.impl.leaderboards.LeaderboardService;
import anarchy.minesky.core.impl.stats.LeaderboardSettings;
import anarchy.minesky.core.impl.stats.PlayerStatsRepository;
import anarchy.minesky.core.impl.stats.PlayerStatsService;
import anarchy.minesky.core.impl.stats.PlayerStatsWeeklyRepository;
import anarchy.minesky.core.impl.stats.WeeklyStatsSyncService;
import anarchy.minesky.core.referral.ReferralFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardServiceCacheTest {

    private DatabaseManager database;
    private LeaderboardService service;

    @BeforeEach
    void setUp() {
        database = ReferralFixtures.database("lb_cache");
        PlayerStatsRepository statsRepo = new PlayerStatsRepository(database);
        CombatStatsRepository combatRepo = new CombatStatsRepository(database);
        AnarchyClansQueryProvider clans = new AnarchyClansQueryProvider(database.plugin(), combatRepo);
        PlayerStatsWeeklyRepository weekly = new PlayerStatsWeeklyRepository(database);
        LeaderboardSettings settings = new LeaderboardSettings(database.plugin());
        WeeklyStatsSyncService weeklySync = new WeeklyStatsSyncService(weekly, settings);
        AnarchyEconomyQueryProvider economy = new AnarchyEconomyQueryProvider(database.plugin());
        PlayerStatsService stats = new PlayerStatsService(database.plugin(), statsRepo, clans, weeklySync);
        service = new LeaderboardService(database.plugin(), stats, clans, economy, weekly, settings);
    }

    @Test
    void economyCategoriesDoNotSupportWeekly() {
        assertFalse(LeaderboardCategory.MONEY.supportsWeekly());
        assertFalse(LeaderboardCategory.SHARDS.supportsWeekly());
        assertTrue(LeaderboardCategory.KILLS.supportsWeekly());
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void cacheKeysDifferByPeriod() {
        service.clearCacheForTests();
        var empty = LeaderboardService.LeaderboardResult.unavailable(
                LeaderboardCategory.KILLS, LeaderboardPeriod.ALL_TIME, "");
        service.seedCacheForTests(LeaderboardCategory.KILLS, LeaderboardPeriod.ALL_TIME, empty);
        assertTrue(service.cacheContainsForTests(LeaderboardCategory.KILLS, LeaderboardPeriod.ALL_TIME));
        assertFalse(service.cacheContainsForTests(LeaderboardCategory.KILLS, LeaderboardPeriod.WEEKLY));
        service.seedCacheForTests(
                LeaderboardCategory.KILLS,
                LeaderboardPeriod.WEEKLY,
                LeaderboardService.LeaderboardResult.unavailable(
                        LeaderboardCategory.KILLS, LeaderboardPeriod.WEEKLY, ""));
        assertEquals(2, service.cacheSizeForTests());
    }
}