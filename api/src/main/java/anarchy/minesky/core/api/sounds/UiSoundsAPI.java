package anarchy.minesky.core.api.sounds;

import anarchy.minesky.core.api.ServiceProviders;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Sons de UI padronizados do AnarchyCore ({@code config.yml → sounds.*}).
 * <p>
 * Outros plugins devem preferir esta API em vez de sons hardcoded, para manter
 * a mesma experiência (erro / sucesso / tell) configurável no servidor.
 */
public interface UiSoundsAPI {

    String SERVICE_NAME = "anarchy.minesky.core.api.sounds.UiSoundsAPI";

    static UiSoundsAPI get() {
        return ServiceProviders.get(UiSoundsAPI.class, SERVICE_NAME);
    }

    static void error(Player player) {
        UiSoundsAPI api = get();
        if (api != null) {
            api.playError(player);
        }
    }

    static void success(Player player) {
        UiSoundsAPI api = get();
        if (api != null) {
            api.playSuccess(player);
        }
    }

    /**
     * Sino de tell; não toca se {@link #isTellEnabled()} for {@code false}.
     */
    static void tell(Player player) {
        UiSoundsAPI api = get();
        if (api != null) {
            api.playTell(player);
        }
    }

    static void playMode(Player player, UiSoundMode mode) {
        UiSoundsAPI api = get();
        if (api != null) {
            api.play(player, mode);
        }
    }

    void playError(Player player);

    void playSuccess(Player player);

    /**
     * Respeita {@link #isTellEnabled()}.
     */
    void playTell(Player player);

    /**
     * Reproduz o som associado ao modo ({@link UiSoundMode#NONE} e {@link UiSoundMode#AUTO} ignorados).
     */
    void play(Player player, UiSoundMode mode);

    boolean isTellEnabled();

    float volume();

    float pitch();

    Sound errorSound();

    Sound successSound();

    Sound tellSound();
}
