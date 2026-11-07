package anarchy.minesky.core.impl.kits;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class KitInventoryHolder implements InventoryHolder {

    public enum MenuType {
        ADMIN_CONTENT,
        PLAYER_PREVIEW
    }

    private final MenuType menuType;
    private final String kitId;
    private Inventory inventory;

    public KitInventoryHolder(MenuType menuType, String kitId) {
        this.menuType = menuType;
        this.kitId = kitId;
    }

    public MenuType menuType() {
        return menuType;
    }

    public String kitId() {
        return kitId;
    }

    void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
