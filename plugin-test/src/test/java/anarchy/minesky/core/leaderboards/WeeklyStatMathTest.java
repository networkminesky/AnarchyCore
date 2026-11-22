package anarchy.minesky.core.leaderboards;

import anarchy.minesky.core.impl.stats.WeeklyStatMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeeklyStatMathTest {

    @Test
    void monotonicWeeklyNeverNegative() {
        assertEquals(50, WeeklyStatMath.monotonicWeekly(150, 100));
        assertEquals(0, WeeklyStatMath.monotonicWeekly(90, 100));
    }

    @Test
    void economyWeeklyClampsSpending() {
        assertEquals(609, WeeklyStatMath.economyWeekly(1609, 1000), 0.001);
        assertEquals(0, WeeklyStatMath.economyWeekly(500, 1000), 0.001);
    }

    @Test
    void mergeEconomyKeepsPeakGainBetweenSyncs() {
        assertEquals(609, WeeklyStatMath.mergeEconomyScore(400, 609), 0.001);
        assertEquals(400, WeeklyStatMath.mergeEconomyScore(400, 100), 0.001);
    }
}