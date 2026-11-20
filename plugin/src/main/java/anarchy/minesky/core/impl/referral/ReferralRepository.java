package anarchy.minesky.core.impl.referral;

import anarchy.minesky.core.impl.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class ReferralRepository {

    private final DatabaseManager database;

    public ReferralRepository(DatabaseManager database) {
        this.database = database;
    }

    public boolean isActive() {
        return database.isActive();
    }

    public String getOrCreateCode(UUID playerUuid, ReferralCodeGenerator generator, int maxAttempts) {
        if (!isActive()) {
            return null;
        }
        return database.querySync(connection -> {
            String existing = selectCodeByPlayer(connection, playerUuid);
            if (existing != null) {
                return existing;
            }
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                String code = generator.generate();
                if (insertCode(connection, playerUuid, code)) {
                    return code;
                }
            }
            return null;
        });
    }

    public String getCodeByPlayer(UUID playerUuid) {
        if (!isActive()) {
            return null;
        }
        return database.querySync(connection -> selectCodeByPlayer(connection, playerUuid));
    }

    public enum AssignResult {
        APPLIED,
        UNCHANGED,
        TAKEN
    }

    public record CodeState(String code, long changedAt, int changeCount) {
    }

    public CodeState codeState(UUID playerUuid) {
        if (!isActive() || playerUuid == null) {
            return null;
        }
        return database.querySync(connection -> selectCodeState(connection, playerUuid));
    }

    /**
     * Grava o código já normalizado. Duplicata (inclusive caixa diferente) vira {@link AssignResult#TAKEN}
     * sem propagar a SQLException da constraint {@code UNIQUE}.
     * Indicações antigas continuam em {@code referrer_uuid}; esta tabela não é chave estrangeira delas.
     */
    public AssignResult assignCustomCode(UUID playerUuid, String normalizedCode, long now) {
        if (!isActive() || playerUuid == null || normalizedCode == null || normalizedCode.isBlank()) {
            return null;
        }
        String code = normalizedCode.trim().toUpperCase(Locale.ROOT);
        return database.querySync(connection -> {
            UUID owner = selectCodeOwner(connection, code);
            if (owner != null && !owner.equals(playerUuid)) {
                return AssignResult.TAKEN;
            }
            CodeState current = selectCodeState(connection, playerUuid);
            if (current == null) {
                return insertCustomCode(connection, playerUuid, code, now);
            }
            if (current.code() != null && current.code().equalsIgnoreCase(code)) {
                return AssignResult.UNCHANGED;
            }
            return updateCustomCode(connection, playerUuid, code, now);
        });
    }

    public ReferralWelcomeGate loadWelcomeGate(UUID playerUuid) {
        if (!isActive() || playerUuid == null) {
            return null;
        }
        return database.querySync(connection -> selectWelcomeGate(connection, playerUuid));
    }

    public CompletableFuture<ReferralWelcomeGate> loadWelcomeGateAsync(UUID playerUuid) {
        return database.submit(connection -> selectWelcomeGate(connection, playerUuid));
    }

    public void markWelcomeSeen(UUID playerUuid) {
        if (!isActive() || playerUuid == null) {
            return;
        }
        database.withConnection(connection -> {
            try {
                upsertWelcomeSeen(connection, playerUuid);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> markWelcomeSeenAsync(UUID playerUuid) {
        return database.submit(connection -> {
            upsertWelcomeSeen(connection, playerUuid);
            return null;
        });
    }

    public UUID getReferrerByCode(String code) {
        if (!isActive() || code == null) {
            return null;
        }
        return database.querySync(connection -> selectReferrerByCode(connection, code.trim().toUpperCase()));
    }

    public boolean isCodeBlocked(String code) {
        if (!isActive()) {
            return false;
        }
        Boolean blocked = database.querySync(connection -> selectCodeBlocked(connection, code));
        return Boolean.TRUE.equals(blocked);
    }

    public boolean isPlayerBlocked(UUID uuid) {
        if (!isActive()) {
            return false;
        }
        Boolean blocked = database.querySync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT 1 FROM ac_referral_blocks WHERE player_uuid = ? LIMIT 1")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
        return Boolean.TRUE.equals(blocked);
    }

    public void blockPlayer(UUID uuid, String reason) {
        if (!isActive()) {
            return;
        }
        database.withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO ac_referral_blocks (player_uuid, blocked_at, reason)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE blocked_at = VALUES(blocked_at), reason = VALUES(reason)
                    """)) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, System.currentTimeMillis());
                ps.setString(3, reason);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public ReferralRecord getByInvitee(UUID invitee) {
        if (!isActive()) {
            return null;
        }
        return database.querySync(connection -> selectByInvitee(connection, invitee));
    }

    public ReferralRecord getById(long id) {
        if (!isActive()) {
            return null;
        }
        return database.querySync(connection -> selectById(connection, id));
    }

    public boolean hasInviteeEverRedeemed(UUID invitee) {
        return getByInvitee(invitee) != null;
    }

    /** A↔B e cadeia indireta até {@code maxDepth} saltos a partir do indicador. */
    public boolean wouldCreateCycle(UUID referrer, UUID invitee) {
        return wouldCreateCycle(referrer, invitee, 16);
    }

    public boolean wouldCreateCycle(UUID referrer, UUID invitee, int maxDepth) {
        if (!isActive() || referrer == null || invitee == null) {
            return false;
        }
        int limit = Math.max(1, maxDepth);
        Boolean cycle = join(connection -> {
            UUID cursor = referrer;
            for (int depth = 0; depth < limit; depth++) {
                ReferralRecord row = selectByInvitee(connection, cursor);
                if (row == null) {
                    return false;
                }
                if (row.referrerUuid().equals(invitee)) {
                    return true;
                }
                cursor = row.referrerUuid();
            }
            return false;
        }, false);
        return Boolean.TRUE.equals(cycle);
    }

    /**
     * Resgates que ocupam vaga na sub-rede. Não conta o próprio convidado, nem
     * {@code EXPIRED} / {@code BLOCKED_SUSPICIOUS}.
     */
    public int countActiveRedemptionsOnSubnet(String subnetKey, UUID excludeInvitee) {
        if (!isActive() || subnetKey == null || subnetKey.isBlank()) {
            return 0;
        }
        Integer count = join(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT COUNT(DISTINCT r.invitee_uuid) AS cnt
                    FROM ac_referral_redemptions r
                    INNER JOIN ac_player_ip_history h ON h.player_uuid = r.invitee_uuid
                    WHERE h.subnet_key = ?
                      AND r.invitee_uuid <> ?
                      AND r.status NOT IN ('EXPIRED', 'BLOCKED_SUSPICIOUS')
                    """)) {
                ps.setString(1, subnetKey);
                ps.setString(2, excludeInvitee != null ? excludeInvitee.toString() : "");
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt("cnt") : 0;
                }
            }
        }, 0);
        return count != null ? count : 0;
    }

    public Long createRedemption(UUID invitee, UUID referrer, String code, long now, long expiresAt) {
        if (!isActive()) {
            return null;
        }
        return database.querySync(connection -> {
            connection.setAutoCommit(false);
            try {
                Long id = insertRedemption(connection, invitee, referrer, code, now, expiresAt);
                connection.commit();
                return id;
            } catch (SQLException e) {
                connection.rollback();
                if (isDuplicateInvitee(e)) {
                    return null;
                }
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        });
    }

    public int countRewardsInPeriod(UUID referrer, long sinceMs) {
        if (!isActive()) {
            return 0;
        }
        Integer count = join(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT COUNT(*) AS cnt FROM ac_referral_redemptions
                    WHERE referrer_uuid = ?
                      AND (
                        (status = 'REWARDED' AND rewarded_at >= ?)
                        OR (status IN ('PENDING', 'QUALIFIED_PENDING_PAYOUT', 'PROCESSING_PAYOUT', 'PAYOUT_FAILED')
                            AND created_at >= ?)
                      )
                    """)) {
                ps.setString(1, referrer.toString());
                ps.setLong(2, sinceMs);
                ps.setLong(3, sinceMs);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt("cnt") : 0;
                }
            }
        }, 0);
        return count != null ? count : 0;
    }

    public int countTotalRewards(UUID referrer) {
        if (!isActive()) {
            return 0;
        }
        Integer count = join(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT COUNT(*) AS cnt FROM ac_referral_redemptions
                    WHERE referrer_uuid = ?
                      AND status IN (
                        'PENDING', 'QUALIFIED_PENDING_PAYOUT', 'PROCESSING_PAYOUT', 'PAYOUT_FAILED', 'REWARDED'
                      )
                    """)) {
                ps.setString(1, referrer.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt("cnt") : 0;
                }
            }
        }, 0);
        return count != null ? count : 0;
    }

    public boolean transitionStatus(long id, ReferralStatus from, ReferralStatus to) {
        if (!isActive()) {
            return false;
        }
        Boolean ok = join(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    UPDATE ac_referral_redemptions
                    SET status = ?
                    WHERE id = ? AND status = ?
                    """)) {
                ps.setString(1, to.dbValue());
                ps.setLong(2, id);
                ps.setString(3, from.dbValue());
                return ps.executeUpdate() == 1;
            }
        }, false);
        return Boolean.TRUE.equals(ok);
    }

    public boolean markQualified(long id, long qualifiedAt) {
        if (!isActive()) {
            return false;
        }
        Boolean ok = join(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    UPDATE ac_referral_redemptions
                    SET status = ?, qualified_at = ?
                    WHERE id = ? AND status = ?
                    """)) {
                ps.setString(1, ReferralStatus.QUALIFIED_PENDING_PAYOUT.dbValue());
                ps.setLong(2, qualifiedAt);
                ps.setLong(3, id);
                ps.setString(4, ReferralStatus.PENDING.dbValue());
                return ps.executeUpdate() == 1;
            }
        }, false);
        return Boolean.TRUE.equals(ok);
    }

    public boolean reservePayout(long id, String idempotencyKey) {
        if (!isActive()) {
            return false;
        }
        Boolean ok = join(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    UPDATE ac_referral_redemptions
                    SET status = ?, payout_idempotency_key = ?
                    WHERE id = ? AND status = ?
                    """)) {
                ps.setString(1, ReferralStatus.PROCESSING_PAYOUT.dbValue());
                ps.setString(2, idempotencyKey);
                ps.setLong(3, id);
                ps.setString(4, ReferralStatus.QUALIFIED_PENDING_PAYOUT.dbValue());
                return ps.executeUpdate() == 1;
            }
        }, false);
        return Boolean.TRUE.equals(ok);
    }

    /** Marca que o {@code deposit} foi disparado. Retry automático não credita de novo. */
    public boolean markPayoutSent(long id) {
        if (!isActive()) {
            return false;
        }
        Boolean ok = join(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    UPDATE ac_referral_redemptions
                    SET payout_idempotency_key = CONCAT(payout_idempotency_key, ':sent')
                    WHERE id = ? AND status = ?
                      AND payout_idempotency_key IS NOT NULL
                      AND payout_idempotency_key NOT LIKE '%:sent'
                    """)) {
                ps.setLong(1, id);
                ps.setString(2, ReferralStatus.PROCESSING_PAYOUT.dbValue());
                return ps.executeUpdate() == 1;
            }
        }, false);
        return Boolean.TRUE.equals(ok);
    }

    public boolean markRewarded(long id, long rewardedAt) {
        if (!isActive()) {
            return false;
        }
        Boolean ok = join(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    UPDATE ac_referral_redemptions
                    SET status = ?, rewarded_at = ?
                    WHERE id = ? AND status = ?
                    """)) {
                ps.setString(1, ReferralStatus.REWARDED.dbValue());
                ps.setLong(2, rewardedAt);
                ps.setLong(3, id);
                ps.setString(4, ReferralStatus.PROCESSING_PAYOUT.dbValue());
                return ps.executeUpdate() == 1;
            }
        }, false);
        return Boolean.TRUE.equals(ok);
    }

    public boolean markPayoutFailed(long id) {
        if (!isActive()) {
            return false;
        }
        Boolean ok = join(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    UPDATE ac_referral_redemptions
                    SET status = ?
                    WHERE id = ? AND status = ?
                    """)) {
                ps.setString(1, ReferralStatus.PAYOUT_FAILED.dbValue());
                ps.setLong(2, id);
                ps.setString(3, ReferralStatus.PROCESSING_PAYOUT.dbValue());
                return ps.executeUpdate() == 1;
            }
        }, false);
        return Boolean.TRUE.equals(ok);
    }

    public boolean releasePayoutToRetry(long id) {
        if (!isActive()) {
            return false;
        }
        Boolean ok = join(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    UPDATE ac_referral_redemptions
                    SET status = ?
                    WHERE id = ? AND status IN (?, ?)
                    """)) {
                ps.setString(1, ReferralStatus.QUALIFIED_PENDING_PAYOUT.dbValue());
                ps.setLong(2, id);
                ps.setString(3, ReferralStatus.PAYOUT_FAILED.dbValue());
                ps.setString(4, ReferralStatus.PROCESSING_PAYOUT.dbValue());
                return ps.executeUpdate() == 1;
            }
        }, false);
        return Boolean.TRUE.equals(ok);
    }

    public boolean markBlockedSuspicious(long id) {
        if (!isActive()) {
            return false;
        }
        Boolean ok = join(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    UPDATE ac_referral_redemptions
                    SET status = ?
                    WHERE id = ? AND status NOT IN ('REWARDED', 'BLOCKED_SUSPICIOUS')
                    """)) {
                ps.setString(1, ReferralStatus.BLOCKED_SUSPICIOUS.dbValue());
                ps.setLong(2, id);
                return ps.executeUpdate() == 1;
            }
        }, false);
        return Boolean.TRUE.equals(ok);
    }

    public void updateQualificationProgress(long id, long activePlaytimeMs, double distance, int mined, int placed) {
        if (!isActive()) {
            return;
        }
        database.withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    UPDATE ac_referral_redemptions
                    SET active_playtime_ms = ?, distance_traveled = ?, blocks_mined = ?, blocks_placed = ?
                    WHERE id = ? AND status = ?
                    """)) {
                ps.setLong(1, activePlaytimeMs);
                ps.setDouble(2, distance);
                ps.setInt(3, mined);
                ps.setInt(4, placed);
                ps.setLong(5, id);
                ps.setString(6, ReferralStatus.PENDING.dbValue());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public List<ReferralRecord> listPendingPayoutOrQualification() {
        if (!isActive()) {
            return List.of();
        }
        List<ReferralRecord> rows = database.querySync(connection -> {
            List<ReferralRecord> list = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT * FROM ac_referral_redemptions
                    WHERE status IN (?, ?, ?, ?)
                    ORDER BY id ASC
                    LIMIT 200
                    """)) {
                ps.setString(1, ReferralStatus.PENDING.dbValue());
                ps.setString(2, ReferralStatus.QUALIFIED_PENDING_PAYOUT.dbValue());
                ps.setString(3, ReferralStatus.PAYOUT_FAILED.dbValue());
                ps.setString(4, ReferralStatus.PROCESSING_PAYOUT.dbValue());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
            return list;
        });
        return rows != null ? rows : List.of();
    }

    public List<ReferralRecord> listByReferrer(UUID referrer) {
        if (!isActive()) {
            return List.of();
        }
        List<ReferralRecord> rows = database.querySync(connection -> {
            List<ReferralRecord> list = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT * FROM ac_referral_redemptions WHERE referrer_uuid = ? ORDER BY created_at DESC LIMIT 100
                    """)) {
                ps.setString(1, referrer.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapRow(rs));
                    }
                }
            }
            return list;
        });
        return rows != null ? rows : List.of();
    }

    public int expireStale(long now) {
        if (!isActive()) {
            return 0;
        }
        Integer updated = join(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    UPDATE ac_referral_redemptions
                    SET status = ?
                    WHERE status = ? AND expires_at < ?
                    """)) {
                ps.setString(1, ReferralStatus.EXPIRED.dbValue());
                ps.setString(2, ReferralStatus.PENDING.dbValue());
                ps.setLong(3, now);
                return ps.executeUpdate();
            }
        }, 0);
        return updated != null ? updated : 0;
    }

    private static CodeState selectCodeState(Connection connection, UUID playerUuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT code, code_changed_at, code_change_count
                FROM ac_referral_codes WHERE player_uuid = ?
                """)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new CodeState(rs.getString("code"), rs.getLong("code_changed_at"), rs.getInt("code_change_count"));
            }
        }
    }

    private static UUID selectCodeOwner(Connection connection, String code) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player_uuid FROM ac_referral_codes WHERE UPPER(code) = ?")) {
            ps.setString(1, code.toUpperCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return UUID.fromString(rs.getString("player_uuid"));
            }
        }
    }

    private static AssignResult insertCustomCode(Connection connection, UUID playerUuid, String code, long now)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO ac_referral_codes
                  (player_uuid, code, created_at, blocked, code_changed_at, code_change_count)
                VALUES (?, ?, ?, 0, ?, 1)
                """)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, code);
            ps.setLong(3, now);
            ps.setLong(4, now);
            return ps.executeUpdate() == 1 ? AssignResult.APPLIED : null;
        } catch (SQLException e) {
            if (isDuplicateCode(e)) {
                return AssignResult.TAKEN;
            }
            throw e;
        }
    }

    private static AssignResult updateCustomCode(Connection connection, UUID playerUuid, String code, long now)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                UPDATE ac_referral_codes
                SET code = ?, code_changed_at = ?, code_change_count = code_change_count + 1
                WHERE player_uuid = ?
                """)) {
            ps.setString(1, code);
            ps.setLong(2, now);
            ps.setString(3, playerUuid.toString());
            if (ps.executeUpdate() == 1) {
                return AssignResult.APPLIED;
            }
            return null;
        } catch (SQLException e) {
            if (isDuplicateCode(e)) {
                return AssignResult.TAKEN;
            }
            throw e;
        }
    }

    private static ReferralWelcomeGate selectWelcomeGate(Connection connection, UUID playerUuid) throws SQLException {
        boolean seen = false;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT referral_dialog_seen FROM ac_player_identity WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    seen = rs.getBoolean("referral_dialog_seen");
                }
            }
        }
        boolean redeemed = selectByInvitee(connection, playerUuid) != null;
        boolean blocked = false;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM ac_referral_blocks WHERE player_uuid = ? LIMIT 1")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                blocked = rs.next();
            }
        }
        return new ReferralWelcomeGate(seen, redeemed, blocked);
    }

    private static void upsertWelcomeSeen(Connection connection, UUID playerUuid) throws SQLException {
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO ac_player_identity (player_uuid, first_join_at, client_brand, referral_dialog_seen)
                VALUES (?, ?, '', 1)
                ON DUPLICATE KEY UPDATE referral_dialog_seen = 1
                """)) {
            ps.setString(1, playerUuid.toString());
            ps.setLong(2, now);
            ps.executeUpdate();
        }
    }

    private static String selectCodeByPlayer(Connection connection, UUID playerUuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT code FROM ac_referral_codes WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("code") : null;
            }
        }
    }

    private static boolean insertCode(Connection connection, UUID playerUuid, String code) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO ac_referral_codes (player_uuid, code, created_at, blocked)
                VALUES (?, ?, ?, 0)
                """)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, code);
            ps.setLong(3, System.currentTimeMillis());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            if (isDuplicateCode(e)) {
                return false;
            }
            throw e;
        }
    }

    private static UUID selectReferrerByCode(Connection connection, String code) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT player_uuid FROM ac_referral_codes
                WHERE UPPER(code) = ? AND blocked = 0
                """)) {
            ps.setString(1, code.toUpperCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return UUID.fromString(rs.getString("player_uuid"));
            }
        }
    }

    private static boolean selectCodeBlocked(Connection connection, String code) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT blocked FROM ac_referral_codes WHERE UPPER(code) = ?")) {
            ps.setString(1, code.toUpperCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                return rs.getBoolean("blocked");
            }
        }
    }

    private static Long insertRedemption(Connection connection, UUID invitee, UUID referrer, String code, long now,
                                         long expiresAt) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO ac_referral_redemptions
                  (invitee_uuid, referrer_uuid, code, status, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, invitee.toString());
            ps.setString(2, referrer.toString());
            ps.setString(3, code.toUpperCase());
            ps.setString(4, ReferralStatus.PENDING.dbValue());
            ps.setLong(5, now);
            ps.setLong(6, expiresAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            return null;
        }
    }

    private static ReferralRecord selectByInvitee(Connection connection, UUID invitee) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM ac_referral_redemptions WHERE invitee_uuid = ?")) {
            ps.setString(1, invitee.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapRow(rs);
            }
        }
    }

    private static ReferralRecord selectById(Connection connection, long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM ac_referral_redemptions WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapRow(rs);
            }
        }
    }

    private static ReferralRecord mapRow(ResultSet rs) throws SQLException {
        long qualifiedAt = rs.getLong("qualified_at");
        Long qualified = rs.wasNull() ? null : qualifiedAt;
        long rewardedAt = rs.getLong("rewarded_at");
        Long rewarded = rs.wasNull() ? null : rewardedAt;
        return new ReferralRecord(
                rs.getLong("id"),
                UUID.fromString(rs.getString("invitee_uuid")),
                UUID.fromString(rs.getString("referrer_uuid")),
                rs.getString("code"),
                ReferralStatus.fromDb(rs.getString("status")),
                rs.getLong("created_at"),
                rs.getLong("expires_at"),
                qualified,
                rewarded,
                rs.getString("payout_idempotency_key"),
                rs.getLong("active_playtime_ms"),
                rs.getDouble("distance_traveled"),
                rs.getInt("blocks_mined"),
                rs.getInt("blocks_placed"));
    }

    private <T> T join(DatabaseManager.SqlFunction<T> function, T fallback) {
        if (!isActive()) {
            return fallback;
        }
        try {
            T value = database.submit(function).join();
            return value != null ? value : fallback;
        } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            database.plugin().getLogger().log(java.util.logging.Level.SEVERE,
                    "[Referral] SQL falhou e o resultado foi descartado", cause);
            return fallback;
        }
    }

    private static boolean isDuplicateInvitee(SQLException e) {
        return isDuplicateKey(e);
    }

    private static boolean isDuplicateCode(SQLException e) {
        return isDuplicateKey(e);
    }

    private static boolean isDuplicateKey(SQLException e) {
        int code = e.getErrorCode();
        if (code == 1062 || code == 23505) {
            return true;
        }
        String state = e.getSQLState();
        if ("23000".equals(state) || "23505".equals(state)) {
            return true;
        }
        String message = e.getMessage();
        return message != null && (message.contains("Duplicate") || message.contains("unique"));
    }
}