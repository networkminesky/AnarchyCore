package anarchy.minesky.core.leaderboards;

import anarchy.minesky.core.impl.leaderboards.LeaderboardInteractions;
import anarchy.minesky.core.impl.leaderboards.LeaderboardService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardInteractionsTest {

    @Test
    void ownLineAlwaysClickable() {
        UUID self = UUID.randomUUID();
        assertTrue(LeaderboardInteractions.profileClickAllowed(self, self, false));
    }

    @Test
    void othersRequirePermission() {
        UUID viewer = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        assertFalse(LeaderboardInteractions.profileClickAllowed(viewer, other, false));
        assertTrue(LeaderboardInteractions.profileClickAllowed(viewer, other, true));
    }

    @Test
    void serviceRespectsTop200Cap() {
        org.junit.jupiter.api.Assertions.assertEquals(200, LeaderboardService.TOP_FULL);
        org.junit.jupiter.api.Assertions.assertEquals(10, LeaderboardService.TOP_PREVIEW);
    }
}