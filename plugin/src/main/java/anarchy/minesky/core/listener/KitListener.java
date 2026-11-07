package anarchy.minesky.core.listener;

import anarchy.minesky.core.impl.kits.KitInventoryHolder;
import anarchy.minesky.core.impl.kits.KitManager;
import anarchy.minesky.core.impl.kits.KitUiSlots;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;

public final class KitListener implements Listener {

    private final KitManager kitManager;

    public KitListener(KitManager kitManager) {
        this.kitManager = kitManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        InventoryHolder topHolder = event.getView().getTopInventory().getHolder();
        if (!(topHolder instanceof KitInventoryHolder holder)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        boolean topInventory = rawSlot >= 0 && rawSlot < KitUiSlots.INVENTORY_SIZE;

        if (holder.menuType() == KitInventoryHolder.MenuType.PLAYER_PREVIEW) {
            event.setCancelled(true);
            if (!topInventory) {
                return;
            }
            if (rawSlot == KitUiSlots.ACTION_PRIMARY) {
                kitManager.handlePreviewClaimClick(player, holder.kitId());
            }
            return;
        }

        if (holder.menuType() == KitInventoryHolder.MenuType.ADMIN_CONTENT) {
            if (!topInventory) {
                return;
            }
            if (rawSlot >= KitUiSlots.CONTENT_SLOTS) {
                event.setCancelled(true);
                kitManager.handleAdminControlClick(player, holder.kitId(), rawSlot);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof KitInventoryHolder kitHolder)) {
            return;
        }
        if (kitHolder.menuType() == KitInventoryHolder.MenuType.PLAYER_PREVIEW) {
            event.setCancelled(true);
            return;
        }
        for (int slot : event.getRawSlots()) {
            if (slot >= KitUiSlots.CONTENT_SLOTS && slot < KitUiSlots.INVENTORY_SIZE) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof KitInventoryHolder kitHolder)) {
            return;
        }
        kitManager.handleMenuClose(player, kitHolder);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        kitManager.preloadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        kitManager.handleQuit(event.getPlayer().getUniqueId());
    }
}
