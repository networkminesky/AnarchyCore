package anarchy.minesky.core.impl.leaderboards;

import anarchy.minesky.core.command.StatsCommand;

import java.util.UUID;

public final class LeaderboardInteractions {

    private LeaderboardInteractions() {}

    public static boolean profileClickAllowed(UUID viewerId, UUID linePlayerId, boolean hasOthersPermission) {
        if (viewerId == null || linePlayerId == null) {
            return false;
        }
        if (viewerId.equals(linePlayerId)) {
            return true;
        }
        return hasOthersPermission;
    }

    public static boolean hasViewOthersPermission(org.bukkit.entity.Player viewer) {
        return viewer != null && viewer.hasPermission(StatsCommand.OTHERS_PERMISSION);
    }
}