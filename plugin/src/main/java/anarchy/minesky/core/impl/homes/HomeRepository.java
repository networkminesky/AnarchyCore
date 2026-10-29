package anarchy.minesky.core.impl.homes;

import anarchy.minesky.core.impl.database.DatabaseManager;
import anarchy.minesky.core.impl.warps.WarpStorage;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HomeRepository {

    private final Plugin plugin;
    private final DatabaseManager database;
    private final HomeSettings settings;

    public HomeRepository(Plugin plugin, DatabaseManager database, HomeSettings settings) {
        this.plugin = plugin;
        this.database = database;
        this.settings = settings;
    }

    public boolean isActive() {
        return database.isActive();
    }

    public void listHomesAsync(UUID ownerUuid, DatabaseManager.AsyncCallback<List<HomeDefinition>> callback) {
        if (!isActive()) {
            callback.complete(List.of(), null);
            return;
        }
        database.queryAsync(connection -> selectHomes(connection, ownerUuid), callback);
    }

    public void getHomeAsync(UUID ownerUuid, String homeId, DatabaseManager.AsyncCallback<HomeDefinition> callback) {
        if (!isActive()) {
            callback.complete(null, null);
            return;
        }
        database.queryAsync(connection -> selectHome(connection, ownerUuid, homeId), callback);
    }

    public void resolveOwnerUuidAsync(String nickname, DatabaseManager.AsyncCallback<UUID> callback) {
        if (!isActive()) {
            callback.complete(null, null);
            return;
        }
        String nick = nickname.trim().toLowerCase();
        database.queryAsync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT uuid FROM ac_player_data WHERE latest_nickname = ? LIMIT 1")) {
                ps.setString(1, nick);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    return UUID.fromString(rs.getString("uuid"));
                }
            }
        }, callback);
    }

    public void saveHomeAsync(HomeDefinition home, DatabaseManager.AsyncCallback<Void> callback) {
        if (!isActive()) {
            callback.complete(null, new SQLException("inactive"));
            return;
        }
        database.queryAsync(connection -> {
            upsertHome(connection, home);
            return null;
        }, callback);
    }

    public void deleteHomeAsync(UUID ownerUuid, String homeId, DatabaseManager.AsyncCallback<Boolean> callback) {
        if (!isActive()) {
            callback.complete(false, new SQLException("inactive"));
            return;
        }
        database.queryAsync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM ac_player_homes WHERE owner_uuid = ? AND home_id = ?")) {
                ps.setString(1, ownerUuid.toString());
                ps.setString(2, homeId);
                return ps.executeUpdate() > 0;
            }
        }, callback);
    }

    private List<HomeDefinition> selectHomes(Connection connection, UUID ownerUuid) throws SQLException {
        List<HomeDefinition> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                """
                        SELECT home_id, display_name, icon_material, world, x, y, z, yaw, pitch
                        FROM ac_player_homes WHERE owner_uuid = ?
                        ORDER BY home_id ASC
                        """)) {
            ps.setString(1, ownerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(ownerUuid, rs));
                }
            }
        }
        return list;
    }

    private HomeDefinition selectHome(Connection connection, UUID ownerUuid, String homeId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                        SELECT home_id, display_name, icon_material, world, x, y, z, yaw, pitch
                        FROM ac_player_homes WHERE owner_uuid = ? AND home_id = ?
                        """)) {
            ps.setString(1, ownerUuid.toString());
            ps.setString(2, homeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapRow(ownerUuid, rs);
            }
        }
    }

    private HomeDefinition mapRow(UUID ownerUuid, ResultSet rs) throws SQLException {
        Material icon = WarpStorage.parseMaterial(rs.getString("icon_material"), settings.defaultHomeIcon());
        return new HomeDefinition(
                ownerUuid,
                rs.getString("home_id"),
                rs.getString("display_name"),
                icon,
                rs.getString("world"),
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getFloat("yaw"),
                rs.getFloat("pitch"));
    }

    private void upsertHome(Connection connection, HomeDefinition home) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO ac_player_homes (
                  owner_uuid, home_id, display_name, icon_material, world, x, y, z, yaw, pitch
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  display_name = VALUES(display_name),
                  icon_material = VALUES(icon_material),
                  world = VALUES(world),
                  x = VALUES(x),
                  y = VALUES(y),
                  z = VALUES(z),
                  yaw = VALUES(yaw),
                  pitch = VALUES(pitch)
                """)) {
            ps.setString(1, home.ownerUuid().toString());
            ps.setString(2, home.id());
            ps.setString(3, home.displayName());
            ps.setString(4, home.listIcon().name());
            ps.setString(5, home.worldName());
            ps.setDouble(6, home.x());
            ps.setDouble(7, home.y());
            ps.setDouble(8, home.z());
            ps.setFloat(9, home.yaw());
            ps.setFloat(10, home.pitch());
            ps.executeUpdate();
        }
    }
}
