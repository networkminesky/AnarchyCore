package anarchy.minesky.core.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

final class CommandPlayers {

    private CommandPlayers() {}

    static @Nullable OfflinePlayer resolveOffline(String name) {
        Player online = resolve(name);
        if (online != null) {
            return online;
        }
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached != null && cached.hasPlayedBefore()) {
            return cached;
        }
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            String offlineName = offline.getName();
            if (offlineName != null
                    && offlineName.equalsIgnoreCase(name)
                    && offline.hasPlayedBefore()) {
                return offline;
            }
        }
        return null;
    }

    static Player resolve(String name) {
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return exact;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(name)) {
                return online;
            }
        }
        return null;
    }
}
