package anarchy.minesky.core.command;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.sounds.CoreSounds;
import anarchy.minesky.core.impl.sounds.GlobalSoundSettings;
import anarchy.minesky.core.utils.RichMessageParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class BroadcastCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "anarchycore.broadcast";

    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final GlobalSoundSettings soundSettings;

    public BroadcastCommand(MessagesManager messages, FeedbackManager feedback, GlobalSoundSettings soundSettings) {
        this.messages = messages;
        this.feedback = feedback;
        this.soundSettings = soundSettings;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            deny(sender);
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(messages.component("broadcast.usage"));
            return true;
        }

        boolean silent = "-s".equalsIgnoreCase(args[0]);
        String rawMessage;
        if (silent) {
            if (args.length < 2) {
                sender.sendMessage(messages.component("broadcast.usage"));
                return true;
            }
            rawMessage = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        } else {
            rawMessage = String.join(" ", args);
        }

        Component body = RichMessageParser.format(sender, rawMessage);
        deliver(body, silent);

        if (sender instanceof Player player) {
            feedback.send(
                    player,
                    messages.component("broadcast.sent"),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS);
        } else {
            sender.sendMessage(messages.component("broadcast.sent"));
        }
        return true;
    }

    private void deliver(Component message, boolean silent) {
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
            if (!silent) {
                CoreSounds.playBroadcast(player, soundSettings);
            }
        }
    }

    private void deny(CommandSender sender) {
        if (sender instanceof Player player) {
            feedback.send(
                    player,
                    messages.component("commands.no-permission"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
        } else {
            sender.sendMessage(messages.component("commands.no-permission"));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION) || args.length != 1) {
            return List.of();
        }
        if ("-s".startsWith(args[0].toLowerCase(Locale.ROOT))) {
            return List.of("-s");
        }
        return List.of();
    }
}
