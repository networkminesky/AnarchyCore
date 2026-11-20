package anarchy.minesky.core.impl.blocks;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public final class BlockSettings {

    private final Plugin plugin;

    private long defaultCooldownMs = 1000L;
    private int particlePeriodTicks = 10;
    private double particleViewDistance = 32.0;

    public BlockSettings(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        defaultCooldownMs = Math.max(0L, config.getLong("blocks.default-cooldown-ms", 1000L));
        particlePeriodTicks = Math.max(5, config.getInt("blocks.particle-period-ticks", 10));
        particleViewDistance = Math.max(8.0, config.getDouble("blocks.particle-view-distance", 32.0));
    }

    public long defaultCooldownMs() {
        return defaultCooldownMs;
    }

    public int particlePeriodTicks() {
        return particlePeriodTicks;
    }

    public double particleViewDistance() {
        return particleViewDistance;
    }
}