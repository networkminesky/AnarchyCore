package anarchy.minesky.core.stats;

import anarchy.minesky.core.impl.combat.CombatStatsKeys;
import anarchy.minesky.core.impl.combat.CombatStatsRepository;
import anarchy.minesky.core.impl.combat.CombatStatsJsonSql;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatStatsRepositoryJsonTest {

    @Test
    void readsNestedAndFlatJsonKeys() {
        String nested = CombatStatsRepository.sampleDataJson(42, 7);
        assertEquals(42, CombatStatsRepository.parseFromDataJson(nested, CombatStatsKeys.KILLS));
        assertEquals(7, CombatStatsRepository.parseFromDataJson(nested, CombatStatsKeys.DEATHS));

        String flat = "{\"stats.kills\":100,\"stats.deaths\":9}";
        assertEquals(100, CombatStatsRepository.parseFromDataJson(flat, CombatStatsKeys.KILLS));
        assertEquals(9, CombatStatsRepository.parseFromDataJson(flat, CombatStatsKeys.DEATHS));
    }

    @Test
    void jsonSqlPathsMatchMariaDbStyle() {
        assertEquals("$.stats.kills", CombatStatsJsonSql.nestedJsonPath(CombatStatsKeys.KILLS));
        assertEquals("$.\"stats.kills\"", CombatStatsJsonSql.flatJsonPath(CombatStatsKeys.KILLS));
        assertTrue(CombatStatsJsonSql.statValueExpression(CombatStatsKeys.KILLS).contains("JSON_EXTRACT"));
    }
}