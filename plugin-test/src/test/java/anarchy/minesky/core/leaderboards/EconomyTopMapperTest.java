package anarchy.minesky.core.leaderboards;

import anarchy.minesky.core.impl.economy.AnarchyEconomyQueryProvider.EconomyRankEntry;
import anarchy.minesky.core.impl.economy.EconomyTopMapper;
import anarchy.minesky.core.impl.economy.EconomyTopMapper.EconomyTopRow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EconomyTopMapperTest {

    @Test
    void mapsShardsStyleRowsWithPositions() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        List<EconomyTopRow> rows = List.of(
                new EconomyTopRow(a, "Alpha", 609),
                new EconomyTopRow(b, "Beta", 120),
                new EconomyTopRow(UUID.randomUUID(), "Zero", 0));
        List<EconomyRankEntry> top = EconomyTopMapper.map(rows, 200);
        assertEquals(2, top.size());
        assertEquals(1, top.get(0).position());
        assertEquals(a, top.get(0).uuid());
        assertEquals(609, top.get(0).value(), 0.001);
        assertEquals(2, top.get(1).position());
        assertEquals("Beta", top.get(1).name());
    }

    @Test
    void respectsLimitLikeTopShardsParameter() {
        UUID x = UUID.randomUUID();
        UUID y = UUID.randomUUID();
        List<EconomyTopRow> rows = List.of(
                new EconomyTopRow(x, "x", 10),
                new EconomyTopRow(y, "y", 9));
        List<EconomyRankEntry> top = EconomyTopMapper.map(rows, 1);
        assertEquals(1, top.size());
        assertEquals(x, top.get(0).uuid());
    }
}