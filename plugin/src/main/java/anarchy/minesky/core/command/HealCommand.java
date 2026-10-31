package anarchy.minesky.core.command;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class HealCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "anarchycore.heal";
    public static final String OTHERS_PERMISSION = "anarchycore.heal.others";

    private final MessagesManager messages;
    private final FeedbackManager feedback;

    public HealCommand(MessagesManager messages, FeedbackManager feedback) {
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

        if (!player.hasPermission(PERMISSION)) {
            feedback.send(
                    player,
                    messages.component("commands.no-permission"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return true;
        }

        Player target;
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
            target = CommandPlayers.resolve(args[0]);
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

        heal(target);

        if (target.getUniqueId().equals(player.getUniqueId())) {
            feedback.send(
                    player,
                    messages.component("heal.self"),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS);
        } else {
            feedback.send(
                    player,
                    messages.component("heal.other-executor", "target", target.getName()),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS);
            feedback.send(
                    target,
                    messages.component("heal.other-target", "player", player.getName()),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS);
        }
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

    private static void heal(Player target) {
        var maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            target.setFoodLevel(20);
            target.setHealth(maxHealth.getValue());
        }
        target.setFireTicks(0);
    }
}
