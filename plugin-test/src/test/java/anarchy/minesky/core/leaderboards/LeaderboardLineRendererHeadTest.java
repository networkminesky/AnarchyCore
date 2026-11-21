package anarchy.minesky.core.leaderboards;

import anarchy.minesky.core.impl.leaderboards.LeaderboardLineRenderer;
import anarchy.minesky.core.impl.leaderboards.LeaderboardRankStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LeaderboardLineRendererHeadTest {

    @Test
    void goldRankLineKeepsHeadWhite() {
        Component head = Component.text("H");
        Component line = Component.text("#1 ", LeaderboardRankStyle.positionColor(1))
                .append(LeaderboardLineRenderer.playerHeadLinePart(head))
                .append(Component.text("Player", LeaderboardRankStyle.nameColor(1)));
        assertNotNull(line.children().get(0).color());
        assertEquals(NamedTextColor.WHITE, line.children().get(0).color());
        assertEquals(LeaderboardRankStyle.GOLD_FIRST, line.children().get(1).color());
    }

    @Test
    void bronzeRankLineKeepsHeadWhite() {
        Component head = Component.text("H");
        Component line = Component.text("#3 ", LeaderboardRankStyle.positionColor(3))
                .append(LeaderboardLineRenderer.playerHeadLinePart(head))
                .append(Component.text("Player", LeaderboardRankStyle.nameColor(3)));
        assertEquals(NamedTextColor.WHITE, line.children().get(0).color());
        assertEquals(LeaderboardRankStyle.BRONZE_THIRD, line.children().get(1).color());
    }
}