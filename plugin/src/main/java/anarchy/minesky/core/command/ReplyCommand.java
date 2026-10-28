package anarchy.minesky.core.command;

import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.messaging.PrivateMessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ReplyCommand implements CommandExecutor {

    private final MessagesManager messages;
    private final PrivateMessageService privateMessages;

    public ReplyCommand(MessagesManager messages, PrivateMessageService privateMessages) {
        this.messages = messages;
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
        privateMessages.sendReply(player, args);
        return true;
    }
}
