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
import anarchy.minesky.core.impl.stats.LeaderboardWeekKey;
import anarchy.minesky.core.impl.stats.PlayerStatsWeeklyRepository;
import anarchy.minesky.core.impl.stats.WeeklyStatsSyncService;
import anarchy.minesky.core.referral.ReferralFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

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

    @Test
    void weeklyKillsPrefersAnarchyClansOverMariaDb() {
        PlayerStatsWeeklyRepository weeklyRepo = new PlayerStatsWeeklyRepository(database);
        LeaderboardSettings settings = new LeaderboardSettings(database.plugin());
        String weekKey = LeaderboardWeekKey.current(ZoneId.of("America/Sao_Paulo"));
        UUID dbLeader = UUID.randomUUID();
        UUID clansLeader = UUID.randomUUID();
        UUID viewer = clansLeader;

        weeklyRepo.saveSync(new PlayerStatsWeeklyRepository.WeeklyRow(
                dbLeader, weekKey, "dbLeader",
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 999, 0, 0, 0, 0, 0));

        AnarchyClansQueryProvider clans = new AnarchyClansQueryProvider(database.plugin(), new CombatStatsRepository(database));
        clans.setTestClansAvailableForTests(true);
        clans.setTestWeeklyKillsTopForTests(
                List.of(new LeaderboardService.LeaderboardLine(1, clansLeader, "clansLeader", 42)));

        PlayerStatsRepository statsRepo = new PlayerStatsRepository(database);
        WeeklyStatsSyncService weeklySync = new WeeklyStatsSyncService(weeklyRepo, settings);
        PlayerStatsService stats = new PlayerStatsService(database.plugin(), statsRepo, clans, weeklySync);
        LeaderboardService clansFirst = new LeaderboardService(
                database.plugin(), stats, clans, new AnarchyEconomyQueryProvider(database.plugin()), weeklyRepo, settings);

        LeaderboardService.LeaderboardResult result =
                clansFirst.loadWeeklyForTests(LeaderboardCategory.KILLS, viewer, 10);
        assertTrue(result.ok());
        assertEquals(1, result.top().size());
        assertEquals("clansLeader", result.top().getFirst().name());
        assertEquals(42, result.top().getFirst().value());
        assertEquals(1, result.viewerRank());
        assertEquals(42, result.viewerValue());
    }
}