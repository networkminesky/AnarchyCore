package anarchy.minesky.core.stats;

import anarchy.minesky.core.impl.combat.CombatStatsKeys;
import anarchy.minesky.core.impl.combat.CombatStatsRepository;
import anarchy.minesky.core.impl.stats.PlayerStatsRecord;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombatStatsRepositoryTest {

    @Test
    void readsKillsAndDeathsFromPlayerDataJson() {
        String json = "{\"stats\":{\"kills\":3100,\"deaths\":684}}";
        var map = anarchy.minesky.core.impl.playerdata.PlayerDataPaths.parseMap(json);
        assertEquals(3100, CombatStatsRepository.parseFromDataJson(json, CombatStatsKeys.KILLS));
        assertEquals(684, CombatStatsRepository.parseFromDataJson(json, CombatStatsKeys.DEATHS));
        assertEquals(3100, anarchy.minesky.core.impl.playerdata.PlayerDataPaths.readDotted(map, CombatStatsKeys.KILLS)
                instanceof Number n ? n.intValue() : 0);
    }

    @Test
    void playerStatsRecordWithCombatReplacesPvpColumns() {
        UUID uuid = UUID.randomUUID();
        PlayerStatsRecord base = new PlayerStatsRecord(uuid, "test", 99, 88, 3600, 10, 20, 5);
        PlayerStatsRecord merged = base.withCombat(3100, 684);
        assertEquals(3100, merged.kills());
        assertEquals(684, merged.deaths());
        assertEquals(3600, merged.playtimeSeconds());
    }
}