package anarchy.minesky.core.command;

import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.referral.ReferralManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ConviteCommand implements CommandExecutor, TabCompleter {

    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final ReferralManager referral;

    public ConviteCommand(MessagesManager messages, FeedbackManager feedback, ReferralManager referral) {
        this.messages = messages;
        this.feedback = feedback;
        this.referral = referral;
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
        if (!player.hasPermission(ReferralManager.PERMISSION)) {
            feedback.send(player, messages.component("commands.no-permission"), FeedbackPriority.HIGH, true,
                    UiSoundMode.ERROR);
            return true;
        }
        if (args.length == 0) {
            referral.showOrCreateCode(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "usar", "use", "resgatar" -> {
                if (args.length < 2) {
                    feedback.send(player, messages.component("referral.usage.usar"), FeedbackPriority.HIGH, true,
                            UiSoundMode.ERROR);
                    yield true;
                }
                referral.redeemCode(player, args[1]);
                yield true;
            }
            case "codigo", "code" -> {
                referral.showOrCreateCode(player);
                yield true;
            }
            case "status", "info" -> {
                referral.showStatus(player);
                yield true;
            }
            default -> {
                feedback.send(player, messages.component("referral.usage.root"), FeedbackPriority.HIGH, true,
                        UiSoundMode.ERROR);
                yield true;
            }
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (!(sender instanceof Player player) || !player.hasPermission(ReferralManager.PERMISSION)) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> options = List.of("usar", "codigo", "status");
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (String option : options) {
                if (option.startsWith(prefix)) {
                    result.add(option);
                }
            }
            return result;
        }
        return List.of();
    }
}