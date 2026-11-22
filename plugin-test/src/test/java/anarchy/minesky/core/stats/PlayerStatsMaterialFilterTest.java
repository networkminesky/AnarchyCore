package anarchy.minesky.core.stats;

import anarchy.minesky.core.impl.stats.PlayerStatsMaterialFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrato do filtro aplicado em {@code PlayerStatsMaterialIndex} (sem tocar no enum Bukkit em teste).
 */
class PlayerStatsMaterialFilterTest {

    @Test
    void legacyMustBeSkippedBeforeBlockCheck() {
        assertTrue(PlayerStatsMaterialFilter.shouldSkipLegacyFirst(true, false));
        assertTrue(PlayerStatsMaterialFilter.shouldIncludeMine(false, true));
        assertFalse(PlayerStatsMaterialFilter.shouldIncludeMine(true, true));
    }

    @Test
    void useRequiresNonLegacyBlockAndItem() {
        assertTrue(PlayerStatsMaterialFilter.shouldIncludeUse(false, true, true));
        assertFalse(PlayerStatsMaterialFilter.shouldIncludeUse(true, true, true));
        assertFalse(PlayerStatsMaterialFilter.shouldIncludeUse(false, true, false));
    }
}