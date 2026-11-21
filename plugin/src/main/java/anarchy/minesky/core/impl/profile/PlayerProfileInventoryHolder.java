package anarchy.minesky.core.impl.profile;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class PlayerProfileInventoryHolder implements InventoryHolder {

    private final PlayerProfileSnapshot profile;
    private Inventory inventory;

    public PlayerProfileInventoryHolder(PlayerProfileSnapshot profile) {
        this.profile = profile;
    }

    public PlayerProfileSnapshot profile() {
        return profile;
    }

    void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}