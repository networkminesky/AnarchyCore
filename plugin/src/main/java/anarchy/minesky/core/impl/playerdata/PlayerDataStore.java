package anarchy.minesky.core.impl.playerdata;

import anarchy.minesky.core.impl.database.DatabaseManager;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class PlayerDataStore {

    record Row(UUID uuid, String latestNickname, Map<String, Object> data) {
    }

    private final Plugin plugin;
    private final DatabaseManager database;

    PlayerDataStore(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
    }

    boolean isActive() {
        return database.isActive();
    }

    void touch(UUID uuid, String nickname, DatabaseManager.AsyncCallback<Void> callback) {
        if (!isActive()) {
            callback.complete(null, new SQLException("inactive"));
            return;
        }
        String nick = normalizeNick(nickname);
        database.queryAsync(connection -> {
            upsertTouch(connection, uuid, nick);
            return null;
        }, callback);
    }

    void findByUuid(UUID uuid, DatabaseManager.AsyncCallback<Row> callback) {
        if (!isActive()) {
            callback.complete(null, new SQLException("inactive"));
            return;
        }
        database.queryAsync(connection -> selectByUuid(connection, uuid), callback);
    }

    void findByNickname(String nickname, DatabaseManager.AsyncCallback<Row> callback) {
        if (!isActive()) {
            callback.complete(null, new SQLException("inactive"));
            return;
        }
        String nick = normalizeNick(nickname);
        database.queryAsync(connection -> selectByNickname(connection, nick), callback);
    }

    void saveMerged(UUID uuid, Map<String, Object> patch, DatabaseManager.AsyncCallback<Boolean> callback) {
        if (!isActive()) {
            callback.complete(null, new SQLException("inactive"));
            return;
        }
        database.queryAsync(connection -> mergeAndSave(connection, uuid, patch), callback);
    }

    static UUID resolveUuid(String nameOrUuid) {
        String param = nameOrUuid.trim();
        if (param.length() == 36) {
            try {
                return UUID.fromString(param);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    static boolean isValidNick(String nickname) {
        if (nickname == null) {
            return false;
        }
        String nick = nickname.trim();
        return nick.length() >= 3 && nick.length() <= 16;
    }

    private static String normalizeNick(String nickname) {
        return nickname == null ? null : nickname.trim().toLowerCase();
    }

    private Row selectByUuid(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid, latest_nickname, data FROM ac_player_data WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rowFromResultSet(rs);
            }
        }
    }

    private Row selectByNickname(Connection connection, String nickname) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid, latest_nickname, data FROM ac_player_data WHERE latest_nickname = ? LIMIT 1")) {
            ps.setString(1, nickname);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rowFromResultSet(rs);
            }
        }
    }

    private void upsertTouch(Connection connection, UUID uuid, String nickname) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO ac_player_data (uuid, latest_nickname, data)
                VALUES (?, ?, JSON_OBJECT())
                ON DUPLICATE KEY UPDATE latest_nickname = VALUES(latest_nickname)
                """)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, nickname);
            ps.executeUpdate();
        }
    }

    private boolean mergeAndSave(Connection connection, UUID uuid, Map<String, Object> patch) throws SQLException {
        Row existing = selectByUuid(connection, uuid);
        Map<String, Object> merged = existing == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(existing.data());
        for (Map.Entry<String, Object> entry : patch.entrySet()) {
            PlayerDataJson.putNested(merged, entry.getKey(), entry.getValue());
        }
        String json = PlayerDataJson.toJson(merged);
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO ac_player_data (uuid, latest_nickname, data)
                VALUES (?, NULL, CAST(? AS JSON))
                ON DUPLICATE KEY UPDATE data = CAST(? AS JSON)
                """)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, json);
            ps.setString(3, json);
            ps.executeUpdate();
        }
        return existing != null;
    }

    private static Row rowFromResultSet(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("uuid"));
        String nick = rs.getString("latest_nickname");
        String json = rs.getString("data");
        return new Row(id, nick, PlayerDataJson.parseMap(json));
    }
}
