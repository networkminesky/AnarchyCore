package anarchy.minesky.core.command;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.utils.PlaytimeFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class TempoJogoCommand implements CommandExecutor, TabCompleter {

    public static final String OTHERS_PERMISSION = "anarchycore.tempojogo.others";

    private final MessagesManager messages;
    private final FeedbackManager feedback;

    public TempoJogoCommand(MessagesManager messages, FeedbackManager feedback) {
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

        OfflinePlayer target;
        if (args.length == 0) {
            target = player;
        } else {
            if (!player.hasPermission(OTHERS_PERMISSION)) {
                feedback.send(
                        player,
                        messages.component("commands.no-permission"),
                        FeedbackPriority.HIGH,
                        true,
                        UiSoundMode.ERROR);
                return true;
            }
            target = CommandPlayers.resolveOffline(args[0]);
            if (target == null) {
                feedback.send(
                        player,
                        messages.component("commands.player-not-found", "player", args[0]),
                        FeedbackPriority.HIGH,
                        true,
                        UiSoundMode.ERROR);
                return true;
            }
        }

        showPlaytime(player, target);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (!(sender instanceof Player player) || !player.hasPermission(OTHERS_PERMISSION)) {
            return List.of();
        }
        return PlayerNameTabComplete.onlineNames(args, 0);
    }

    private void showPlaytime(Player viewer, OfflinePlayer target) {
        int ticks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
        String formatted = PlaytimeFormat.fromPlayTicks(ticks);
        String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();

        Component message;
        if (target.getUniqueId().equals(viewer.getUniqueId())) {
            message = messages.component("playtime.self", "time", formatted);
        } else {
            message = messages.component("playtime.other", "target", targetName, "time", formatted);
        }
        feedback.send(viewer, message, FeedbackPriority.NORMAL, true, UiSoundMode.SUCCESS);
    }
}
