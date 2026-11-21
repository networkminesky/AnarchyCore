package anarchy.minesky.core.command;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.escmenu.EscMenuRuntime;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class EscMenuCommand implements CommandExecutor {

    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final EscMenuRuntime escMenu;

    public EscMenuCommand(MessagesManager messages, FeedbackManager feedback, EscMenuRuntime escMenu) {
        this.messages = messages;
        this.feedback = feedback;
        this.escMenu = escMenu;
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
        if (!player.hasPermission("anarchycore.menu")) {
            feedback.send(player, messages.component("commands.no-permission"), FeedbackPriority.HIGH, true, UiSoundMode.ERROR);
            return true;
        }
        escMenu.showHub(player);
        return true;
    }
}