package anarchy.minesky.core.escmenu;

import anarchy.minesky.core.impl.escmenu.flows.VisibleOnlinePlayers;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisibleOnlinePlayersFilterTest {

  private static final UUID VIEWER = UUID.randomUUID();
  private static final UUID TARGET = UUID.randomUUID();

    @Test
    void rejectsSelf() {
        assertFalse(VisibleOnlinePlayers.passesFilter(VIEWER, VIEWER, true, false, true));
    }

    @Test
    void rejectsVanished() {
        assertFalse(VisibleOnlinePlayers.passesFilter(VIEWER, TARGET, true, true, true));
    }

    @Test
    void rejectsWhenCannotSee() {
        assertFalse(VisibleOnlinePlayers.passesFilter(VIEWER, TARGET, true, false, false));
    }

    @Test
    void acceptsVisibleTarget() {
        assertTrue(VisibleOnlinePlayers.passesFilter(VIEWER, TARGET, true, false, true));
    }
}