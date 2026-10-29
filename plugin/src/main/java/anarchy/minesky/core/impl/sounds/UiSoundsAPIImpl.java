package anarchy.minesky.core.impl.sounds;

import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.api.sounds.UiSoundsAPI;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

public final class UiSoundsAPIImpl implements UiSoundsAPI {

    private final Plugin plugin;
    private final GlobalSoundSettings settings;

    public UiSoundsAPIImpl(Plugin plugin, GlobalSoundSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void register() {
        plugin.getServer().getServicesManager().register(UiSoundsAPI.class, this, plugin, ServicePriority.Normal);
    }

    public void unregister() {
        plugin.getServer().getServicesManager().unregister(UiSoundsAPI.class, this);
    }

    @Override
    public void playError(Player player) {
        CoreSounds.playError(player, settings);
    }

    @Override
    public void playSuccess(Player player) {
        CoreSounds.playSuccess(player, settings);
    }

    @Override
    public void playTell(Player player) {
        if (!settings.tellEnabled()) {
            return;
        }
        CoreSounds.playTell(player, settings);
    }

    @Override
    public void play(Player player, UiSoundMode mode) {
        if (player == null || mode == null || mode == UiSoundMode.NONE || mode == UiSoundMode.AUTO) {
            return;
        }
        switch (mode) {
            case ERROR -> playError(player);
            case SUCCESS -> playSuccess(player);
            case TELL -> playTell(player);
            default -> { }
        }
    }

    @Override
    public boolean isTellEnabled() {
        return settings.tellEnabled();
    }

    @Override
    public float volume() {
        return settings.volume();
    }

    @Override
    public float pitch() {
        return settings.pitch();
    }

    @Override
    public Sound errorSound() {
        return settings.error();
    }

    @Override
    public Sound successSound() {
        return settings.success();
    }

    @Override
    public Sound tellSound() {
        return settings.tell();
    }
}
