package anarchy.minesky.core.api.dialogapi;

import org.bukkit.inventory.ItemStack;

public record DialogBodyItem(
        ItemStack item,
        int width,
        int height,
        boolean showDecorations,
        boolean showTooltip) {

    public static DialogBodyItem icon(ItemStack item) {
        return new DialogBodyItem(item, 16, 16, false, false);
    }

    public static DialogBodyItem full(ItemStack item) {
        return new DialogBodyItem(item, 64, 64, true, true);
    }
}
