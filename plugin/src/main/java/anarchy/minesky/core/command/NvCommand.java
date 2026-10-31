package anarchy.minesky.core.command;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class NvCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "anarchycore.nv";
    public static final String OTHERS_PERMISSION = "anarchycore.nv.others";

    private final MessagesManager messages;
    private final FeedbackManager feedback;

    public NvCommand(MessagesManager messages, FeedbackManager feedback) {
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

        boolean enabled = toggleNightVision(target);
        notify(player, target, enabled);
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

    private void notify(Player executor, Player target, boolean enabled) {
        if (target.getUniqueId().equals(executor.getUniqueId())) {
            String key = enabled ? "nv.enabled-self" : "nv.disabled-self";
            feedback.send(
                    executor,
                    messages.component(key),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS);
            return;
        }

        if (enabled) {
            feedback.send(
                    executor,
                    messages.component("nv.enabled-other-executor", "target", target.getName()),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS);
            feedback.send(
                    target,
                    messages.component("nv.enabled-other-target", "player", executor.getName()),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS);
        } else {
            feedback.send(
                    executor,
                    messages.component("nv.disabled-other-executor", "target", target.getName()),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS);
            feedback.send(
                    target,
                    messages.component("nv.disabled-other-target", "player", executor.getName()),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS);
        }
    }

    private static boolean toggleNightVision(Player target) {
        if (target.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            target.removePotionEffect(PotionEffectType.NIGHT_VISION);
            return false;
        }
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.NIGHT_VISION,
                PotionEffect.INFINITE_DURATION,
                0,
                false,
                false,
                true));
        return true;
    }
}
