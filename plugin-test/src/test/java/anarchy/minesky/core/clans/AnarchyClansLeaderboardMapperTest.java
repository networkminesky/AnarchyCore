package anarchy.minesky.core.clans;

import anarchy.minesky.core.impl.clans.AnarchyClansLeaderboardMapper;
import anarchy.minesky.core.impl.clans.AnarchyClansLeaderboardMapper.ClansLeaderboardEntry;
import anarchy.minesky.core.impl.leaderboards.LeaderboardService.LeaderboardLine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AnarchyClansLeaderboardMapperTest {

    @Test
    void mapsEntriesWithPositionsAndSkipsZeros() {
        List<ClansLeaderboardEntry> source = List.of(
                new ClansLeaderboardEntry("Alpha", 500),
                new ClansLeaderboardEntry("Beta", 0),
                new ClansLeaderboardEntry("Gamma", 120));
        List<LeaderboardLine> lines = AnarchyClansLeaderboardMapper.toLines(source, 200);
        assertEquals(2, lines.size());
        assertEquals(1, lines.get(0).position());
        assertEquals("Alpha", lines.get(0).name());
        assertEquals(500, lines.get(0).value());
        assertEquals(2, lines.get(1).position());
        assertEquals("Gamma", lines.get(1).name());
    }

    @Test
    void respectsLimitBelowClansCacheSize() {
        List<ClansLeaderboardEntry> source = List.of(
                new ClansLeaderboardEntry("a", 1),
                new ClansLeaderboardEntry("b", 2),
                new ClansLeaderboardEntry("c", 3));
        List<LeaderboardLine> lines = AnarchyClansLeaderboardMapper.toLines(source, 2);
        assertEquals(2, lines.size());
        assertEquals("a", lines.get(0).name());
        assertEquals("b", lines.get(1).name());
    }

    @Test
    void resolveUuidFromNicknameIsStable() {
        UUID first = AnarchyClansLeaderboardMapper.resolveUuid("Notch");
        UUID second = AnarchyClansLeaderboardMapper.resolveUuid("Notch");
        assertEquals(first, second);
        assertFalse(first.equals(new UUID(0L, 0L)));
    }
}