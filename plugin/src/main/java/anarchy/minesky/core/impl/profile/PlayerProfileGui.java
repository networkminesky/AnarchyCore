package anarchy.minesky.core.impl.profile;

import anarchy.minesky.core.utils.ItemRichText;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public final class PlayerProfileGui {

    private final Plugin plugin;

    public PlayerProfileGui(Plugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, PlayerProfileSnapshot profile, Runnable reopenSummary) {
        if (viewer == null || !viewer.isOnline() || profile == null) {
            return;
        }
        viewer.getScheduler().run(plugin, task -> {
            if (!viewer.isOnline()) {
                return;
            }
            viewer.closeDialog();
            PlayerProfileInventoryHolder holder = new PlayerProfileInventoryHolder(profile);
            Component title = ItemRichText.parse(PlayerProfileFormatter.chestTitle(profile.displayName()));
            Inventory inventory = Bukkit.createInventory(holder, PlayerProfileSlots.SIZE, title);
            holder.attach(inventory);
            ItemStackArray.apply(inventory, PlayerProfileGuiBuilder.buildContents(profile));
            viewer.openInventory(inventory);
        }, null);
    }

    public void handleBackClick(Player viewer, Runnable reopenSummary) {
        if (viewer == null) {
            return;
        }
        viewer.closeInventory();
        viewer.getScheduler().run(plugin, task -> {
            if (reopenSummary != null) {
                reopenSummary.run();
            }
        }, null);
    }

    /** Helper para aplicar array sem perder referência do inventário. */
    private static final class ItemStackArray {
        private ItemStackArray() {}

        static void apply(Inventory inventory, org.bukkit.inventory.ItemStack[] stacks) {
            for (int i = 0; i < stacks.length; i++) {
                inventory.setItem(i, stacks[i]);
            }
        }
    }
}