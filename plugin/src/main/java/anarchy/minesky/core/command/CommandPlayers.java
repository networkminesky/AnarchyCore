package anarchy.minesky.core.command;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

final class CommandPlayers {

    private CommandPlayers() {}

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
