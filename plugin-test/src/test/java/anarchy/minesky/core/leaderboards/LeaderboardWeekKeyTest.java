package anarchy.minesky.core.leaderboards;

import anarchy.minesky.core.impl.stats.LeaderboardWeekKey;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardWeekKeyTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Test
    void weekKeyIsSundayDateNotIsoWeek() {
        String key = LeaderboardWeekKey.current(ZONE);
        assertTrue(key.matches("\\d{4}-\\d{2}-\\d{2}"), () -> "formato inesperado: " + key);
        assertEquals(DayOfWeek.SUNDAY, LocalDate.parse(key).getDayOfWeek());
    }

    @Test
    void weekKeyRollsOnSundayMidnightBoundary() {
        // Domingo permanece na mesma chave; segunda usa o domingo anterior.
        assertEquals("2026-11-22", LeaderboardWeekKey.forDate(LocalDate.of(2026, 11, 22), ZONE));
        assertEquals("2026-11-22", LeaderboardWeekKey.forDate(LocalDate.of(2026, 11, 23), ZONE));
        assertEquals("2026-11-22", LeaderboardWeekKey.forDate(LocalDate.of(2026, 11, 28), ZONE));
        assertEquals("2026-11-29", LeaderboardWeekKey.forDate(LocalDate.of(2026, 11, 29), ZONE));
    }
}