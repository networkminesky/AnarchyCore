package anarchy.minesky.core.impl.stats;

import java.util.UUID;

public record PlayerStatsRecord(
        UUID uuid,
        String playerName,
        int kills,
        int deaths,
        long playtimeSeconds,
        int blocksPlaced,
        int blocksBroken,
        int mobsKilled) {

    public PlayerStatsRecord withCombat(int kills, int deaths) {
        return new PlayerStatsRecord(
                uuid, playerName, kills, deaths, playtimeSeconds, blocksPlaced, blocksBroken, mobsKilled);
    }

    public static boolean sameValues(PlayerStatsRecord a, PlayerStatsRecord b) {
        if (a == null || b == null) {
            return false;
        }
        return a.kills == b.kills
                && a.deaths == b.deaths
                && a.playtimeSeconds == b.playtimeSeconds
                && a.blocksPlaced == b.blocksPlaced
                && a.blocksBroken == b.blocksBroken
                && a.mobsKilled == b.mobsKilled
                && a.playerName.equals(b.playerName);
    }
}