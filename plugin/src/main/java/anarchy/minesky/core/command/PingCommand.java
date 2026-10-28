package anarchy.minesky.core.command;

import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class PingCommand implements CommandExecutor, TabCompleter {

    public static final String OTHERS_PERMISSION = "anarchycore.ping.others";

    private final MessagesManager messages;
    private final FeedbackManager feedback;

    public PingCommand(MessagesManager messages, FeedbackManager feedback) {
        this.messages = messages;
        this.feedback = feedback;
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
            showPing(player, player);
            return true;
        }

        if (!player.hasPermission(OTHERS_PERMISSION)) {
            feedback.send(player, messages.component("commands.no-permission"));
            return true;
        }

        Player target = resolvePlayer(args[0]);
        if (target == null) {
            feedback.send(player, messages.component("commands.player-not-found", "player", args[0]));
            return true;
        }
        showPing(player, target);
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
        if (!player.hasPermission(OTHERS_PERMISSION)) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }

    private void showPing(Player viewer, Player target) {
        int ping = target.getPing();
        Component message;
        if (viewer.getUniqueId().equals(target.getUniqueId())) {
            message = messages.component("ping.self", "ping", String.valueOf(ping));
        } else {
            message = messages.component("ping.other", "target", target.getName(), "ping", String.valueOf(ping));
        }
        feedback.send(viewer, message);
    }

    private static Player resolvePlayer(String name) {
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
