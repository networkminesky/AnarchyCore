package anarchy.minesky.core.impl.escmenu.flows;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

public final class VisibleOnlinePlayers {

    private VisibleOnlinePlayers() {}

    public static boolean isValidTarget(Player viewer, Player target) {
        if (viewer == null || target == null || !target.isOnline()) {
            return false;
        }
        return passesFilter(
                viewer.getUniqueId(),
                target.getUniqueId(),
                true,
                target.hasMetadata("vanished"),
                viewer.canSee(target));
    }

    public static boolean passesFilter(
            java.util.UUID viewerId,
            java.util.UUID targetId,
            boolean targetOnline,
            boolean targetVanished,
            boolean canSee) {
        if (viewerId == null || targetId == null || !targetOnline) {
            return false;
        }
        if (viewerId.equals(targetId)) {
            return false;
        }
        if (targetVanished) {
            return false;
        }
        return canSee;
    }

    public static Predicate<Player> visibleTo(Player viewer) {
        return candidate -> isValidTarget(viewer, candidate);
    }

    public static List<Player> sortedVisible(Player viewer) {
        List<Player> result = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (isValidTarget(viewer, online)) {
                result.add(online);
            }
        }
        result.sort(Comparator.comparing(p -> p.getName().toLowerCase(Locale.ROOT)));
        return result;
    }

    public static Optional<Player> resolveVisible(Player viewer, String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return Optional.empty();
        }
        String name = rawName.trim();
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null && isValidTarget(viewer, exact)) {
            return Optional.of(exact);
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!isValidTarget(viewer, online)) {
                continue;
            }
            if (online.getName().equalsIgnoreCase(name)) {
                return Optional.of(online);
            }
        }
        return Optional.empty();
    }
}