package anarchy.minesky.core.leaderboards;

import anarchy.minesky.core.impl.leaderboards.LeaderboardFormatter;
import anarchy.minesky.core.impl.leaderboards.LeaderboardRankStyle;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaderboardCompactFormatTest {

    @Test
    void compactNumberSuffixes() {
        assertEquals("3.1K", LeaderboardFormatter.formatCompactNumber(3100));
        assertEquals("5.7T", LeaderboardFormatter.formatCompactNumber(5.7e12));
        assertEquals("977B", LeaderboardFormatter.formatCompactNumber(977e9));
        assertEquals("684", LeaderboardFormatter.formatCompactNumber(684));
    }

    @Test
    void compactPlaytimeDaysAndHours() {
        assertEquals("148d 3h", LeaderboardFormatter.formatCompactPlaytime(148 * 86_400 + 3 * 3600));
        assertEquals("1h 53m", LeaderboardFormatter.formatCompactPlaytime(3600 + 53 * 60));
    }

    @Test
    void compactMoneyDisplay() {
        assertEquals("$ 5.7T", LeaderboardFormatter.formatCompactMoneyValue(5.7e12));
    }

    @Test
    void rankColorsTopThreeAndRest() {
        assertEquals(LeaderboardRankStyle.GOLD_FIRST, LeaderboardRankStyle.positionColor(1));
        assertEquals(TextColor.color(0xFFFFFF), LeaderboardRankStyle.positionColor(2));
        assertEquals(LeaderboardRankStyle.BRONZE_THIRD, LeaderboardRankStyle.positionColor(3));
        assertEquals(LeaderboardRankStyle.GRAY_RANK, LeaderboardRankStyle.positionColor(4));
        assertEquals(LeaderboardRankStyle.GRAY_RANK, LeaderboardRankStyle.positionColor(200));
    }
}