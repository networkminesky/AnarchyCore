package anarchy.minesky.core.impl.combat;

import anarchy.minesky.core.impl.database.DatabaseManager;
import anarchy.minesky.core.impl.leaderboards.LeaderboardService.LeaderboardLine;
import anarchy.minesky.core.impl.playerdata.PlayerDataPaths;
import com.google.gson.Gson;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Abates e mortes oficiais em {@code ac_player_data.data} ({@link CombatStatsKeys}).
 * Mesma fonte dos placeholders {@code %anarchyc_kills%} / {@code %anarchyc_deaths%}.
 */
public final class CombatStatsRepository {

    private final DatabaseManager database;

    public CombatStatsRepository(DatabaseManager database) {
        this.database = database;
    }

    public boolean isActive() {
        return database.isActive();
    }

    public int kills(UUID uuid) {
        return readInt(uuid, CombatStatsKeys.KILLS).orElse(0);
    }

    public int deaths(UUID uuid) {
        return readInt(uuid, CombatStatsKeys.DEATHS).orElse(0);
    }

    public OptionalInt readInt(UUID uuid, String key) {
        if (!isActive() || uuid == null || key == null) {
            return OptionalInt.empty();
        }
        return database.querySync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT data, latest_nickname FROM ac_player_data WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return OptionalInt.empty();
                    }
                    Map<String, Object> map = PlayerDataPaths.parseMap(rs.getString("data"));
                    Object raw = PlayerDataPaths.readDotted(map, key);
                    return toOptionalInt(raw);
                }
            }
        });
    }

    public List<LeaderboardLine> topKills(int limit) {
        return top(CombatStatsKeys.KILLS, limit);
    }

    public List<LeaderboardLine> topDeaths(int limit) {
        return top(CombatStatsKeys.DEATHS, limit);
    }

    public int rank(String key, int value) {
        if (!isActive() || value <= 0) {
            return -1;
        }
        Integer rank = database.querySync(connection -> {
            String sql = """
                    SELECT 1 + COUNT(*) AS r FROM ac_player_data
                    WHERE CAST(COALESCE(%s, '0') AS UNSIGNED) > ?
                    """.formatted(CombatStatsJsonSql.statValueExpression(key));
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, CombatStatsJsonSql.nestedJsonPath(key));
                ps.setString(2, CombatStatsJsonSql.flatJsonPath(key));
                ps.setInt(3, value);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt("r") : -1;
                }
            }
        });
        return rank != null ? rank : -1;
    }

    private List<LeaderboardLine> top(String key, int limit) {
        if (!isActive() || limit <= 0) {
            return List.of();
        }
        List<LeaderboardLine> rows = database.querySync(connection -> {
            String jsonExpr = CombatStatsJsonSql.statValueExpression(key);
            String sql = """
                    SELECT uuid, COALESCE(latest_nickname, '-') AS player_name,
                           CAST(COALESCE(%s, '0') AS UNSIGNED) AS stat_value
                    FROM ac_player_data
                    WHERE CAST(COALESCE(%s, '0') AS UNSIGNED) > 0
                    ORDER BY stat_value DESC, player_name ASC
                    LIMIT ?
                    """.formatted(jsonExpr, jsonExpr);
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, CombatStatsJsonSql.nestedJsonPath(key));
                ps.setString(2, CombatStatsJsonSql.flatJsonPath(key));
                ps.setString(3, CombatStatsJsonSql.nestedJsonPath(key));
                ps.setString(4, CombatStatsJsonSql.flatJsonPath(key));
                ps.setInt(5, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    List<LeaderboardLine> out = new ArrayList<>();
                    int position = 1;
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        String name = rs.getString("player_name");
                        double value = rs.getLong("stat_value");
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

    private static OptionalInt toOptionalInt(Object raw) {
        if (raw == null) {
            return OptionalInt.empty();
        }
        if (raw instanceof Number number) {
            return OptionalInt.of(Math.max(0, number.intValue()));
        }
        try {
            return OptionalInt.of(Math.max(0, Integer.parseInt(raw.toString())));
        } catch (NumberFormatException ignored) {
            return OptionalInt.empty();
        }
    }

    /** Para testes e diagnóstico de formato JSON em {@code ac_player_data}. */
    public static int parseFromDataJson(String json, String key) {
        Map<String, Object> map = PlayerDataPaths.parseMap(json);
        return toOptionalInt(PlayerDataPaths.readDotted(map, key)).orElse(0);
    }

    public static String sampleDataJson(int kills, int deaths) {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("kills", kills);
        stats.put("deaths", deaths);
        root.put("stats", stats);
        return new Gson().toJson(root);
    }
}