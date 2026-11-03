package anarchy.minesky.core.api.dialogapi;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * Diálogo reutilizável para buscar e escolher um {@link Material} (sprite de item/bloco + nome).
 */
public final class MaterialPickerDialogs {

    private MaterialPickerDialogs() {
    }

    public static void open(Player player, String title, Consumer<Material> onSelected) {
        DialogAPI api = DialogAPI.get();
        if (api != null && onSelected != null) {
            api.openMaterialPicker(player, title, onSelected);
        }
    }

    public static void open(Player player, net.kyori.adventure.text.Component title, Consumer<Material> onSelected) {
        open(player, title, null, onSelected);
    }

    /**
     * @param initialQuery texto inicial do campo de busca; se não vazio, abre direto nos resultados da pesquisa
     */
    public static void open(Player player, String title, String initialQuery, Consumer<Material> onSelected) {
        DialogAPI api = DialogAPI.get();
        if (api != null && onSelected != null) {
            api.openMaterialPicker(player, title, initialQuery, onSelected);
        }
    }

    public static void open(Player player, net.kyori.adventure.text.Component title, String initialQuery,
                            Consumer<Material> onSelected) {
        DialogAPI api = DialogAPI.get();
        if (api != null && onSelected != null) {
            api.openMaterialPicker(player, title, initialQuery, onSelected);
        }
    }
}
