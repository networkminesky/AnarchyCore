package anarchy.minesky.core.leaderboards;

import anarchy.minesky.core.impl.leaderboards.LeaderboardCategory;
import anarchy.minesky.core.impl.leaderboards.LeaderboardFormatter;
import anarchy.minesky.core.impl.leaderboards.LeaderboardService;
import anarchy.minesky.core.impl.stats.PlayerStatsSeed;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaderboardFormatterTest {

    @Test
    void playtimeFormatting() {
        assertEquals("45m", LeaderboardFormatter.formatPlaytime(45 * 60));
        assertEquals("2h 15m", LeaderboardFormatter.formatPlaytime(2 * 3600 + 15 * 60));
    }

    @Test
    void ticksToSecondsMatchesFormatter() {
        assertEquals("10m", LeaderboardFormatter.formatPlaytime(PlayerStatsSeed.ticksToSeconds(20 * 60 * 10)));
    }

    @Test
    void moneyFormatting() {
        assertEquals("$1500", LeaderboardFormatter.formatMoney(1500));
    }

    @Test
    void playtimeStillSupportsHourMinutes() {
        assertEquals("2h 15m", LeaderboardFormatter.formatPlaytime(2 * 3600 + 15 * 60));
    }

    @Test
    void viewerRankLookup() {
        UUID id = UUID.randomUUID();
        List<LeaderboardService.LeaderboardLine> lines = List.of(
                new LeaderboardService.LeaderboardLine(1, UUID.randomUUID(), "A", 10),
                new LeaderboardService.LeaderboardLine(2, id, "B", 5));
        assertEquals(2, LeaderboardFormatter.viewerRankOrMinusOne(lines, id));
        assertEquals(-1, LeaderboardFormatter.viewerRankOrMinusOne(lines, UUID.randomUUID()));
    }

    @Test
    void categoryFormattingKills() {
        assertEquals("100", LeaderboardFormatter.formatValue(LeaderboardCategory.KILLS, 100));
    }
}