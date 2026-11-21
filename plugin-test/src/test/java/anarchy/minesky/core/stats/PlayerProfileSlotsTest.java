package anarchy.minesky.core.stats;

import anarchy.minesky.core.impl.profile.PlayerProfileGuiBuilder;
import anarchy.minesky.core.impl.profile.PlayerProfileSlots;
import anarchy.minesky.core.impl.profile.PlayerProfileSnapshot;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerProfileSlotsTest {

  @Test
    void layoutRowTwoSlots() {
        assertArrayEquals(new int[] {10, 11, 12, 13, 14, 15, 16}, PlayerProfileSlots.dataRowTwo());
    }

    @Test
    void statCellMaterials() {
        assertEquals(Material.EMERALD, PlayerProfileSlots.StatCell.MONEY.icon());
        assertEquals(Material.ZOMBIE_HEAD, PlayerProfileSlots.StatCell.MOBS_KILLED.icon());
        assertEquals(27, PlayerProfileSlots.StatCell.BACK_BUTTON.slot());
    }

    @Test
    void fillerSlotsAreNotStatSlots() {
        assertFalse(PlayerProfileSlots.isStatSlot(0));
        assertFalse(PlayerProfileSlots.isStatSlot(9));
        assertTrue(PlayerProfileSlots.isStatSlot(12));
    }

    @Test
    void loreMappingMatchesSnapshot() {
        PlayerProfileSnapshot snap = new PlayerProfileSnapshot(
                UUID.randomUUID(), "Test", 10, 2, 3600, 100, 200, 5, 99.5, 12);
        assertEquals("$ 99.50", PlayerProfileGuiBuilder.loreForCell(PlayerProfileSlots.StatCell.MONEY, snap));
        assertEquals("12", PlayerProfileGuiBuilder.loreForCell(PlayerProfileSlots.StatCell.SHARDS, snap));
        assertEquals("1h 0m", PlayerProfileGuiBuilder.loreForCell(PlayerProfileSlots.StatCell.PLAYTIME, snap));
        assertEquals("ᴠᴏʟᴛᴀʀ", PlayerProfileGuiBuilder.loreForCell(PlayerProfileSlots.StatCell.BACK_BUTTON, snap));
    }
}