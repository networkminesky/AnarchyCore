package anarchy.minesky.core.leaderboards;

import anarchy.minesky.core.impl.leaderboards.LeaderboardCategory;
import anarchy.minesky.core.impl.leaderboards.LeaderboardFormatter;
import anarchy.minesky.core.impl.leaderboards.LeaderboardPeriod;
import anarchy.minesky.core.impl.stats.LeaderboardWeekKey;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardPeriodUiTest {

    @Test
    void toggleButtonLabels() {
        assertEquals("Ver Semanal", LeaderboardFormatter.periodToggleLabel(LeaderboardPeriod.ALL_TIME));
        assertEquals("Ver Tudo", LeaderboardFormatter.periodToggleLabel(LeaderboardPeriod.WEEKLY));
    }

    @Test
    void titlesIncludePeriod() {
        assertEquals("Top Kills • Tudo", LeaderboardFormatter.previewTitle(LeaderboardCategory.KILLS, LeaderboardPeriod.ALL_TIME));
        assertEquals("Top Fragmentos • Tudo",
                LeaderboardFormatter.previewTitle(LeaderboardCategory.SHARDS, LeaderboardPeriod.ALL_TIME));
        assertTrue(LeaderboardFormatter.fullTitle(LeaderboardCategory.MONEY, LeaderboardPeriod.ALL_TIME).contains("Top 200 • Tudo"));
    }

    @Test
    void economyCategoriesSkipWeeklyToggle() {
        assertFalse(LeaderboardCategory.MONEY.supportsWeekly());
        assertFalse(LeaderboardCategory.SHARDS.supportsWeekly());
        assertTrue(LeaderboardCategory.PLAYTIME.supportsWeekly());
    }

    @Test
    void weekKeyUsesSundayDateFormat() {
        String w1 = LeaderboardWeekKey.current(ZoneId.of("America/Sao_Paulo"));
        assertNotEquals("", w1);
        assertTrue(w1.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void periodToggleSwitchesMode() {
        assertEquals(LeaderboardPeriod.WEEKLY, LeaderboardPeriod.ALL_TIME.toggle());
        assertEquals(LeaderboardPeriod.ALL_TIME, LeaderboardPeriod.WEEKLY.toggle());
    }
}