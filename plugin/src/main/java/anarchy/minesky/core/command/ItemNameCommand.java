package anarchy.minesky.core.command;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.utils.ItemRichText;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ItemNameCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "anarchycore.itemname";

    private final MessagesManager messages;
    private final FeedbackManager feedback;

    public ItemNameCommand(MessagesManager messages, FeedbackManager feedback) {
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
            feedback.send(player, messages.component("itemname.usage"), FeedbackPriority.HIGH, true, UiSoundMode.ERROR);
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            feedback.send(player, messages.component("itemname.no-item"), FeedbackPriority.HIGH, true, UiSoundMode.ERROR);
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            feedback.send(player, messages.component("itemname.no-item"), FeedbackPriority.HIGH, true, UiSoundMode.ERROR);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("clear")) {
            meta.itemName(null);
            item.setItemMeta(meta);
            player.getInventory().setItemInMainHand(item);
            feedback.send(
                    player,
                    messages.component("itemname.cleared"),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS);
            return true;
        }

        String raw = String.join(" ", args);
        meta.itemName(ItemRichText.parse(raw));
        item.setItemMeta(meta);
        player.getInventory().setItemInMainHand(item);
        feedback.send(
                player,
                messages.component("itemname.set"),
                FeedbackPriority.NORMAL,
                true,
                UiSoundMode.SUCCESS);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION) || args.length != 1) {
            return List.of();
        }
        if ("clear".startsWith(args[0].toLowerCase(Locale.ROOT))) {
            return List.of("clear");
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
