package anarchy.minesky.core.impl.stats;

import anarchy.minesky.core.impl.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PlayerStatsRepository {

    public enum SortColumn {
        KILLS("kills", "kills"),
        DEATHS("deaths", "deaths"),
        PLAYTIME("playtime_seconds", "playtime_seconds"),
        BLOCKS_PLACED("blocks_placed", "blocks_placed"),
        BLOCKS_BROKEN("blocks_broken", "blocks_broken"),
        MOBS_KILLED("mobs_killed", "mobs_killed");

        private final String sqlName;
        private final String selectExpr;

        SortColumn(String sqlName, String selectExpr) {
            this.sqlName = sqlName;
            this.selectExpr = selectExpr;
        }

        String sqlName() {
            return sqlName;
        }

        public long readValue(PlayerStatsRecord record) {
            return switch (this) {
                case KILLS -> record.kills();
                case DEATHS -> record.deaths();
                case PLAYTIME -> record.playtimeSeconds();
                case BLOCKS_PLACED -> record.blocksPlaced();
                case BLOCKS_BROKEN -> record.blocksBroken();
                case MOBS_KILLED -> record.mobsKilled();
            };
        }
    }

    private final DatabaseManager database;

    public PlayerStatsRepository(DatabaseManager database) {
        this.database = database;
    }

    public boolean isActive() {
        return database.isActive();
    }

    public Optional<PlayerStatsRecord> find(UUID uuid) {
        if (!isActive()) {
            return Optional.empty();
        }
        Optional<PlayerStatsRecord> loaded = database.querySync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT player_name, kills, deaths, playtime_seconds, blocks_placed, blocks_broken, mobs_killed
                    FROM ac_player_stats WHERE uuid = ?
                    """)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(mapRow(uuid, rs));
                }
            }
        });
        return loaded != null ? loaded : Optional.empty();
    }

    public void save(PlayerStatsRecord record) {
        if (!isActive() || record == null) {
            return;
        }
        database.runAsync(() -> database.withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO ac_player_stats (
                        uuid, player_name, kills, deaths, playtime_seconds,
                        blocks_placed, blocks_broken, mobs_killed, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    ON DUPLICATE KEY UPDATE
                        player_name = VALUES(player_name),
                        kills = VALUES(kills),
                        deaths = VALUES(deaths),
                        playtime_seconds = VALUES(playtime_seconds),
                        blocks_placed = VALUES(blocks_placed),
                        blocks_broken = VALUES(blocks_broken),
                        mobs_killed = VALUES(mobs_killed),
                        updated_at = CURRENT_TIMESTAMP
                    """)) {
                bind(ps, record);
                ps.executeUpdate();
            } catch (SQLException e) {
                database.plugin().getLogger().severe("[Stats] Falha ao salvar: " + e.getMessage());
            }
        }));
    }

    public List<PlayerStatsRecord> top(SortColumn column, int limit) {
        if (!isActive()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(50, limit));
        return database.querySync(connection -> {
            String sql = "SELECT uuid, player_name, kills, deaths, playtime_seconds, blocks_placed, blocks_broken, mobs_killed"
                    + " FROM ac_player_stats ORDER BY " + column.sqlName + " DESC, player_name ASC LIMIT ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, safeLimit);
                try (ResultSet rs = ps.executeQuery()) {
                    List<PlayerStatsRecord> rows = new ArrayList<>();
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        rows.add(mapRow(uuid, rs));
                    }
                    return rows;
                }
            }
        });
    }

    public int rank(SortColumn column, long value) {
        if (!isActive()) {
            return -1;
        }
        Integer rank = database.querySync(connection -> {
            String sql = "SELECT 1 + COUNT(*) AS r FROM ac_player_stats WHERE " + column.sqlName + " > ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, value);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("r");
                    }
                }
            }
            return -1;
        });
        return rank != null ? rank : -1;
    }

    private static void bind(PreparedStatement ps, PlayerStatsRecord record) throws SQLException {
        ps.setString(1, record.uuid().toString());
        ps.setString(2, record.playerName());
        ps.setInt(3, record.kills());
        ps.setInt(4, record.deaths());
        ps.setLong(5, record.playtimeSeconds());
        ps.setInt(6, record.blocksPlaced());
        ps.setInt(7, record.blocksBroken());
        ps.setInt(8, record.mobsKilled());
    }

    private static PlayerStatsRecord mapRow(UUID uuid, ResultSet rs) throws SQLException {
        return new PlayerStatsRecord(
                uuid,
                rs.getString("player_name"),
                rs.getInt("kills"),
                rs.getInt("deaths"),
                rs.getLong("playtime_seconds"),
                rs.getInt("blocks_placed"),
                rs.getInt("blocks_broken"),
                rs.getInt("mobs_killed"));
    }
}