package anarchy.minesky.core.impl.guard;

import anarchy.minesky.core.impl.database.DatabaseManager;
import com.google.gson.Gson;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class GuardRepository {

    private static final Gson GSON = new Gson();

    private final Plugin plugin;
    private final DatabaseManager database;
    private final GuardSettings settings;

    public GuardRepository(Plugin plugin, DatabaseManager database, GuardSettings settings) {
        this.plugin = plugin;
        this.database = database;
        this.settings = settings;
    }

    public boolean isActive() {
        return database.isActive();
    }

    public void appendHistoryAsync(UUID uuid, GuardHistoryEntry entry, DatabaseManager.AsyncCallback<Void> callback) {
        if (!isActive()) {
            plugin.getLogger().warning("Guard: histórico não salvo — database inativa.");
            callback.complete(null, null);
            return;
        }
        database.queryAsync(connection -> {
            insertHistory(connection, uuid, entry);
            trimHistory(connection, uuid, settings.maxHistoryEntries());
            return null;
        }, callback);
    }

    public List<GuardHistoryEntry> getHistory(UUID uuid) {
        if (!isActive()) {
            return List.of();
        }
        List<GuardHistoryEntry> result = database.querySync(connection -> selectHistory(connection, uuid));
        return result != null ? result : List.of();
    }

    public int getStrikes(UUID uuid) {
        if (!isActive()) {
            return 0;
        }
        Integer strikes = database.querySync(connection -> selectStrikes(connection, uuid));
        return strikes != null ? strikes : 0;
    }

    public int incrementStrikes(UUID uuid) {
        if (!isActive()) {
            plugin.getLogger().warning("Guard: strike não persistido — database inativa.");
            return 1;
        }
        Integer strikes = database.querySync(connection -> upsertIncrementStrikes(connection, uuid));
        return strikes != null ? strikes : 1;
    }

    private static void insertHistory(Connection connection, UUID uuid, GuardHistoryEntry entry) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                        INSERT INTO ac_guard_history
                          (player_uuid, recorded_at, trigger_source, passed, detected_mods, client_brand, ping, detail)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, entry.timestampMillis());
            ps.setString(3, entry.trigger());
            ps.setBoolean(4, entry.passed());
            ps.setString(5, GSON.toJson(entry.detectedMods()));
            ps.setString(6, entry.clientBrand());
            ps.setInt(7, entry.ping());
            ps.setString(8, entry.detail());
            ps.executeUpdate();
        }
    }

    private static void trimHistory(Connection connection, UUID uuid, int maxEntries) throws SQLException {
        if (maxEntries <= 0) {
            return;
        }
        try (PreparedStatement ps = connection.prepareStatement(
                """
                        DELETE FROM ac_guard_history
                        WHERE player_uuid = ?
                          AND id NOT IN (
                            SELECT id FROM (
                              SELECT id FROM ac_guard_history
                              WHERE player_uuid = ?
                              ORDER BY recorded_at DESC, id DESC
                              LIMIT ?
                            ) recent
                          )
                        """)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, uuid.toString());
            ps.setInt(3, maxEntries);
            ps.executeUpdate();
        }
    }

    private static List<GuardHistoryEntry> selectHistory(Connection connection, UUID uuid) throws SQLException {
        List<GuardHistoryEntry> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                """
                        SELECT recorded_at, trigger_source, passed, detected_mods, client_brand, ping, detail
                        FROM ac_guard_history
                        WHERE player_uuid = ?
                        ORDER BY recorded_at ASC, id ASC
                        """)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapHistoryRow(rs));
                }
            }
        }
        return list;
    }

    private static GuardHistoryEntry mapHistoryRow(ResultSet rs) throws SQLException {
        String modsJson = rs.getString("detected_mods");
        List<String> mods = List.of();
        if (modsJson != null && !modsJson.isBlank()) {
            String[] parsed = GSON.fromJson(modsJson, String[].class);
            if (parsed != null && parsed.length > 0) {
                mods = List.copyOf(Arrays.asList(parsed));
            }
        }
        return new GuardHistoryEntry(
                rs.getLong("recorded_at"),
                rs.getString("trigger_source"),
                rs.getBoolean("passed"),
                Collections.unmodifiableList(new ArrayList<>(mods)),
                rs.getString("client_brand"),
                rs.getInt("ping"),
                rs.getString("detail"));
    }

    private static int selectStrikes(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT strikes FROM ac_guard_strikes WHERE player_uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return 0;
                }
                return rs.getInt("strikes");
            }
        }
    }

    private static int upsertIncrementStrikes(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                        INSERT INTO ac_guard_strikes (player_uuid, strikes) VALUES (?, 1)
                        ON DUPLICATE KEY UPDATE strikes = strikes + 1
                        """)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
        return selectStrikes(connection, uuid);
    }
}
