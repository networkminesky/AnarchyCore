package anarchy.minesky.core.command;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.profile.PlayerProfileFlow;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class StatsCommand implements CommandExecutor, TabCompleter {

    public static final String OTHERS_PERMISSION = "anarchycore.stats.others";

    private final Plugin plugin;
    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final PlayerProfileFlow profiles;

    public StatsCommand(
            Plugin plugin, MessagesManager messages, FeedbackManager feedback, PlayerProfileFlow profiles) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
        this.profiles = profiles;
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
            player.getScheduler().run(plugin, task -> profiles.openSelf(player), null);
            return true;
        }
        if (!player.hasPermission(OTHERS_PERMISSION)) {
            feedback.send(
                    player,
                    messages.component("commands.no-permission"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return true;
        }
        String name = args[0];
        OfflinePlayer offline = CommandPlayers.resolveOffline(name);
        if (offline == null) {
            feedback.send(
                    player,
                    messages.component("commands.player-not-found", "player", name),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return true;
        }
        player.getScheduler().run(plugin, task -> profiles.openOffline(player, offline, null), null);
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
}