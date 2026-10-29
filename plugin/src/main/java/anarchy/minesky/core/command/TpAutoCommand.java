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
import java.util.Locale;

public final class TpAutoCommand implements CommandExecutor, TabCompleter {

    private final MessagesManager messages;
    private final TpaManager tpa;

    public TpAutoCommand(MessagesManager messages, TpaManager tpa) {
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
            tpa.toggleTpAuto(player);
            return true;
        }
        String arg = args[0].toLowerCase(Locale.ROOT);
        if (arg.equals("on") || arg.equals("ligar") || arg.equals("sim")) {
            tpa.setTpAuto(player, true);
            return true;
        }
        if (arg.equals("off") || arg.equals("desligar") || arg.equals("nao")) {
            tpa.setTpAuto(player, false);
            return true;
        }
        sender.sendMessage(messages.component("tpa.tpauto.usage"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return List.of("on", "off").stream().filter(s -> s.startsWith(prefix)).toList();
    }
}
