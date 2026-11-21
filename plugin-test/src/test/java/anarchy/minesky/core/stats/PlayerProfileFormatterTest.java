package anarchy.minesky.core.stats;

import anarchy.minesky.core.impl.profile.PlayerProfileFormatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerProfileFormatterTest {

    @Test
    void formatMoneyLine_donutCompactColors() {
        assertEquals("&7Dinheiro: &a$ &f5.7T", PlayerProfileFormatter.formatMoneyLine(5.7e12));
        assertEquals("&7Dinheiro: &a$ &f1.5K", PlayerProfileFormatter.formatMoneyLine(1500));
    }

    @Test
    void formatPlaytimeLine_donutCompactColors() {
        assertEquals(
                "&7Tempo Jogado: &f148d 3h",
                PlayerProfileFormatter.formatPlaytimeLine(148 * 86_400L + 3 * 3600L));
        assertEquals("&7Tempo Jogado: &f1h 53m", PlayerProfileFormatter.formatPlaytimeLine(6780));
    }

    @Test
    void formatKillsAndDeaths_compactColors() {
        assertEquals("&7Abates: &f3.1K", PlayerProfileFormatter.formatKillsLine(3100));
        assertEquals("&7Mortes: &f684", PlayerProfileFormatter.formatDeathsLine(684));
    }

    @Test
    void chestTitle_includesNick() {
        assertTrue(PlayerProfileFormatter.chestTitle("zBruno").contains("ᴢʙʀᴜɴᴏ"));
    }
}