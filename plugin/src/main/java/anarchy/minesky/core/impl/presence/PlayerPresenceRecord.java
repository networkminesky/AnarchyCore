package anarchy.minesky.core.impl.presence;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record PlayerPresenceRecord(
        UUID uuid,
        String playerName,
        @Nullable Long lastJoinAtMillis,
        @Nullable Long lastQuitAtMillis,
        @Nullable String world,
        @Nullable Double x,
        @Nullable Double y,
        @Nullable Double z,
        @Nullable Float yaw,
        @Nullable Float pitch) {

    public boolean hasLogoutLocation() {
        return world != null && !world.isBlank() && x != null && y != null && z != null;
    }

    public @Nullable Location toLogoutLocation() {
        if (!hasLogoutLocation()) {
            return null;
        }
        World loaded = Bukkit.getWorld(world);
        if (loaded == null) {
            return null;
        }
        float safeYaw = yaw != null ? yaw : 0f;
        float safePitch = pitch != null ? pitch : 0f;
        return new Location(loaded, x, y, z, safeYaw, safePitch);
    }
}