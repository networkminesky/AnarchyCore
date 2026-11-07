package anarchy.minesky.core.impl.kits;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Simula {@code PlayerInventory#addItem} para saber quantos slots a mais seriam necessários.
 */
public final class KitInventorySpace {

    private static final int STORAGE_SLOTS = 36;

    private KitInventorySpace() {}

    public record SpaceCheck(boolean fits, int slotsShort) {}

    public static SpaceCheck check(Player player, ItemStack[] kitContents) {
        List<ItemStack> slots = storageSnapshot(player);

        for (ItemStack kitStack : kitContents) {
            if (kitStack == null || kitStack.getType().isAir()) {
                continue;
            }
            ItemStack remaining = kitStack.clone();
            mergeInto(slots, remaining);
            while (remaining.getAmount() > 0) {
                int chunk = Math.min(remaining.getAmount(), remaining.getMaxStackSize());
                ItemStack placed = remaining.clone();
                placed.setAmount(chunk);
                slots.add(placed);
                remaining.setAmount(remaining.getAmount() - chunk);
            }
        }

        if (slots.size() <= STORAGE_SLOTS) {
            return new SpaceCheck(true, 0);
        }
        return new SpaceCheck(false, slots.size() - STORAGE_SLOTS);
    }

    private static List<ItemStack> storageSnapshot(Player player) {
        List<ItemStack> slots = new ArrayList<>();
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            slots.add(stack.clone());
        }
        return slots;
    }

    private static void mergeInto(List<ItemStack> slots, ItemStack remaining) {
        for (ItemStack slot : slots) {
            if (remaining.getAmount() <= 0) {
                return;
            }
            if (!slot.isSimilar(remaining)) {
                continue;
            }
            int max = slot.getMaxStackSize();
            int space = max - slot.getAmount();
            if (space <= 0) {
                continue;
            }
            int move = Math.min(space, remaining.getAmount());
            slot.setAmount(slot.getAmount() + move);
            remaining.setAmount(remaining.getAmount() - move);
        }
    }
}
