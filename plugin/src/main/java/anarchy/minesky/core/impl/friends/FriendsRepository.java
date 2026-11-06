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
 * Amizades persistentes: linha {@code (player_uuid, friend_uuid)} = {@code player} listou {@code friend}.
 */
public final class FriendsRepository {

    private final DatabaseManager database;

    public FriendsRepository(DatabaseManager database) {
        this.database = database;
    }

    public boolean isActive() {
        return database.isActive();
    }

    public boolean addFriend(UUID playerId, UUID friendId) {
        if (!database.isActive() || playerId == null || friendId == null || playerId.equals(friendId)) {
            return false;
        }
        Boolean ok = database.querySync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT IGNORE INTO ac_player_friends (player_uuid, friend_uuid) VALUES (?, ?)
                    """)) {
                ps.setString(1, playerId.toString());
                ps.setString(2, friendId.toString());
                return ps.executeUpdate() > 0;
            }
        });
        return Boolean.TRUE.equals(ok);
    }

    public boolean removeFriend(UUID playerId, UUID friendId) {
        if (!database.isActive() || playerId == null || friendId == null) {
            return false;
        }
        Boolean ok = database.querySync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    DELETE FROM ac_player_friends WHERE player_uuid = ? AND friend_uuid = ?
                    """)) {
                ps.setString(1, playerId.toString());
                ps.setString(2, friendId.toString());
                return ps.executeUpdate() > 0;
            }
        });
        return Boolean.TRUE.equals(ok);
    }

    public boolean areFriends(UUID playerId, UUID friendId) {
        if (!database.isActive() || playerId == null || friendId == null) {
            return false;
        }
        Boolean ok = database.querySync(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT 1 FROM ac_player_friends WHERE player_uuid = ? AND friend_uuid = ? LIMIT 1
                    """)) {
                ps.setString(1, playerId.toString());
                ps.setString(2, friendId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
        return Boolean.TRUE.equals(ok);
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
                SELECT friend_uuid FROM ac_player_friends WHERE player_uuid = ? ORDER BY created_at ASC
                """)) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                List<UUID> out = new ArrayList<>();
                while (rs.next()) {
                    try {
                        out.add(UUID.fromString(rs.getString("friend_uuid")));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                return out;
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
