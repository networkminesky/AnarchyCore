package anarchy.minesky.core.command;

import anarchy.minesky.core.AnarchyCore;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.kits.KitManager;
import net.kyori.adventure.text.Component;
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

public final class AnarchyCoreCommand implements CommandExecutor, TabCompleter {

    public static final String RELOAD_PERMISSION = "anarchycore.reload";

    private final AnarchyCore plugin;
    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final GuardCommandHandler guardCommandHandler;
    private final KitsCommandHandler kitsCommandHandler;
    private final ReferralAdminHandler referralAdminHandler;
    private final BlocksAdminHandler blocksAdminHandler;

    public AnarchyCoreCommand(
            AnarchyCore plugin,
            MessagesManager messages,
            FeedbackManager feedback,
            GuardCommandHandler guardCommandHandler,
            KitsCommandHandler kitsCommandHandler,
            ReferralAdminHandler referralAdminHandler,
            BlocksAdminHandler blocksAdminHandler) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
        this.guardCommandHandler = guardCommandHandler;
        this.kitsCommandHandler = kitsCommandHandler;
        this.referralAdminHandler = referralAdminHandler;
        this.blocksAdminHandler = blocksAdminHandler;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0) {
            send(sender, messages.component("commands.reload-usage"));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(RELOAD_PERMISSION)) {
                send(sender, messages.component("commands.no-permission"));
                return true;
            }
            plugin.reloadPlugin();
            send(sender, messages.component("commands.reload-success"));
            return true;
        }
        if (args[0].equalsIgnoreCase("guard")) {
            return guardCommandHandler.handle(sender, args);
        }
        if (args[0].equalsIgnoreCase("kits")) {
            return kitsCommandHandler.handle(sender, args);
        }
        if (args[0].equalsIgnoreCase("convite")) {
            if (referralAdminHandler != null && referralAdminHandler.handle(sender, args)) {
                return true;
            }
            send(sender, messages.component("referral.admin.usage"));
            return true;
        }
        if (args[0].equalsIgnoreCase("blocks")) {
            return blocksAdminHandler.handle(sender, args);
        }
        send(sender, messages.component("commands.reload-usage"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission(RELOAD_PERMISSION)) {
                options.add("reload");
            }
            if (sender.hasPermission(GuardCommandHandler.PERMISSION)) {
                options.add("guard");
            }
            if (sender.hasPermission(KitManager.ADMIN_PERMISSION)) {
                options.add("kits");
            }
            if (sender.hasPermission(ReferralAdminHandler.PERMISSION)) {
                options.add("convite");
            }
            if (sender.hasPermission(BlocksAdminHandler.PERMISSION)) {
                options.add("blocks");
            }
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return options.stream().filter(o -> o.startsWith(prefix)).toList();
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("guard")) {
            return guardCommandHandler.tabComplete(args);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("kits")) {
            return kitsCommandHandler.tabComplete(args);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("convite") && referralAdminHandler != null) {
            return referralAdminHandler.tabComplete(args);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("blocks")) {
            return blocksAdminHandler.tabComplete(args);
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
