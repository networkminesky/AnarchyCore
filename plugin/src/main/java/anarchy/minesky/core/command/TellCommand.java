package anarchy.minesky.core.command;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.messaging.PrivateMessageService;
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

public final class TellCommand implements CommandExecutor, TabCompleter {

    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final PrivateMessageService privateMessages;

    public TellCommand(MessagesManager messages, FeedbackManager feedback, PrivateMessageService privateMessages) {
        this.messages = messages;
        this.feedback = feedback;
        this.privateMessages = privateMessages;
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
            feedback.send(
                    player,
                    messages.component("commands.tell-usage"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return true;
        }
        String targetName = args[0];
        String[] messageArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        privateMessages.sendTell(player, targetName, messageArgs);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return List.of();
        }
        if (args.length == 0 || args.length > 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }
}
