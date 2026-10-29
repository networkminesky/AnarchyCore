package anarchy.minesky.core.impl.tpa;

import anarchy.minesky.core.impl.sounds.CoreSounds;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** @deprecated use {@link CoreSounds} */
@Deprecated
public final class TpaSounds {

    private TpaSounds() {}

    public static void play(Player player, Sound sound, float volume, float pitch) {
        CoreSounds.play(player, sound, volume, pitch);
    }

    public static Sound parseSound(String name, Sound fallback) {
        return CoreSounds.parseSound(name, fallback);
    }
}
