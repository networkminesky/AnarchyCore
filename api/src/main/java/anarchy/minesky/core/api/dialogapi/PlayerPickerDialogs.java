package anarchy.minesky.core.api.dialogapi;

import org.bukkit.entity.Player;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Diálogo para buscar e escolher um jogador online (nick).
 */
public final class PlayerPickerDialogs {

    private PlayerPickerDialogs() {
    }

    public static void open(Player player, String title, Predicate<Player> visible, Consumer<String> onNickSelected) {
        open(player, title, visible, null, onNickSelected);
    }

    public static void open(
            Player player,
            String title,
            Predicate<Player> visible,
            String initialQuery,
            Consumer<String> onNickSelected) {
        DialogAPI api = DialogAPI.get();
        if (api != null && onNickSelected != null) {
            api.openPlayerPicker(player, title, visible, initialQuery, onNickSelected);
        }
    }

    public static void open(Player player, net.kyori.adventure.text.Component title, Predicate<Player> visible,
                            Consumer<String> onNickSelected) {
        open(player, title, visible, null, onNickSelected);
    }

    public static void open(
            Player player,
            net.kyori.adventure.text.Component title,
            Predicate<Player> visible,
            String initialQuery,
            Consumer<String> onNickSelected) {
        DialogAPI api = DialogAPI.get();
        if (api != null && onNickSelected != null) {
            api.openPlayerPicker(player, title, visible, initialQuery, onNickSelected);
        }
    }
}
