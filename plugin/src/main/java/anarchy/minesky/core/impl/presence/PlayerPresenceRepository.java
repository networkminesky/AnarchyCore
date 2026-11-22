package anarchy.minesky.core.impl.presence;

import anarchy.minesky.core.impl.database.DatabaseManager;
import org.bukkit.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerPresenceRepository {

    private final DatabaseManager database;

    public PlayerPresenceRepository(DatabaseManager database) {
        this.database = database;
    }

    public boolean isActive() {
        return database.isActive();
    }

    public void upsertJoinAsync(UUID uuid, String playerName, long joinMillis) {
        if (!isActive() || uuid == null || playerName == null) {
            return;
        }
        database.runAsync(() -> upsertJoinSync(uuid, playerName, joinMillis));
    }

    public void upsertJoinSync(UUID uuid, String playerName, long joinMillis) {
        if (!isActive() || uuid == null || playerName == null) {
            return;
        }
        database.querySync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO ac_player_presence (uuid, player_name, last_join_at)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                      player_name = VALUES(player_name),
                      last_join_at = VALUES(last_join_at)
                    """)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, playerName);
                ps.setLong(3, joinMillis);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void saveQuitAsync(UUID uuid, String playerName, long quitMillis, Location location) {
        if (!isActive() || uuid == null) {
            return;
        }
        database.runAsync(() -> saveQuitSync(uuid, playerName, quitMillis, location));
    }

    public void saveQuitSync(UUID uuid, String playerName, long quitMillis, Location location) {
        if (!isActive() || uuid == null) {
            return;
        }
        String world = location != null && location.getWorld() != null ? location.getWorld().getName() : null;
        Double x = location != null ? location.getX() : null;
        Double y = location != null ? location.getY() : null;
        Double z = location != null ? location.getZ() : null;
        Float yaw = location != null ? location.getYaw() : null;
        Float pitch = location != null ? location.getPitch() : null;
        String name = playerName != null ? playerName : "";
        database.querySync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO ac_player_presence (
                      uuid, player_name, last_quit_at, world, x, y, z, yaw, pitch
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                      player_name = VALUES(player_name),
                      last_quit_at = VALUES(last_quit_at),
                      world = VALUES(world),
                      x = VALUES(x),
                      y = VALUES(y),
                      z = VALUES(z),
                      yaw = VALUES(yaw),
                      pitch = VALUES(pitch)
                    """)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setLong(3, quitMillis);
                ps.setString(4, world);
                setNullable(ps, 5, x);
                setNullable(ps, 6, y);
                setNullable(ps, 7, z);
                setNullableFloat(ps, 8, yaw);
                setNullableFloat(ps, 9, pitch);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public CompletableFuture<Optional<PlayerPresenceRecord>> findByUuid(UUID uuid) {
        if (!isActive() || uuid == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return database.submit(connection -> findByUuid(connection, uuid));
    }

    public CompletableFuture<Optional<PlayerPresenceRecord>> findByNameIgnoreCase(String playerName) {
        if (!isActive() || playerName == null || playerName.isBlank()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return database.submit(connection -> findByName(connection, playerName));
    }

    private static Optional<PlayerPresenceRecord> findByUuid(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT uuid, player_name, last_join_at, last_quit_at,
                       world, x, y, z, yaw, pitch
                FROM ac_player_presence WHERE uuid = ? LIMIT 1
                """)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(readRow(rs));
            }
        }
    }

    private static Optional<PlayerPresenceRecord> findByName(Connection connection, String playerName)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT uuid, player_name, last_join_at, last_quit_at,
                       world, x, y, z, yaw, pitch
                FROM ac_player_presence
                WHERE LOWER(player_name) = LOWER(?)
                ORDER BY updated_at DESC
                LIMIT 1
                """)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(readRow(rs));
            }
        }
    }

    private static PlayerPresenceRecord readRow(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        String name = rs.getString("player_name");
        Long join = rs.getObject("last_join_at") != null ? rs.getLong("last_join_at") : null;
        Long quit = rs.getObject("last_quit_at") != null ? rs.getLong("last_quit_at") : null;
        String world = rs.getString("world");
        Double x = rs.getObject("x") != null ? rs.getDouble("x") : null;
        Double y = rs.getObject("y") != null ? rs.getDouble("y") : null;
        Double z = rs.getObject("z") != null ? rs.getDouble("z") : null;
        Float yaw = rs.getObject("yaw") != null ? rs.getFloat("yaw") : null;
        Float pitch = rs.getObject("pitch") != null ? rs.getFloat("pitch") : null;
        return new PlayerPresenceRecord(uuid, name, join, quit, world, x, y, z, yaw, pitch);
    }

    private static void setNullable(PreparedStatement ps, int index, @org.jetbrains.annotations.Nullable Double value)
            throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.DOUBLE);
        } else {
            ps.setDouble(index, value);
        }
    }

    private static void setNullableFloat(PreparedStatement ps, int index, @org.jetbrains.annotations.Nullable Float value)
            throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.FLOAT);
        } else {
            ps.setFloat(index, value);
        }
    }
}