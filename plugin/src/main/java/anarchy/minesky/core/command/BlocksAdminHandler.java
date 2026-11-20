package anarchy.minesky.core.command;

import anarchy.minesky.core.impl.blocks.BlockManager;
import anarchy.minesky.core.impl.blocks.BlockStorage;
import anarchy.minesky.core.impl.blocks.BlockType;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BlocksAdminHandler {

    public static final String PERMISSION = BlockStorage.ADMIN_PERMISSION;

    private final BlockManager blocks;
    private final MessagesManager messages;
    private final FeedbackManager feedback;

    public BlocksAdminHandler(BlockManager blocks, MessagesManager messages, FeedbackManager feedback) {
        this.blocks = blocks;
        this.messages = messages;
        this.feedback = feedback;
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            send(sender, messages.component("commands.no-permission"));
            return true;
        }
        if (args.length < 2) {
            send(sender, messages.component("blocks.admin.usage"));
            return true;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "create" -> handleCreate(sender, args);
            case "edit" -> handleEdit(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "reload" -> handleReload(sender);
            default -> {
                send(sender, messages.component("blocks.admin.usage"));
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
                return filterPrefix(args[2], blocks.storage().ids());
            }
            if (sub.equals("create")) {
                return filterPrefix(args[2], List.of());
            }
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("create")) {
            return filterPrefix(args[3], List.of("POSITION", "CLICK"));
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("edit")) {
            return filterPrefix(args[3], List.of("location", "type", "commands"));
        }
        if (args.length == 5 && args[1].equalsIgnoreCase("edit") && args[3].equalsIgnoreCase("type")) {
            return filterPrefix(args[4], List.of("POSITION", "CLICK"));
        }
        return List.of();
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, messages.component("commands.player-only"));
            return true;
        }
        if (args.length < 4) {
            send(sender, messages.component("blocks.admin.create-usage"));
            return true;
        }
        String id = BlockStorage.normalizeId(args[2]);
        if (!BlockStorage.isValidId(id)) {
            send(sender, messages.component("blocks.error.invalid-id"));
            return true;
        }
        if (blocks.storage().get(id).isPresent()) {
            send(sender, messages.component("blocks.error.already-exists", "id", id));
            return true;
        }
        BlockType type = BlockType.parse(args[3]);
        if (type == null) {
            send(sender, messages.component("blocks.error.invalid-type"));
            return true;
        }
        List<String> commands = new ArrayList<>();
        if (args.length > 4) {
            commands.addAll(joinRemainingAsCommands(args, 4));
        }
        blocks.createAtPlayer(player, id, type, commands);
        send(sender, messages.component("blocks.admin.create-success", "id", id));
        return true;
    }

    private boolean handleEdit(CommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, messages.component("blocks.admin.edit-usage"));
            return true;
        }
        String id = BlockStorage.normalizeId(args[2]);
        if (blocks.storage().get(id).isEmpty()) {
            send(sender, messages.component("blocks.error.not-found", "id", id));
            return true;
        }
        if (args.length == 3) {
            send(sender, messages.component("blocks.admin.edit-usage"));
            return true;
        }
        String field = args[3].toLowerCase(Locale.ROOT);
        return switch (field) {
            case "location" -> {
                if (!(sender instanceof Player player)) {
                    send(sender, messages.component("commands.player-only"));
                    yield true;
                }
                if (blocks.updateLocation(player, id)) {
                    send(sender, messages.component("blocks.admin.edit-location-success", "id", id));
                } else {
                    send(sender, messages.component("blocks.error.not-found", "id", id));
                }
                yield true;
            }
            case "type" -> {
                if (args.length < 5) {
                    send(sender, messages.component("blocks.admin.edit-type-usage"));
                    yield true;
                }
                BlockType type = BlockType.parse(args[4]);
                if (type == null) {
                    send(sender, messages.component("blocks.error.invalid-type"));
                    yield true;
                }
                if (blocks.updateType(id, type)) {
                    send(sender, messages.component("blocks.admin.edit-type-success", "id", id));
                } else {
                    send(sender, messages.component("blocks.error.not-found", "id", id));
                }
                yield true;
            }
            case "commands" -> {
                List<String> commands = joinRemainingAsCommands(args, 4);
                if (commands.isEmpty()) {
                    send(sender, messages.component("blocks.admin.edit-commands-usage"));
                    yield true;
                }
                if (blocks.updateCommands(id, commands)) {
                    send(sender, messages.component("blocks.admin.edit-commands-success", "id", id));
                } else {
                    send(sender, messages.component("blocks.error.not-found", "id", id));
                }
                yield true;
            }
            default -> {
                send(sender, messages.component("blocks.admin.edit-usage"));
                yield true;
            }
        };
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, messages.component("blocks.admin.remove-usage"));
            return true;
        }
        String id = BlockStorage.normalizeId(args[2]);
        if (blocks.remove(id)) {
            send(sender, messages.component("blocks.admin.remove-success", "id", id));
        } else {
            send(sender, messages.component("blocks.error.not-found", "id", id));
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        blocks.reload();
        send(sender, messages.component("blocks.admin.reload-success"));
        return true;
    }

    private static List<String> joinRemainingAsCommands(String[] args, int fromIndex) {
        if (fromIndex >= args.length) {
            return List.of();
        }
        StringBuilder builder = new StringBuilder(args[fromIndex]);
        for (int i = fromIndex + 1; i < args.length; i++) {
            builder.append(' ').append(args[i]);
        }
        return List.of(builder.toString());
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
        if (options.isEmpty()) {
            return List.of();
        }
        String prefix = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}