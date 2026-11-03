package anarchy.minesky.core.command;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.utils.ItemRichText;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class LoreCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "anarchycore.lore";

    private static final List<String> SUBCOMMANDS = List.of("add", "remove", "set");

    private final MessagesManager messages;
    private final FeedbackManager feedback;

    public LoreCommand(MessagesManager messages, FeedbackManager feedback) {
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
            deny(player);
            return true;
        }
        if (args.length == 0) {
            feedback.send(player, messages.component("lore.usage"), FeedbackPriority.HIGH, true, UiSoundMode.ERROR);
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            feedback.send(player, messages.component("lore.no-item"), FeedbackPriority.HIGH, true, UiSoundMode.ERROR);
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            feedback.send(player, messages.component("lore.no-item"), FeedbackPriority.HIGH, true, UiSoundMode.ERROR);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        return switch (action) {
            case "add" -> handleAdd(player, item, meta, args);
            case "remove" -> handleRemove(player, item, meta, args);
            case "set" -> handleSet(player, item, meta, args);
            default -> {
                feedback.send(player, messages.component("lore.usage"), FeedbackPriority.HIGH, true, UiSoundMode.ERROR);
                yield true;
            }
        };
    }

    private boolean handleAdd(Player player, ItemStack item, ItemMeta meta, String[] args) {
        if (args.length < 2) {
            feedback.send(player, messages.component("lore.usage"), FeedbackPriority.HIGH, true, UiSoundMode.ERROR);
            return true;
        }
        String raw = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Component line = ItemRichText.parse(raw);
        List<Component> lore = new ArrayList<>(meta.lore() != null ? meta.lore() : List.of());
        lore.add(line);
        meta.lore(lore);
        apply(player, item, meta);
        feedback.send(player, messages.component("lore.added"), FeedbackPriority.NORMAL, true, UiSoundMode.SUCCESS);
        return true;
    }

    private boolean handleRemove(Player player, ItemStack item, ItemMeta meta, String[] args) {
        if (args.length == 1) {
            meta.lore(null);
            apply(player, item, meta);
            feedback.send(player, messages.component("lore.cleared"), FeedbackPriority.NORMAL, true, UiSoundMode.SUCCESS);
            return true;
        }
        if (args.length != 2) {
            feedback.send(player, messages.component("lore.usage"), FeedbackPriority.HIGH, true, UiSoundMode.ERROR);
            return true;
        }
        Integer line = parseLine(args[1]);
        if (line == null) {
            feedback.send(
                    player,
                    messages.component("lore.invalid-line", "line", args[1]),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return true;
        }
        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) {
            feedback.send(
                    player,
                    messages.component("lore.line-out-of-range", "line", args[1]),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return true;
        }
        if (line < 1 || line > lore.size()) {
            feedback.send(
                    player,
                    messages.component("lore.line-out-of-range", "line", args[1]),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return true;
        }
        lore = new ArrayList<>(lore);
        lore.remove(line - 1);
        meta.lore(lore.isEmpty() ? null : lore);
        apply(player, item, meta);
        feedback.send(
                player,
                messages.component("lore.removed", "line", String.valueOf(line)),
                FeedbackPriority.NORMAL,
                true,
                UiSoundMode.SUCCESS);
        return true;
    }

    private boolean handleSet(Player player, ItemStack item, ItemMeta meta, String[] args) {
        if (args.length < 2) {
            feedback.send(player, messages.component("lore.usage"), FeedbackPriority.HIGH, true, UiSoundMode.ERROR);
            return true;
        }

        if (args.length >= 3 && isLineNumber(args[1])) {
            Integer line = parseLine(args[1]);
            if (line == null || line < 1) {
                feedback.send(
                        player,
                        messages.component("lore.invalid-line", "line", args[1]),
                        FeedbackPriority.HIGH,
                        true,
                        UiSoundMode.ERROR);
                return true;
            }
            String raw = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            Component component = ItemRichText.parse(raw);
            List<Component> lore = new ArrayList<>(meta.lore() != null ? meta.lore() : List.of());
            while (lore.size() < line) {
                lore.add(Component.empty());
            }
            lore.set(line - 1, component);
            meta.lore(lore);
            apply(player, item, meta);
            feedback.send(
                    player,
                    messages.component("lore.set-line", "line", String.valueOf(line)),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS);
            return true;
        }

        String raw = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        meta.lore(List.of(ItemRichText.parse(raw)));
        apply(player, item, meta);
        feedback.send(player, messages.component("lore.set"), FeedbackPriority.NORMAL, true, UiSoundMode.SUCCESS);
        return true;
    }

    private static void apply(Player player, ItemStack item, ItemMeta meta) {
        item.setItemMeta(meta);
        player.getInventory().setItemInMainHand(item);
    }

    private static boolean isLineNumber(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            if (!Character.isDigit(token.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable Integer parseLine(String token) {
        if (!isLineNumber(token)) {
            return null;
        }
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return List.of();
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            if (!(sender instanceof Player player)) {
                return List.of();
            }
            ItemStack item = player.getInventory().getItemInMainHand();
            ItemMeta meta = item.getItemMeta();
            if (meta == null || meta.lore() == null) {
                return List.of();
            }
            List<String> lines = new ArrayList<>();
            for (int i = 1; i <= meta.lore().size(); i++) {
                lines.add(String.valueOf(i));
            }
            String prefix = args[1];
            return lines.stream().filter(n -> n.startsWith(prefix)).toList();
        }
        return List.of();
    }

    private void deny(Player player) {
        feedback.send(
                player,
                messages.component("commands.no-permission"),
                FeedbackPriority.HIGH,
                true,
                UiSoundMode.ERROR);
    }
}
