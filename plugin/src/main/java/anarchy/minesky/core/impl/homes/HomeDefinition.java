package anarchy.minesky.core.impl.homes;

import anarchy.minesky.core.api.text.DialogComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.UUID;

public final class HomeDefinition {

    private final UUID ownerUuid;
    private final String id;
    private final String displayName;
    private final Material listIcon;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public HomeDefinition(
            UUID ownerUuid,
            String id,
            String displayName,
            Material listIcon,
            Location location) {
        this.ownerUuid = ownerUuid;
        this.id = id;
        this.displayName = displayName;
        this.listIcon = listIcon;
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    public HomeDefinition(
            UUID ownerUuid,
            String id,
            String displayName,
            Material listIcon,
            String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch) {
        this.ownerUuid = ownerUuid;
        this.id = id;
        this.displayName = displayName;
        this.listIcon = listIcon;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public UUID ownerUuid() {
        return ownerUuid;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
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

    public Material listIcon() {
        return listIcon;
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    public Component menuButtonLabel() {
        return DialogComponentUtils.materialIconComponent(listIcon)
                .append(Component.space())
                .append(DialogComponentUtils.parse(displayName));
    }

    public HomeDefinition withDisplayName(String newDisplay) {
        return new HomeDefinition(ownerUuid, id, newDisplay, listIcon, worldName, x, y, z, yaw, pitch);
    }

    public HomeDefinition withIcon(Material newIcon) {
        return new HomeDefinition(ownerUuid, id, displayName, newIcon, worldName, x, y, z, yaw, pitch);
    }

    public HomeDefinition atLocation(Location location) {
        return new HomeDefinition(ownerUuid, id, displayName, listIcon, location);
    }
}
