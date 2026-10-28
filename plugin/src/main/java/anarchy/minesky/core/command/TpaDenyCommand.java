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

public final class TpaDenyCommand implements CommandExecutor, TabCompleter {

    private final MessagesManager messages;
    private final TpaManager tpa;

    public TpaDenyCommand(MessagesManager messages, TpaManager tpa) {
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
        String requesterName = args.length >= 1 ? args[0] : null;
        tpa.denyRequest(player, requesterName);
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
