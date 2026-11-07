package anarchy.minesky.core.command;

import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.kits.KitManager;
import anarchy.minesky.core.impl.kits.KitStorage;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class KitsCommandHandler {

    private final KitManager kitManager;
    private final MessagesManager messages;
    private final FeedbackManager feedback;

    public KitsCommandHandler(KitManager kitManager, MessagesManager messages, FeedbackManager feedback) {
        this.kitManager = kitManager;
        this.messages = messages;
        this.feedback = feedback;
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (!sender.hasPermission(KitManager.ADMIN_PERMISSION)) {
            send(sender, messages.component("commands.no-permission"));
            return true;
        }
        if (args.length < 2) {
            send(sender, messages.component("kits.admin.usage"));
            return true;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "create" -> handleCreate(sender, args);
            case "edit" -> handleEdit(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "reload" -> handleReload(sender);
            default -> {
                send(sender, messages.component("kits.admin.usage"));
                yield true;
            }
        };
    }

    public List<String> tabComplete(String[] args) {
        if (args.length == 2) {
            return filterPrefix(args[1], List.of("create", "edit", "remove", "reload"));
        }
        if (args.length == 3) {
            String sub = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("edit") || sub.equals("remove")) {
                return filterPrefix(args[2], kitManager.storage().ids());
            }
            return List.of();
        }
        return List.of();
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, messages.component("commands.player-only"));
            return true;
        }
        if (args.length < 3) {
            send(sender, messages.component("kits.admin.create-usage"));
            return true;
        }
        kitManager.beginCreateKit(player, args[2]);
        return true;
    }

    private boolean handleEdit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, messages.component("commands.player-only"));
            return true;
        }
        if (args.length < 3) {
            send(sender, messages.component("kits.admin.edit-usage"));
            return true;
        }
        kitManager.beginEditKit(player, args[2]);
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, messages.component("kits.admin.remove-usage"));
            return true;
        }
        String id = KitStorage.normalizeId(args[2]);
        if (kitManager.removeKit(id)) {
            send(sender, messages.component("kits.admin.remove-success", "kit", id));
        } else {
            send(sender, messages.component("kits.error.not-found", "kit", id));
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        kitManager.reloadStorage();
        send(sender, messages.component("kits.admin.reload-success"));
        return true;
    }

    private void send(CommandSender sender, Component message) {
        if (sender instanceof Player player) {
            feedback.send(player, message);
        } else {
            sender.sendMessage(message);
        }
    }

    private static List<String> filterPrefix(String input, List<String> options) {
        if (input == null || input.isEmpty()) {
            return options;
        }
        String prefix = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                result.add(option);
            }
        }
        return result;
    }
}
