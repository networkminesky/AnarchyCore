package anarchy.minesky.core.impl.friends;

import anarchy.minesky.core.impl.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Amizades (ambas as direções em {@code ac_player_friends}), pedidos em {@code ac_player_friend_requests}.
 */
public final class FriendsRepository {

    private final DatabaseManager database;

    public FriendsRepository(DatabaseManager database) {
        this.database = database;
    }

    public boolean isActive() {
        return database.isActive();
    }

    /**
     * Amizade mútua: ambas as direções em {@code ac_player_friends}.
     * @return true se pelo menos uma linha foi inserida (novo par).
     */
    public boolean addFriendshipMutual(UUID a, UUID b) {
        if (!database.isActive() || a == null || b == null || a.equals(b)) {
            return false;
        }
        Boolean ok = database.querySync(connection -> {
            boolean any = false;
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT IGNORE INTO ac_player_friends (player_uuid, friend_uuid) VALUES (?, ?)
                    """)) {
                ps.setString(1, a.toString());
                ps.setString(2, b.toString());
                if (ps.executeUpdate() > 0) {
                    any = true;
                }
            }
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT IGNORE INTO ac_player_friends (player_uuid, friend_uuid) VALUES (?, ?)
                    """)) {
                ps.setString(1, b.toString());
                ps.setString(2, a.toString());
                if (ps.executeUpdate() > 0) {
                    any = true;
                }
            }
            return any;
        });
        return Boolean.TRUE.equals(ok);
    }

    /** Remove a amizade nas duas direções. */
    public boolean removeFriendshipMutual(UUID playerId, UUID friendId) {
        if (!database.isActive() || playerId == null || friendId == null) {
            return false;
        }
        Boolean ok = database.querySync(connection -> {
            int n = 0;
            try (PreparedStatement ps = connection.prepareStatement("""
                    DELETE FROM ac_player_friends WHERE player_uuid = ? AND friend_uuid = ?
                    """)) {
                ps.setString(1, playerId.toString());
                ps.setString(2, friendId.toString());
                n += ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("""
                    DELETE FROM ac_player_friends WHERE player_uuid = ? AND friend_uuid = ?
                    """)) {
                ps.setString(1, friendId.toString());
                ps.setString(2, playerId.toString());
                n += ps.executeUpdate();
            }
            return n > 0;
        });
        return Boolean.TRUE.equals(ok);
    }

    public boolean areFriends(UUID playerId, UUID friendId) {
        if (!database.isActive() || playerId == null || friendId == null) {
            return false;
        }
        Boolean ok = database.querySync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT 1 FROM ac_player_friends
                    WHERE (player_uuid = ? AND friend_uuid = ?) OR (player_uuid = ? AND friend_uuid = ?)
                    LIMIT 1
                    """)) {
                String a = playerId.toString();
                String b = friendId.toString();
                ps.setString(1, a);
                ps.setString(2, b);
                ps.setString(3, b);
                ps.setString(4, a);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
        return Boolean.TRUE.equals(ok);
    }

    /** true se inseriu novo pedido; false se já existia (duplicata). */
    public boolean createRequest(UUID fromId, UUID toId) {
        if (!database.isActive() || fromId == null || toId == null || fromId.equals(toId)) {
            return false;
        }
        Boolean ok = database.querySync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT IGNORE INTO ac_player_friend_requests (from_uuid, to_uuid) VALUES (?, ?)
                    """)) {
                ps.setString(1, fromId.toString());
                ps.setString(2, toId.toString());
                return ps.executeUpdate() > 0;
            }
        });
        return Boolean.TRUE.equals(ok);
    }

    public boolean deleteRequest(UUID fromId, UUID toId) {
        if (!database.isActive() || fromId == null || toId == null) {
            return false;
        }
        Boolean ok = database.querySync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    DELETE FROM ac_player_friend_requests WHERE from_uuid = ? AND to_uuid = ?
                    """)) {
                ps.setString(1, fromId.toString());
                ps.setString(2, toId.toString());
                return ps.executeUpdate() > 0;
            }
        });
        return Boolean.TRUE.equals(ok);
    }

    public boolean hasOutgoingRequest(UUID fromId, UUID toId) {
        if (!database.isActive() || fromId == null || toId == null) {
            return false;
        }
        return Boolean.TRUE.equals(database.querySync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT 1 FROM ac_player_friend_requests WHERE from_uuid = ? AND to_uuid = ? LIMIT 1
                    """)) {
                ps.setString(1, fromId.toString());
                ps.setString(2, toId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        }));
    }

    public boolean hasIncomingRequest(UUID toId, UUID fromId) {
        return hasOutgoingRequest(fromId, toId);
    }

    /** Pedidos recebidos por {@code toId} (quem enviou). */
    public List<UUID> listIncomingRequestFromUuids(UUID toId) {
        if (!database.isActive() || toId == null) {
            return List.of();
        }
        List<UUID> list = database.querySync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT from_uuid FROM ac_player_friend_requests WHERE to_uuid = ? ORDER BY created_at ASC
                    """)) {
                ps.setString(1, toId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    List<UUID> out = new ArrayList<>();
                    while (rs.next()) {
                        try {
                            out.add(UUID.fromString(rs.getString("from_uuid")));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    return out;
                }
            }
        });
        return list != null ? list : List.of();
    }

    /**
     * UUIDs que {@code playerId} adicionou como amigos (ordem de inserção aproximada).
     */
    public List<UUID> listFriendUuids(UUID playerId) {
        if (!database.isActive() || playerId == null) {
            return List.of();
        }
        List<UUID> list = database.querySync(connection -> loadFriendUuids(connection, playerId));
        return list != null ? list : List.of();
    }

    private static List<UUID> loadFriendUuids(Connection connection, UUID playerId) throws java.sql.SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT friend_uuid FROM ac_player_friends WHERE player_uuid = ?
                UNION
                SELECT player_uuid AS friend_uuid FROM ac_player_friends WHERE friend_uuid = ?
                ORDER BY friend_uuid
                """)) {
            String id = playerId.toString();
            ps.setString(1, id);
            ps.setString(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                LinkedHashSet<UUID> distinct = new LinkedHashSet<>();
                while (rs.next()) {
                    try {
                        distinct.add(UUID.fromString(rs.getString("friend_uuid")));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                return new ArrayList<>(distinct);
            }
        }
    }

    /**
     * Jogadores que têm {@code friendId} na lista de amigos (para avisar quando {@code friendId} entrar).
     */
    public Set<UUID> whoListsAsFriend(UUID friendId) {
        if (!database.isActive() || friendId == null) {
            return Set.of();
        }
        Set<UUID> set = database.querySync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT player_uuid FROM ac_player_friends WHERE friend_uuid = ?
                    """)) {
                ps.setString(1, friendId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    Set<UUID> out = new LinkedHashSet<>();
                    while (rs.next()) {
                        try {
                            out.add(UUID.fromString(rs.getString("player_uuid")));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    return out;
                }
            }
        });
        return set != null ? set : Set.of();
    }
}
