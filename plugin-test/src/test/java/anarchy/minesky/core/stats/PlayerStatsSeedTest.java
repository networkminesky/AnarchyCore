package anarchy.minesky.core.stats;

import anarchy.minesky.core.impl.stats.PlayerStatsSeed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerStatsSeedTest {

    @Test
    void ticksToSecondsHandlesZeroAndNegative() {
        assertEquals(0L, PlayerStatsSeed.ticksToSeconds(0));
        assertEquals(0L, PlayerStatsSeed.ticksToSeconds(-20));
        assertEquals(30L, PlayerStatsSeed.ticksToSeconds(600));
    }
}