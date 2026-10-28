package anarchy.minesky.core.command;

import anarchy.minesky.core.AnarchyCore;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class AnarchyCoreCommand implements CommandExecutor, TabCompleter {

    public static final String RELOAD_PERMISSION = "anarchycore.reload";

    private final AnarchyCore plugin;
    private final MessagesManager messages;
    private final FeedbackManager feedback;

    public AnarchyCoreCommand(AnarchyCore plugin, MessagesManager messages, FeedbackManager feedback) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            send(sender, messages.component("commands.reload-usage"));
            return true;
        }
        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            send(sender, messages.component("commands.no-permission"));
            return true;
        }
        plugin.reloadPlugin();
        send(sender, messages.component("commands.reload-success"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            if ("reload".startsWith(prefix)) {
                return List.of("reload");
            }
        }
        return List.of();
    }

    private void send(CommandSender sender, Component message) {
        if (sender instanceof Player player) {
            feedback.send(player, message);
        } else {
            sender.sendMessage(message);
        }
    }
}
