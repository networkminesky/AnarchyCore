package anarchy.minesky.core.impl.warps;

import anarchy.minesky.core.impl.tpa.TpaSounds;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public final class WarpSettings {

    public static final String BYPASS_TIMER_PERMISSION = "anarchycore.warp.bypasstimer";
    public static final String ADMIN_PERMISSION = "anarchycore.warp.admin";

    private final Plugin plugin;

    private int countdownSeconds = 5;
    private double moveThreshold = 0.5;
    private float soundVolume = 0.8f;
    private float soundPitch = 1.0f;

    private Sound countdownTick = Sound.BLOCK_NOTE_BLOCK_HAT;
    private Sound teleport = Sound.ENTITY_ENDERMAN_TELEPORT;
    private Sound cancel = Sound.ENTITY_ITEM_BREAK;
    private Material listHeaderIcon = Material.RED_BED;

    public WarpSettings(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        countdownSeconds = Math.max(0, config.getInt("warps.countdown-seconds", 5));
        moveThreshold = Math.max(0.01, config.getDouble("warps.move-threshold", 0.5));
        soundVolume = (float) config.getDouble("warps.sounds.volume", 0.8);
        soundPitch = (float) config.getDouble("warps.sounds.pitch", 1.0);

        countdownTick = TpaSounds.parseSound(config.getString("warps.sounds.countdown-tick"), countdownTick);
        teleport = TpaSounds.parseSound(config.getString("warps.sounds.teleport"), teleport);
        cancel = TpaSounds.parseSound(config.getString("warps.sounds.cancel"), cancel);
        listHeaderIcon = WarpStorage.parseMaterial(config.getString("warps.list-header-icon"), Material.RED_BED);
    }

    public int countdownSeconds() {
        return countdownSeconds;
    }

    public double moveThreshold() {
        return moveThreshold;
    }

    public float soundVolume() {
        return soundVolume;
    }

    public float soundPitch() {
        return soundPitch;
    }

    public Sound countdownTick() {
        return countdownTick;
    }

    public Sound teleport() {
        return teleport;
    }

    public Sound cancel() {
        return cancel;
    }

    public Material listHeaderIcon() {
        return listHeaderIcon;
    }
}
