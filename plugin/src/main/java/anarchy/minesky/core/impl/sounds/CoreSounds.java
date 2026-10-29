package anarchy.minesky.core.impl.sounds;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class CoreSounds {

    private CoreSounds() {}

    public static void play(Player player, Sound sound, float volume, float pitch) {
        if (player == null || !player.isOnline() || sound == null) {
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static void playError(Player player, GlobalSoundSettings settings) {
        CoreSounds.play(player, settings.error(), settings.volume(), settings.pitch());
    }

    public static void playSuccess(Player player, GlobalSoundSettings settings) {
        CoreSounds.play(player, settings.success(), settings.volume(), settings.pitch());
    }

    public static void playTell(Player player, GlobalSoundSettings settings) {
        CoreSounds.play(player, settings.tell(), settings.volume(), settings.pitch() + 0.1f);
    }

    public static Sound parseSound(String name, Sound fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        try {
            return Sound.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
