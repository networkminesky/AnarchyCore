package anarchy.minesky.core.impl.warps;

import anarchy.minesky.core.api.text.DialogComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class WarpDefinition {

    private final String id;
    private final String displayName;
    private final Material iconMaterial;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public WarpDefinition(
            String id,
            String displayName,
            Material iconMaterial,
            Location location) {
        this.id = id;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    public WarpDefinition(
            String id,
            String displayName,
            Material iconMaterial,
            String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch) {
        this.id = id;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Material iconMaterial() {
        return iconMaterial;
    }

    public String worldName() {
        return worldName;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public String permissionNode() {
        return "anarchycore.warps." + id;
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    public ItemStack iconStack() {
        ItemStack stack = new ItemStack(iconMaterial);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(DialogComponentUtils.parse(displayName));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public Component materialIconComponent() {
        return DialogComponentUtils.materialIconComponent(iconMaterial);
    }

    /**
     * Rótulo de botão no dialog Java (Paper): sprite do item + nome público numa única ação.
     */
    public Component menuButtonLabel() {
        Component icon = materialIconComponent();
        Component name = DialogComponentUtils.parse(displayName);
        return icon.append(Component.space()).append(name);
    }

    public WarpDefinition withDisplay(String displayName, Material iconMaterial) {
        return new WarpDefinition(id, displayName, iconMaterial, worldName, x, y, z, yaw, pitch);
    }
}
