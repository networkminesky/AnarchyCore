package anarchy.minesky.core.impl.profile;

import anarchy.minesky.core.impl.stats.PlayerStatsRecord;
import anarchy.minesky.core.impl.stats.PlayerStatsSeed;

import java.util.UUID;

public record PlayerProfileSnapshot(
        UUID uuid,
        String displayName,
        int kills,
        int deaths,
        long playtimeSeconds,
        int blocksPlaced,
        int blocksBroken,
        int mobsKilled,
        double money,
        double shards) {

    public static PlayerProfileSnapshot fromStats(
            UUID uuid,
            String displayName,
            PlayerStatsRecord stats,
            double money,
            double shards) {
        if (stats != null) {
            return new PlayerProfileSnapshot(
                    stats.uuid(),
                    stats.playerName(),
                    stats.kills(),
                    stats.deaths(),
                    stats.playtimeSeconds(),
                    stats.blocksPlaced(),
                    stats.blocksBroken(),
                    stats.mobsKilled(),
                    money,
                    shards);
        }
        String name = displayName != null ? displayName : uuid.toString();
        return new PlayerProfileSnapshot(uuid, name, 0, 0, 0, 0, 0, 0, money, shards);
    }

    public static PlayerProfileSnapshot fromOnline(
            org.bukkit.entity.Player player, double money, double shards) {
        PlayerStatsRecord stats = PlayerStatsSeed.fromVanilla(player);
        return fromStats(player.getUniqueId(), player.getName(), stats, money, shards);
    }

    public boolean hasAnyStats() {
        return kills > 0
                || deaths > 0
                || playtimeSeconds > 0
                || blocksPlaced > 0
                || blocksBroken > 0
                || mobsKilled > 0;
    }
}