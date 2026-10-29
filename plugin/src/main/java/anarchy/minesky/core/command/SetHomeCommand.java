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

public final class SetHomeCommand implements CommandExecutor, TabCompleter {

    private final MessagesManager messages;
    private final HomeManager homes;

    public SetHomeCommand(MessagesManager messages, HomeManager homes) {
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
            player.sendMessage(messages.component("homes.usage.sethome"));
            return true;
        }
        homes.beginSetHome(player, args[0]);
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
        return homes.completeOwnHomeIds(player, args[0]);
    }
}
