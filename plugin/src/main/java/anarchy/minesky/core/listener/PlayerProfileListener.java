package anarchy.minesky.core.listener;

import anarchy.minesky.core.impl.profile.PlayerProfileFlow;
import anarchy.minesky.core.impl.profile.PlayerProfileInventoryHolder;
import anarchy.minesky.core.impl.profile.PlayerProfileSlots;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public final class PlayerProfileListener implements Listener {

    private final PlayerProfileFlow flow;

    public PlayerProfileListener(PlayerProfileFlow flow) {
        this.flow = flow;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof PlayerProfileInventoryHolder profileHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() != PlayerProfileSlots.StatCell.BACK_BUTTON.slot()) {
            return;
        }
        flow.onChestBack(player, profileHolder.profile());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof PlayerProfileInventoryHolder) {
            event.setCancelled(true);
        }
    }
}