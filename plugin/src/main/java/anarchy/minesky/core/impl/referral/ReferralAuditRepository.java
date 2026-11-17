package anarchy.minesky.core.impl.referral;

import anarchy.minesky.core.impl.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ReferralAuditRepository {

    public record AuditEntry(
            long id,
            Long referralId,
            UUID playerUuid,
            String eventType,
            String reason,
            String detail,
            long createdAt) {
    }

    private final DatabaseManager database;

    public ReferralAuditRepository(DatabaseManager database) {
        this.database = database;
    }

    public boolean isActive() {
        return database.isActive();
    }

    public void logAsync(UUID playerUuid, Long referralId, String eventType, ReferralRejectReason reason, String detail) {
        if (!isActive()) {
            return;
        }
        long now = System.currentTimeMillis();
        String reasonKey = reason != null ? reason.auditKey() : null;
        database.submit(connection -> {
            insert(connection, playerUuid, referralId, eventType, reasonKey, detail, now);
            return Boolean.TRUE;
        });
    }

    public void logSync(Connection connection, UUID playerUuid, Long referralId, String eventType,
                        ReferralRejectReason reason, String detail) throws SQLException {
        insert(connection, playerUuid, referralId, eventType, reason != null ? reason.auditKey() : null, detail,
                System.currentTimeMillis());
    }

    public List<AuditEntry> historyForPlayer(UUID uuid, int limit) {
        if (!isActive()) {
            return List.of();
        }
        List<AuditEntry> result = database.querySync(connection -> selectByPlayer(connection, uuid, limit));
        return result != null ? result : List.of();
    }

    private static void insert(Connection connection, UUID playerUuid, Long referralId, String eventType,
                               String reason, String detail, long createdAt) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO ac_referral_audit (referral_id, player_uuid, event_type, reason, detail, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            if (referralId != null) {
                ps.setLong(1, referralId);
            } else {
                ps.setNull(1, java.sql.Types.BIGINT);
            }
            ps.setString(2, playerUuid.toString());
            ps.setString(3, eventType);
            ps.setString(4, reason);
            ps.setString(5, detail);
            ps.setLong(6, createdAt);
            ps.executeUpdate();
        }
    }

    private static List<AuditEntry> selectByPlayer(Connection connection, UUID uuid, int limit) throws SQLException {
        List<AuditEntry> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT id, referral_id, player_uuid, event_type, reason, detail, created_at
                FROM ac_referral_audit
                WHERE player_uuid = ?
                ORDER BY created_at DESC
                LIMIT ?
                """)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private static AuditEntry mapRow(ResultSet rs) throws SQLException {
        long refId = rs.getLong("referral_id");
        Long referralId = rs.wasNull() ? null : refId;
        return new AuditEntry(
                rs.getLong("id"),
                referralId,
                UUID.fromString(rs.getString("player_uuid")),
                rs.getString("event_type"),
                rs.getString("reason"),
                rs.getString("detail"),
                rs.getLong("created_at"));
    }
}