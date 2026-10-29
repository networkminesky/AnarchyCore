package anarchy.minesky.core.command;

import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.homes.HomeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class HomeCommand implements CommandExecutor, TabCompleter {

    private final MessagesManager messages;
    private final HomeManager homes;

    public HomeCommand(MessagesManager messages, HomeManager homes) {
        this.messages = messages;
        this.homes = homes;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.component("commands.player-only"));
            return true;
        }
        if (args.length == 0) {
            homes.openHomeList(player);
            return true;
        }
        String arg = args[0];
        int colon = arg.indexOf(':');
        if (colon > 0 && colon < arg.length() - 1) {
            String owner = arg.substring(0, colon);
            String homeId = arg.substring(colon + 1);
            homes.teleportToOtherHome(player, owner, homeId);
            return true;
        }
        homes.teleportToHome(player, arg);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length != 1 || !(sender instanceof Player player)) {
            return List.of();
        }
        return homes.completeHomeArgument(player, args[0]);
    }
}
