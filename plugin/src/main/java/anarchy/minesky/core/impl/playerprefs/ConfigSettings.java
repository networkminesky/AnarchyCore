package anarchy.minesky.core.impl.playerprefs;

import anarchy.minesky.core.impl.tpa.TpaSounds;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public final class ConfigSettings {

    private float volume = 0.7f;
    private float pitch = 1.0f;
    private Sound toggleOn = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
    private Sound toggleOff = Sound.BLOCK_NOTE_BLOCK_BASS;
    private Sound tellNotify = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;

    public ConfigSettings(Plugin plugin) {
        reload(plugin);
    }

    public void reload(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();
        volume = (float) config.getDouble("player-config.sounds.volume", 0.7);
        pitch = (float) config.getDouble("player-config.sounds.pitch", 1.0);
        toggleOn = TpaSounds.parseSound(config.getString("player-config.sounds.toggle-on"), toggleOn);
        toggleOff = TpaSounds.parseSound(config.getString("player-config.sounds.toggle-off"), toggleOff);
        tellNotify = TpaSounds.parseSound(config.getString("player-config.sounds.tell-notify"), tellNotify);
    }

    public float volume() {
        return volume;
    }

    public float pitch() {
        return pitch;
    }

    public Sound toggleOn() {
        return toggleOn;
    }

    public Sound toggleOff() {
        return toggleOff;
    }

    public Sound tellNotify() {
        return tellNotify;
    }
}
