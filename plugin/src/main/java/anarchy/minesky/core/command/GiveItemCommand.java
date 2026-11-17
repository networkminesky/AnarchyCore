package anarchy.minesky.core.command;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * {@code /i <item> [quantidade]}. Prefixo único resolve sozinho ({@code pap} → paper),
 * maiúsculas e underscores são ignorados. Ambíguo não dá item nenhum.
 */
public final class GiveItemCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "anarchycore.give";

    /** Um baú cheio de stack 64. Acima disso vira abuso, não give. */
    static final int MAX_STACKABLE = 36 * 64;
    /** Entidades de item soltas demais derrubam o tick; unstackable fica nesse teto. */
    static final int MAX_UNSTACKABLE = 64;

    private static final List<String> AMOUNTS = List.of("1", "16", "32", "64");

    private final MessagesManager messages;
    private final FeedbackManager feedback;

    public GiveItemCommand(MessagesManager messages, FeedbackManager feedback) {
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
        if (args.length == 0 || args.length > 2) {
            feedback.send(
                    player,
                    messages.component("give.usage"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return true;
        }

        ItemMaterials.Match match = ItemMaterials.resolve(args[0]);
        if (match.status() == ItemMaterials.Status.MISSING) {
            feedback.send(
                    player,
                    messages.component("give.unknown", "item", args[0]),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return true;
        }
        if (match.status() == ItemMaterials.Status.AMBIGUOUS) {
            feedback.send(
                    player,
                    messages.component("give.ambiguous", "item", args[0]),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return true;
        }

        int max = match.material().getMaxStackSize() > 1 ? MAX_STACKABLE : MAX_UNSTACKABLE;
        int amount = 1;
        if (args.length == 2) {
            amount = parseAmount(args[1], max);
            if (amount < 1) {
                feedback.send(
                        player,
                        messages.component("give.invalid-amount", "max", String.valueOf(max)),
                        FeedbackPriority.HIGH,
                        true,
                        UiSoundMode.ERROR);
                return true;
            }
        }

        int dropped = give(player, match.material(), amount);
        String display = ItemMaterials.displayName(match.material());
        feedback.send(
                player,
                messages.component("give.success", "amount", String.valueOf(amount), "item", display),
                FeedbackPriority.NORMAL,
                true,
                UiSoundMode.SUCCESS);
        if (dropped > 0) {
            feedback.send(
                    player,
                    messages.component("give.dropped", "amount", String.valueOf(dropped)),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.NONE);
        }
        return true;
    }

    /**
     * Entrega em stacks do tamanho do material. O que não cabe no inventário cai no chão.
     *
     * @return quantidade que caiu no chão
     */
    private static int give(Player player, Material material, int amount) {
        int stackSize = Math.max(1, material.getMaxStackSize());
        int dropped = 0;
        int remaining = amount;
        while (remaining > 0) {
            int size = Math.min(stackSize, remaining);
            remaining -= size;
            ItemStack stack = new ItemStack(material, size);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            for (ItemStack extra : leftover.values()) {
                dropped += extra.getAmount();
                player.getWorld().dropItemNaturally(player.getLocation(), extra);
            }
        }
        return dropped;
    }

    private static int parseAmount(String raw, int max) {
        try {
            int value = Integer.parseInt(raw);
            if (value < 1 || value > max) {
                return -1;
            }
            return value;
        } catch (NumberFormatException ignored) {
            return -1;
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
            return ItemMaterials.complete(args[0]);
        }
        if (args.length == 2 && ItemMaterials.resolve(args[0]).status() == ItemMaterials.Status.FOUND) {
            String prefix = args[1];
            return AMOUNTS.stream().filter(amount -> amount.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
