package anarchy.minesky.core.impl.stats;

import anarchy.minesky.core.impl.database.DatabaseManager;
import anarchy.minesky.core.impl.leaderboards.LeaderboardCategory;
import anarchy.minesky.core.impl.leaderboards.LeaderboardService.LeaderboardLine;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PlayerStatsWeeklyRepository {

    public enum ScoreColumn {
        MONEY("score_money", "baseline_money"),
        SHARDS("score_shards", "baseline_shards"),
        KILLS("score_kills", "baseline_kills"),
        DEATHS("score_deaths", "baseline_deaths"),
        PLAYTIME("score_playtime_seconds", "baseline_playtime_seconds"),
        BLOCKS_PLACED("score_blocks_placed", "baseline_blocks_placed"),
        BLOCKS_BROKEN("score_blocks_broken", "baseline_blocks_broken"),
        MOBS_KILLED("score_mobs_killed", "baseline_mobs_killed");

        private final String scoreSql;
        private final String baselineSql;

        ScoreColumn(String scoreSql, String baselineSql) {
            this.scoreSql = scoreSql;
            this.baselineSql = baselineSql;
        }

        String scoreSql() {
            return scoreSql;
        }

        public static ScoreColumn forCategory(LeaderboardCategory category) {
            return switch (category) {
                case MONEY -> MONEY;
                case SHARDS -> SHARDS;
                case KILLS -> KILLS;
                case DEATHS -> DEATHS;
                case PLAYTIME -> PLAYTIME;
                case BLOCKS_PLACED -> BLOCKS_PLACED;
                case BLOCKS_BROKEN -> BLOCKS_BROKEN;
                case MOBS_KILLED -> MOBS_KILLED;
            };
        }
    }

    private final DatabaseManager database;

    public PlayerStatsWeeklyRepository(DatabaseManager database) {
        this.database = database;
    }

    public boolean isActive() {
        return database.isActive();
    }

    public List<LeaderboardLine> top(String weekKey, ScoreColumn column, int limit) {
        if (!isActive() || weekKey == null || column == null || limit <= 0) {
            return List.of();
        }
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        List<LeaderboardLine> rows = database.querySync(connection -> {
            String sql = "SELECT uuid, player_name, " + column.scoreSql + " AS stat_value"
                    + " FROM ac_player_stats_weekly"
                    + " WHERE week_key = ? AND " + column.scoreSql + " > 0"
                    + " ORDER BY stat_value DESC, player_name ASC"
                    + " LIMIT ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, weekKey);
                ps.setInt(2, safeLimit);
                try (ResultSet rs = ps.executeQuery()) {
                    List<LeaderboardLine> out = new ArrayList<>();
                    int position = 1;
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        String name = rs.getString("player_name");
                        double value = rs.getDouble("stat_value");
                        if (value <= 0) {
                            continue;
                        }
                        out.add(new LeaderboardLine(position++, uuid, name, value));
                    }
                    return out;
                }
            }
        });
        return rows != null ? rows : List.of();
    }

    public int rank(String weekKey, ScoreColumn column, UUID uuid, double value) {
        if (!isActive() || value <= 0 || weekKey == null || uuid == null) {
            return -1;
        }
        Integer rank = database.querySync(connection -> {
            String sql = "SELECT 1 + COUNT(*) AS r FROM ac_player_stats_weekly"
                    + " WHERE week_key = ? AND " + column.scoreSql + " > ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, weekKey);
                ps.setDouble(2, value);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt("r") : -1;
                }
            }
        });
        return rank != null ? rank : -1;
    }

    public Optional<WeeklyRow> find(UUID uuid, String weekKey) {
        if (!isActive() || uuid == null || weekKey == null) {
            return Optional.empty();
        }
        Optional<WeeklyRow> loaded = database.querySync(connection -> {
            String sql = """
                    SELECT player_name, baseline_money, baseline_shards, baseline_kills, baseline_deaths,
                           baseline_playtime_seconds, baseline_blocks_placed, baseline_blocks_broken, baseline_mobs_killed,
                           score_money, score_shards, score_kills, score_deaths,
                           score_playtime_seconds, score_blocks_placed, score_blocks_broken, score_mobs_killed
                    FROM ac_player_stats_weekly WHERE uuid = ? AND week_key = ?
                    """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, weekKey);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(mapRow(uuid, weekKey, rs));
                }
            }
        });
        return loaded != null ? loaded : Optional.empty();
    }

    public double scoreFor(WeeklyRow row, ScoreColumn column) {
        if (row == null) {
            return 0;
        }
        return switch (column) {
            case MONEY -> row.scoreMoney();
            case SHARDS -> row.scoreShards();
            case KILLS -> row.scoreKills();
            case DEATHS -> row.scoreDeaths();
            case PLAYTIME -> row.scorePlaytimeSeconds();
            case BLOCKS_PLACED -> row.scoreBlocksPlaced();
            case BLOCKS_BROKEN -> row.scoreBlocksBroken();
            case MOBS_KILLED -> row.scoreMobsKilled();
        };
    }

    public void upsert(WeeklyRow row) {
        if (!isActive() || row == null) {
            return;
        }
        database.runAsync(() -> saveSync(row));
    }

    public void saveSync(WeeklyRow row) {
        if (!isActive() || row == null) {
            return;
        }
        database.withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO ac_player_stats_weekly (
                        uuid, week_key, player_name,
                        baseline_money, baseline_shards, baseline_kills, baseline_deaths,
                        baseline_playtime_seconds, baseline_blocks_placed, baseline_blocks_broken, baseline_mobs_killed,
                        score_money, score_shards, score_kills, score_deaths,
                        score_playtime_seconds, score_blocks_placed, score_blocks_broken, score_mobs_killed,
                        updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    ON DUPLICATE KEY UPDATE
                        player_name = VALUES(player_name),
                        baseline_money = VALUES(baseline_money),
                        baseline_shards = VALUES(baseline_shards),
                        baseline_kills = VALUES(baseline_kills),
                        baseline_deaths = VALUES(baseline_deaths),
                        baseline_playtime_seconds = VALUES(baseline_playtime_seconds),
                        baseline_blocks_placed = VALUES(baseline_blocks_placed),
                        baseline_blocks_broken = VALUES(baseline_blocks_broken),
                        baseline_mobs_killed = VALUES(baseline_mobs_killed),
                        score_money = VALUES(score_money),
                        score_shards = VALUES(score_shards),
                        score_kills = VALUES(score_kills),
                        score_deaths = VALUES(score_deaths),
                        score_playtime_seconds = VALUES(score_playtime_seconds),
                        score_blocks_placed = VALUES(score_blocks_placed),
                        score_blocks_broken = VALUES(score_blocks_broken),
                        score_mobs_killed = VALUES(score_mobs_killed),
                        updated_at = CURRENT_TIMESTAMP
                    """)) {
                bind(ps, row);
                ps.executeUpdate();
            } catch (SQLException e) {
                database.plugin().getLogger().severe("[Weekly] Falha ao salvar: " + e.getMessage());
            }
        });
    }

    private static void bind(PreparedStatement ps, WeeklyRow row) throws SQLException {
        ps.setString(1, row.uuid().toString());
        ps.setString(2, row.weekKey());
        ps.setString(3, row.playerName());
        ps.setDouble(4, row.baselineMoney());
        ps.setDouble(5, row.baselineShards());
        ps.setInt(6, row.baselineKills());
        ps.setInt(7, row.baselineDeaths());
        ps.setLong(8, row.baselinePlaytimeSeconds());
        ps.setInt(9, row.baselineBlocksPlaced());
        ps.setInt(10, row.baselineBlocksBroken());
        ps.setInt(11, row.baselineMobsKilled());
        ps.setDouble(12, row.scoreMoney());
        ps.setDouble(13, row.scoreShards());
        ps.setInt(14, row.scoreKills());
        ps.setInt(15, row.scoreDeaths());
        ps.setLong(16, row.scorePlaytimeSeconds());
        ps.setInt(17, row.scoreBlocksPlaced());
        ps.setInt(18, row.scoreBlocksBroken());
        ps.setInt(19, row.scoreMobsKilled());
    }

    private static WeeklyRow mapRow(UUID uuid, String weekKey, ResultSet rs) throws SQLException {
        return new WeeklyRow(
                uuid,
                weekKey,
                rs.getString("player_name"),
                rs.getDouble("baseline_money"),
                rs.getDouble("baseline_shards"),
                rs.getInt("baseline_kills"),
                rs.getInt("baseline_deaths"),
                rs.getLong("baseline_playtime_seconds"),
                rs.getInt("baseline_blocks_placed"),
                rs.getInt("baseline_blocks_broken"),
                rs.getInt("baseline_mobs_killed"),
                rs.getDouble("score_money"),
                rs.getDouble("score_shards"),
                rs.getInt("score_kills"),
                rs.getInt("score_deaths"),
                rs.getLong("score_playtime_seconds"),
                rs.getInt("score_blocks_placed"),
                rs.getInt("score_blocks_broken"),
                rs.getInt("score_mobs_killed"));
    }

    public record WeeklyRow(
            UUID uuid,
            String weekKey,
            String playerName,
            double baselineMoney,
            double baselineShards,
            int baselineKills,
            int baselineDeaths,
            long baselinePlaytimeSeconds,
            int baselineBlocksPlaced,
            int baselineBlocksBroken,
            int baselineMobsKilled,
            double scoreMoney,
            double scoreShards,
            int scoreKills,
            int scoreDeaths,
            long scorePlaytimeSeconds,
            int scoreBlocksPlaced,
            int scoreBlocksBroken,
            int scoreMobsKilled) {}
}