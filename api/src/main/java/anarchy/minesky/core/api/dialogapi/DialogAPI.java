package anarchy.minesky.core.api.dialogapi;

import anarchy.minesky.core.api.ServiceProviders;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface DialogAPI {

    static DialogAPI get() {
        return ServiceProviders.get(DialogAPI.class, "anarchy.minesky.core.api.dialogapi.DialogAPI");
    }

    /**
     * Exibe o diálogo para o jogador (detecta automaticamente Bedrock ou Java).
     */
    void show(Player player, MineSkyDialog dialog);

    /**
     * Atalho estático para exibir.
     */
    static void display(Player player, MineSkyDialog dialog) {
        DialogAPI api = get();
        if (api != null) {
            api.show(player, dialog);
        }
    }

    /**
     * Cria um novo builder de diálogo.
     */
    MineSkyDialog.Builder createDialog();

    /**
     * Cria um builder já com título em Component.
     */
    MineSkyDialog.Builder createDialog(Component title);

    /**
     * Cria um builder já com título em String (suporta MiniMessage e &).
     */
    MineSkyDialog.Builder createDialog(String title);

    /**
     * Checa se o jogador está conectado via Bedrock (Floodgate).
     */
    boolean isBedrock(Player player);
}