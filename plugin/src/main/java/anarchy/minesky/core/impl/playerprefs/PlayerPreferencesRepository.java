package anarchy.minesky.core.impl.playerprefs;

import anarchy.minesky.core.api.playerconfig.PlayerConfigKeys;
import anarchy.minesky.core.impl.database.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerPreferencesRepository {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Boolean>>() {}.getType();

    private final Plugin plugin;
    private final DatabaseManager database;

    public PlayerPreferencesRepository(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
    }

    public PlayerPreferences load(UUID playerId) {
        if (!database.isActive()) {
            return PlayerPreferences.defaults();
        }
        PlayerPreferences prefs = database.querySync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT allow_tpa_strangers, allow_tpahere_strangers, allow_pay_strangers, payment_sounds,"
                            + " allow_tell_strangers, tell_sounds, scoreboard_enabled, tpa_auto, config_options"
                            + " FROM ac_player_prefs WHERE uuid = ?")) {
                ps.setString(1, playerId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    PlayerPreferences loaded = PlayerPreferences.defaults();
                    loaded.setTpaAuto(rs.getBoolean("tpa_auto"));

                    Map<String, Boolean> fromJson = parseConfigOptions(rs.getString("config_options"));
                    mergeLegacyColumns(loaded, fromJson, rs);
                    loaded.configBooleans().clear();
                    loaded.configBooleans().putAll(fromJson);
                    return loaded;
                }
            }
        });
        return prefs != null ? prefs : PlayerPreferences.defaults();
    }

    public void saveSync(UUID playerId, PlayerPreferences prefs) {
        if (!database.isActive()) {
            return;
        }
        database.querySync(connection -> {
            persist(connection, playerId, prefs);
            return null;
        });
    }

    public void save(UUID playerId, PlayerPreferences prefs) {
        if (!database.isActive()) {
            return;
        }
        database.runAsync(() -> database.withConnection(connection -> persist(connection, playerId, prefs)));
    }

    private void mergeLegacyColumns(PlayerPreferences loaded, Map<String, Boolean> target, ResultSet rs)
            throws java.sql.SQLException {
        putIfAbsent(target, PlayerConfigKeys.TPA_FROM_STRANGERS, rs.getBoolean("allow_tpa_strangers"));
        putIfAbsent(target, PlayerConfigKeys.TPAHERE_FROM_STRANGERS, rs.getBoolean("allow_tpahere_strangers"));
        putIfAbsent(target, PlayerConfigKeys.PAY_FROM_STRANGERS, rs.getBoolean("allow_pay_strangers"));
        putIfAbsent(target, PlayerConfigKeys.PAYMENT_SOUNDS, rs.getBoolean("payment_sounds"));
        putIfAbsent(target, PlayerConfigKeys.TELL_FROM_STRANGERS, rs.getBoolean("allow_tell_strangers"));
        putIfAbsent(target, PlayerConfigKeys.TELL_NOTIFICATION_SOUNDS, rs.getBoolean("tell_sounds"));
        putIfAbsent(target, PlayerConfigKeys.SCOREBOARD_ENABLED, rs.getBoolean("scoreboard_enabled"));
    }

    private static void putIfAbsent(Map<String, Boolean> target, String key, boolean value) {
        target.putIfAbsent(key, value);
    }

    private static Map<String, Boolean> parseConfigOptions(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            Map<String, Boolean> parsed = GSON.fromJson(json, MAP_TYPE);
            return parsed != null ? new HashMap<>(parsed) : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private void persist(java.sql.Connection connection, UUID playerId, PlayerPreferences prefs) {
        String configJson = GSON.toJson(prefs.configBooleans());
        try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO ac_player_prefs (
                      uuid, allow_tpa_strangers, allow_tpahere_strangers, allow_pay_strangers, payment_sounds,
                      allow_tell_strangers, tell_sounds, scoreboard_enabled, tpa_auto, config_options
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                      allow_tpa_strangers = VALUES(allow_tpa_strangers),
                      allow_tpahere_strangers = VALUES(allow_tpahere_strangers),
                      allow_pay_strangers = VALUES(allow_pay_strangers),
                      payment_sounds = VALUES(payment_sounds),
                      allow_tell_strangers = VALUES(allow_tell_strangers),
                      tell_sounds = VALUES(tell_sounds),
                      scoreboard_enabled = VALUES(scoreboard_enabled),
                      tpa_auto = VALUES(tpa_auto),
                      config_options = VALUES(config_options)
                    """)) {
            ps.setString(1, playerId.toString());
            ps.setBoolean(2, prefs.allowTpaFromStrangers());
            ps.setBoolean(3, prefs.allowTpaHereFromStrangers());
            ps.setBoolean(4, prefs.allowPayFromStrangers());
            ps.setBoolean(5, prefs.paymentSounds());
            ps.setBoolean(6, prefs.allowTellFromStrangers());
            ps.setBoolean(7, prefs.tellNotificationSounds());
            ps.setBoolean(8, prefs.scoreboardEnabled());
            ps.setBoolean(9, prefs.tpaAuto());
            ps.setString(10, configJson);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("Falha ao salvar prefs de " + playerId + ": " + e.getMessage());
        }
    }

    public Set<UUID> loadKnown(UUID playerId) {
        Set<UUID> known = ConcurrentHashMap.newKeySet();
        if (!database.isActive()) {
            return known;
        }
        database.withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT known_uuid FROM ac_player_known WHERE player_uuid = ?")) {
                ps.setString(1, playerId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        known.add(UUID.fromString(rs.getString("known_uuid")));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Falha ao carregar conhecidos: " + e.getMessage());
            }
        });
        return known;
    }

    public void markKnown(UUID a, UUID b) {
        if (a.equals(b) || !database.isActive()) {
            return;
        }
        database.runAsync(() -> database.withConnection(connection -> insertKnown(connection, a, b)));
    }

    private void insertKnown(Connection connection, UUID a, UUID b) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT IGNORE INTO ac_player_known (player_uuid, known_uuid) VALUES (?, ?), (?, ?)")) {
            ps.setString(1, a.toString());
            ps.setString(2, b.toString());
            ps.setString(3, b.toString());
            ps.setString(4, a.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().severe("Falha ao salvar conhecido: " + e.getMessage());
        }
    }
}
