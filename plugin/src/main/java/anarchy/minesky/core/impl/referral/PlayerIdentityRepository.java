package anarchy.minesky.core.impl.referral;

import anarchy.minesky.core.impl.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class PlayerIdentityRepository {

    private final DatabaseManager database;

    public PlayerIdentityRepository(DatabaseManager database) {
        this.database = database;
    }

    public boolean isActive() {
        return database.isActive();
    }

    public long ensureFirstJoin(UUID uuid, String clientBrand) {
        if (!isActive()) {
            return System.currentTimeMillis();
        }
        Long result = database.querySync(connection -> upsertFirstJoin(connection, uuid, clientBrand));
        return result != null ? result : System.currentTimeMillis();
    }

    public Long getFirstJoinAt(UUID uuid) {
        if (!isActive()) {
            return null;
        }
        return database.querySync(connection -> selectFirstJoin(connection, uuid));
    }

    private static long upsertFirstJoin(Connection connection, UUID uuid, String clientBrand) throws SQLException {
        long now = System.currentTimeMillis();
        String brand = clientBrand != null ? clientBrand : "";
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO ac_player_identity (player_uuid, first_join_at, client_brand)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE player_uuid = player_uuid
                """)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, now);
            ps.setString(3, brand);
            ps.executeUpdate();
        }
        Long existing = selectFirstJoin(connection, uuid);
        return existing != null ? existing : now;
    }

    private static Long selectFirstJoin(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT first_join_at FROM ac_player_identity WHERE player_uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getLong("first_join_at");
            }
        }
    }
}