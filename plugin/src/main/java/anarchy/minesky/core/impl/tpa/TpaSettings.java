package anarchy.minesky.core.impl.tpa;

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

    private Sound requestSend = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
    private Sound requestReceive = Sound.BLOCK_NOTE_BLOCK_PLING;
    private Sound accept = Sound.ENTITY_PLAYER_LEVELUP;
    private Sound deny = Sound.ENTITY_VILLAGER_NO;
    private Sound countdownTick = Sound.BLOCK_NOTE_BLOCK_HAT;
    private Sound teleport = Sound.ENTITY_ENDERMAN_TELEPORT;
    private Sound cancel = Sound.ENTITY_ITEM_BREAK;

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

        requestSend = TpaSounds.parseSound(config.getString("tpa.sounds.request-send"), requestSend);
        requestReceive = TpaSounds.parseSound(config.getString("tpa.sounds.request-receive"), requestReceive);
        accept = TpaSounds.parseSound(config.getString("tpa.sounds.accept"), accept);
        deny = TpaSounds.parseSound(config.getString("tpa.sounds.deny"), deny);
        countdownTick = TpaSounds.parseSound(config.getString("tpa.sounds.countdown-tick"), countdownTick);
        teleport = TpaSounds.parseSound(config.getString("tpa.sounds.teleport"), teleport);
        cancel = TpaSounds.parseSound(config.getString("tpa.sounds.cancel"), cancel);
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

    public Sound requestSend() {
        return requestSend;
    }

    public Sound requestReceive() {
        return requestReceive;
    }

    public Sound accept() {
        return accept;
    }

    public Sound deny() {
        return deny;
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
}
