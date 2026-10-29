package anarchy.minesky.core.impl.tpa;

import anarchy.minesky.core.impl.sounds.CoreSounds;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public final class TpaSettings {

    public static final String BYPASS_TIMER_PERMISSION = "anarchycore.tpa.bypasstimer";

    private final Plugin plugin;

    private int countdownSeconds = 5;
    private double moveThreshold = 0.5;
    private int requestExpireSeconds = 120;
    private float soundVolume = 0.8f;
    private float soundPitch = 1.0f;
    private Sound countdownTick = Sound.BLOCK_NOTE_BLOCK_HAT;

    public TpaSettings(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        countdownSeconds = Math.max(0, config.getInt("tpa.countdown-seconds", 5));
        moveThreshold = Math.max(0.01, config.getDouble("tpa.move-threshold", 0.5));
        requestExpireSeconds = Math.max(30, config.getInt("tpa.request-expire-seconds", 120));
        soundVolume = (float) config.getDouble("tpa.sounds.volume", 0.8);
        soundPitch = (float) config.getDouble("tpa.sounds.pitch", 1.0);
        countdownTick = CoreSounds.parseSound(config.getString("tpa.sounds.countdown-tick"), countdownTick);
    }

    public int countdownSeconds() {
        return countdownSeconds;
    }

    public double moveThreshold() {
        return moveThreshold;
    }

    public int requestExpireSeconds() {
        return requestExpireSeconds;
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
}
