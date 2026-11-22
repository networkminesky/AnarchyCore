package anarchy.minesky.core.presence;

import anarchy.minesky.core.impl.presence.PlayerPresenceRecord;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtpLocationMappingTest {

    @Test
    void incompleteRowsAreNotTeleportable() {
        UUID uuid = UUID.randomUUID();
        assertFalse(new PlayerPresenceRecord(uuid, "a", 1L, 2L, null, 1.0, 2.0, 3.0, 0f, 0f).hasLogoutLocation());
        assertFalse(new PlayerPresenceRecord(uuid, "a", 1L, 2L, "", 1.0, 2.0, 3.0, 0f, 0f).hasLogoutLocation());
    }

    @Test
    void hasLogoutLocationWhenWorldAndCoordinatesPresent() {
        UUID uuid = UUID.randomUUID();
        var record = new PlayerPresenceRecord(uuid, "a", 1L, 2L, "world_nether", 1.0, 64.0, -8.0, 180f, 10f);
        assertTrue(record.hasLogoutLocation());
    }
}