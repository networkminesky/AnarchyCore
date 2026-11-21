package anarchy.minesky.core.stats;

import anarchy.minesky.core.impl.database.DatabaseManager;
import anarchy.minesky.core.impl.stats.LeaderboardSettings;
import anarchy.minesky.core.impl.stats.LeaderboardWeekKey;
import anarchy.minesky.core.impl.stats.PlayerStatsRecord;
import anarchy.minesky.core.impl.stats.PlayerStatsWeeklyRepository;
import anarchy.minesky.core.impl.stats.WeeklyStatsSyncService;
import anarchy.minesky.core.referral.ReferralFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyStatsSyncIntegrationTest {

    private DatabaseManager database;
    private PlayerStatsWeeklyRepository repository;
    private WeeklyStatsSyncService sync;
    private String weekKey;

    @BeforeEach
    void setUp() {
        database = ReferralFixtures.database("weekly_stats");
        repository = new PlayerStatsWeeklyRepository(database);
        sync = new WeeklyStatsSyncService(repository, new LeaderboardSettings(database.plugin()));
        weekKey = LeaderboardWeekKey.current(ZoneId.of("America/Sao_Paulo"));
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void syncTracksWeeklyGainsWithoutEconomy() {
        UUID uuid = UUID.randomUUID();
        PlayerStatsRecord base = new PlayerStatsRecord(uuid, "tester", 0, 0, 1000, 5, 3, 2);
        sync.sync(uuid, "tester", base, 10, 2);
        var row = repository.find(uuid, weekKey).orElseThrow();
        assertEquals(0, row.scoreKills());
        assertEquals(0, row.scoreMoney(), 0.001);

        PlayerStatsRecord progressed = base.withCombat(60, 5);
        sync.sync(uuid, "tester", progressed, 60, 5);
        var after = repository.find(uuid, weekKey).orElseThrow();
        assertEquals(50, after.scoreKills());
        assertEquals(0, after.scoreMoney(), 0.001);

        sync.sync(uuid, "tester", progressed, 60, 5);
        var again = repository.find(uuid, weekKey).orElseThrow();
        assertEquals(50, again.scoreKills());
        assertEquals(0, again.scoreMoney(), 0.001);
        assertEquals(0, again.scoreShards(), 0.001);
    }
}