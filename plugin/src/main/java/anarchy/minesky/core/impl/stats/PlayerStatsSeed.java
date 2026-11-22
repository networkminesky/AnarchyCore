package anarchy.minesky.core.impl.stats;

import org.bukkit.Statistic;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public final class PlayerStatsSeed {

    private PlayerStatsSeed() {}

    public static PlayerStatsRecord fromVanilla(Player player) {
        return fromVanillaProgression(player);
    }

    /** Estatísticas vanilla exceto abates/mortes (fonte: {@code ac_player_data}). */
    public static PlayerStatsRecord fromVanillaProgression(Player player) {
        long playtimeSeconds = ticksToSeconds(safeStatistic(player, Statistic.PLAY_ONE_MINUTE));
        int blocksBroken = totalStatistic(player, Statistic.MINE_BLOCK, PlayerStatsMaterialIndex.mineBlockMaterials());
        int blocksPlaced = totalStatistic(player, Statistic.USE_ITEM, PlayerStatsMaterialIndex.useBlockMaterials());
        int mobsKilled = totalMobKills(player);
        String name = player.getName() != null ? player.getName() : player.getUniqueId().toString();
        return new PlayerStatsRecord(
                player.getUniqueId(),
                name,
                0,
                0,
                playtimeSeconds,
                blocksPlaced,
                blocksBroken,
                mobsKilled);
    }

    public static long ticksToSeconds(int ticks) {
        return Math.max(0L, ticks / 20L);
    }

    private static int safeStatistic(Player player, Statistic statistic) {
        try {
            return player.getStatistic(statistic);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static int totalStatistic(Player player, Statistic statistic, Iterable<Material> materials) {
        int total = 0;
        for (Material material : materials) {
            if (material.isLegacy()) {
                continue;
            }
            try {
                total += player.getStatistic(statistic, material);
            } catch (Throwable ignored) {
                // material não rastreado nesta versão
            }
        }
        return total;
    }

    private static int totalMobKills(Player player) {
        try {
            return player.getStatistic(Statistic.MOB_KILLS);
        } catch (Throwable ignored) {
            // versões sem MOB_KILLS agregado
        }
        int total = 0;
        for (EntityType type : EntityType.values()) {
            if (!type.isAlive() || !type.isSpawnable()) {
                continue;
            }
            try {
                total += player.getStatistic(Statistic.KILL_ENTITY, type);
            } catch (Throwable ignored) {
            }
        }
        return total;
    }
}