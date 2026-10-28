package anarchy.minesky.core.api.playerconfig;

import anarchy.minesky.core.api.ServiceProviders;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Preferências do menu {@code /config} (MariaDB + cache por jogador).
 * <p>
 * Valores refletem o que o jogador escolheu no diálogo; não incluem regras extras
 * (ex.: admin ignorando bloqueio de desconhecidos) — use os métodos {@code canReceive*} para isso.
 */
public interface PlayerConfigAPI {

    static PlayerConfigAPI get() {
        return ServiceProviders.get(PlayerConfigAPI.class, "anarchy.minesky.core.api.playerconfig.PlayerConfigAPI");
    }

    /**
     * Todas as opções booleanas do {@code /config} para o jogador.
     */
    PlayerConfigValues values(Player player);

    PlayerConfigValues values(UUID playerId);

    /**
     * Uma opção específica ({@link PlayerConfigOption}).
     */
    default boolean option(Player player, PlayerConfigOption option) {
        return option.read(values(player));
    }

    default boolean option(UUID playerId, PlayerConfigOption option) {
        return option.read(values(playerId));
    }

    /**
     * {@code true} se o alvo aceita TPA do remetente (conhecidos sempre; desconhecidos depende da pref).
     */
    boolean canReceiveTpaFrom(Player target, Player requester);

    boolean canReceiveTpaHereFrom(Player target, Player requester);

    boolean canReceiveTellFrom(Player target, Player sender);

    boolean canReceivePayFrom(Player target, Player sender);
}
