package anarchy.minesky.core.api.feedback;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * API de mensagens de feedback no estilo action bar (DonutSMP-like).
 * Textos longos são divididos e exibidos em sequência no action bar do jogador.
 */
public interface FeedbackAPI {

    static FeedbackAPI get() {
        RegisteredServiceProvider<FeedbackAPI> provider =
                Bukkit.getServicesManager().getRegistration(FeedbackAPI.class);
        return provider != null ? provider.getProvider() : null;
    }

    static void display(Player player, String message) {
        display(player, message, FeedbackPriority.NORMAL);
    }

    static void display(Player player, String message, FeedbackPriority priority) {
        FeedbackAPI api = get();
        if (api != null) {
            api.send(player, message, priority);
        }
    }

    static void display(Player player, Component message) {
        display(player, message, FeedbackPriority.NORMAL);
    }

    static void display(Player player, Component message, FeedbackPriority priority) {
        FeedbackAPI api = get();
        if (api != null) {
            api.send(player, message, priority);
        }
    }

    void send(Player player, String message);

    void send(Player player, String message, FeedbackPriority priority);

    void send(Player player, Component message);

    void send(Player player, Component message, FeedbackPriority priority);

    /**
     * Limpa a fila do jogador e remove o texto do action bar.
     */
    void clear(Player player);

    /**
     * {@code true} se ainda há partes ou mensagens pendentes para o jogador.
     */
    boolean isBusy(Player player);
}
