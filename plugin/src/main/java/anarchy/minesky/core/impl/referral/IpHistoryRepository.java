package anarchy.minesky.core.impl.referral;

import anarchy.minesky.core.impl.database.DatabaseManager;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;

public final class IpHistoryRepository {

    private final DatabaseManager database;
    private final ReferralSettings settings;

    public IpHistoryRepository(DatabaseManager database, ReferralSettings settings) {
        this.database = database;
        this.settings = settings;
    }

    public boolean isActive() {
        return database.isActive();
    }

    public void recordIp(UUID playerUuid, InetAddress address) {
        if (!isActive() || address == null || playerUuid == null) {
            return;
        }
        String ip = NetworkAddressUtils.normalizeIp(address);
        String subnet = NetworkAddressUtils.subnetKey(address, settings.ipv4Slash24(), settings.ipv6Slash64());
        long now = System.currentTimeMillis();
        try {
            database.submit(connection -> {
                upsertIp(connection, playerUuid, ip, subnet, now);
                return Boolean.TRUE;
            }).join();
        } catch (CompletionException ignored) {
            // submit já registrou a falha de SQL
        }
    }

    public void recordIpAsync(UUID playerUuid, InetAddress address) {
        if (!isActive() || address == null || playerUuid == null) {
            return;
        }
        String ip = NetworkAddressUtils.normalizeIp(address);
        String subnet = NetworkAddressUtils.subnetKey(address, settings.ipv4Slash24(), settings.ipv6Slash64());
        long now = System.currentTimeMillis();
        database.submit(connection -> {
            upsertIp(connection, playerUuid, ip, subnet, now);
            return Boolean.TRUE;
        }).exceptionally(error -> {
            Throwable cause = error.getCause() != null ? error.getCause() : error;
            database.plugin().getLogger().log(java.util.logging.Level.SEVERE,
                    "[Referral] Falha ao gravar IP de " + playerUuid, cause);
            return null;
        });
    }

    public Set<String> ipsForPlayer(UUID uuid) {
        if (!isActive()) {
            return Set.of();
        }
        Set<String> result = database.querySync(connection -> selectIps(connection, uuid));
        return result != null ? result : Set.of();
    }

    public Set<String> subnetsForPlayer(UUID uuid) {
        if (!isActive()) {
            return Set.of();
        }
        Set<String> result = database.querySync(connection -> selectSubnets(connection, uuid));
        return result != null ? result : Set.of();
    }

    public int countDistinctPlayersOnSubnet(String subnetKey) {
        if (!isActive() || subnetKey == null || subnetKey.isBlank()) {
            return 0;
        }
        Integer count = database.querySync(connection -> countPlayersOnSubnet(connection, subnetKey));
        return count != null ? count : 0;
    }

    public boolean shareAnyIp(UUID a, UUID b) {
        if (!isActive() || a == null || b == null) {
            return false;
        }
        Boolean shared = database.querySync(connection -> existsShared(connection, a, b, "ip_address"));
        return Boolean.TRUE.equals(shared);
    }

    public boolean shareAnySubnet(UUID a, UUID b) {
        if (!isActive() || a == null || b == null) {
            return false;
        }
        Boolean shared = database.querySync(connection -> existsShared(connection, a, b, "subnet_key"));
        return Boolean.TRUE.equals(shared);
    }

    private static boolean existsShared(Connection connection, UUID a, UUID b, String column) throws SQLException {
        String sql = """
                SELECT 1
                FROM ac_player_ip_history left_row
                INNER JOIN ac_player_ip_history right_row ON left_row.%s = right_row.%s
                WHERE left_row.player_uuid = ? AND right_row.player_uuid = ?
                LIMIT 1
                """.formatted(column, column);
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, a.toString());
            ps.setString(2, b.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void upsertIp(Connection connection, UUID playerUuid, String ip, String subnet, long now)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO ac_player_ip_history (player_uuid, ip_address, subnet_key, first_seen, last_seen)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE last_seen = VALUES(last_seen), subnet_key = VALUES(subnet_key)
                """)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, ip);
            ps.setString(3, subnet);
            ps.setLong(4, now);
            ps.setLong(5, now);
            ps.executeUpdate();
        }
    }

    private static Set<String> selectIps(Connection connection, UUID uuid) throws SQLException {
        Set<String> ips = new HashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT ip_address FROM ac_player_ip_history WHERE player_uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ips.add(rs.getString("ip_address"));
                }
            }
        }
        return ips;
    }

    private static Set<String> selectSubnets(Connection connection, UUID uuid) throws SQLException {
        Set<String> subnets = new HashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT DISTINCT subnet_key FROM ac_player_ip_history WHERE player_uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    subnets.add(rs.getString("subnet_key"));
                }
            }
        }
        return subnets;
    }

    private static int countPlayersOnSubnet(Connection connection, String subnetKey) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT COUNT(DISTINCT player_uuid) AS cnt
                FROM ac_player_ip_history
                WHERE subnet_key = ?
                """)) {
            ps.setString(1, subnetKey);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("cnt") : 0;
            }
        }
    }
}