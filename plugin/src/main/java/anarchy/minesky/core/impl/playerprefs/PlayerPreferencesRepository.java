package anarchy.minesky.core.impl.playerprefs;

import anarchy.minesky.core.impl.database.DatabaseManager;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerPreferencesRepository {

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
                            + " allow_tell_strangers, tell_sounds, scoreboard_enabled"
                            + " FROM ac_player_prefs WHERE uuid = ?")) {
                ps.setString(1, playerId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    PlayerPreferences loaded = PlayerPreferences.defaults();
                    loaded.setAllowTpaFromStrangers(rs.getBoolean("allow_tpa_strangers"));
                    loaded.setAllowTpaHereFromStrangers(rs.getBoolean("allow_tpahere_strangers"));
                    loaded.setAllowPayFromStrangers(rs.getBoolean("allow_pay_strangers"));
                    loaded.setPaymentSounds(rs.getBoolean("payment_sounds"));
                    loaded.setAllowTellFromStrangers(rs.getBoolean("allow_tell_strangers"));
                    loaded.setTellNotificationSounds(rs.getBoolean("tell_sounds"));
                    loaded.setScoreboardEnabled(rs.getBoolean("scoreboard_enabled"));
                    return loaded;
                }
            }
        });
        return prefs != null ? prefs : PlayerPreferences.defaults();
    }

    public void save(UUID playerId, PlayerPreferences prefs) {
        if (!database.isActive()) {
            return;
        }
        database.runAsync(() -> database.withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO ac_player_prefs (
                      uuid, allow_tpa_strangers, allow_tpahere_strangers, allow_pay_strangers, payment_sounds,
                      allow_tell_strangers, tell_sounds, scoreboard_enabled
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                      allow_tpa_strangers = VALUES(allow_tpa_strangers),
                      allow_tpahere_strangers = VALUES(allow_tpahere_strangers),
                      allow_pay_strangers = VALUES(allow_pay_strangers),
                      payment_sounds = VALUES(payment_sounds),
                      allow_tell_strangers = VALUES(allow_tell_strangers),
                      tell_sounds = VALUES(tell_sounds),
                      scoreboard_enabled = VALUES(scoreboard_enabled)
                    """)) {
                ps.setString(1, playerId.toString());
                ps.setBoolean(2, prefs.allowTpaFromStrangers());
                ps.setBoolean(3, prefs.allowTpaHereFromStrangers());
                ps.setBoolean(4, prefs.allowPayFromStrangers());
                ps.setBoolean(5, prefs.paymentSounds());
                ps.setBoolean(6, prefs.allowTellFromStrangers());
                ps.setBoolean(7, prefs.tellNotificationSounds());
                ps.setBoolean(8, prefs.scoreboardEnabled());
                ps.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().severe("Falha ao salvar prefs de " + playerId + ": " + e.getMessage());
            }
        }));
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
