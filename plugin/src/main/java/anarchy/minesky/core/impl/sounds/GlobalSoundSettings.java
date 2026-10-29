package anarchy.minesky.core.impl.sounds;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public final class GlobalSoundSettings {

    private float volume = 0.8f;
    private float pitch = 1.0f;
    private Sound error = Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO;
    private Sound success = Sound.BLOCK_NOTE_BLOCK_CHIME;
    private Sound tell = Sound.BLOCK_NOTE_BLOCK_BELL;
    private boolean tellEnabled = true;

    public GlobalSoundSettings(Plugin plugin) {
        reload(plugin);
    }

    public void reload(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();
        volume = (float) config.getDouble("sounds.volume", 0.8);
        pitch = (float) config.getDouble("sounds.pitch", 1.0);
        error = CoreSounds.parseSound(config.getString("sounds.error"), error);
        success = CoreSounds.parseSound(config.getString("sounds.success"), success);
        tell = CoreSounds.parseSound(config.getString("sounds.tell.sound"), tell);
        tellEnabled = config.getBoolean("sounds.tell.enabled", true);
    }

    public float volume() {
        return volume;
    }

    public float pitch() {
        return pitch;
    }

    public Sound error() {
        return error;
    }

    public Sound success() {
        return success;
    }

    public Sound tell() {
        return tell;
    }

    public boolean tellEnabled() {
        return tellEnabled;
    }
}
