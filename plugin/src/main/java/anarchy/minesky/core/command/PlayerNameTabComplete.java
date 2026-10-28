package anarchy.minesky.core.command;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

final class PlayerNameTabComplete {

    private PlayerNameTabComplete() {}

    static List<String> onlineNames(String[] args, int argIndex) {
        if (args.length != argIndex + 1) {
            return List.of();
        }
        String prefix = args[argIndex].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }
}
