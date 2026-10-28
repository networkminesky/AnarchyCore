package anarchy.minesky.core.command;

import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.tpa.TpaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class TpaHereCommand implements CommandExecutor, TabCompleter {

    private final MessagesManager messages;
    private final TpaManager tpa;

    public TpaHereCommand(MessagesManager messages, TpaManager tpa) {
        this.messages = messages;
        this.tpa = tpa;
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
            sender.sendMessage(messages.component("tpa.usage.tpahere"));
            return true;
        }
        Player target = TpaManager.resolvePlayer(args[0]);
        if (target == null) {
            sender.sendMessage(messages.component("commands.player-not-found", "player", args[0]));
            return true;
        }
        tpa.requestTpaHereWithConfirm(player, target);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        return PlayerNameTabComplete.onlineNames(args, 0);
    }
}
