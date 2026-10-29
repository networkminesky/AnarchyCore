package anarchy.minesky.core.api.playerconfig;

import anarchy.minesky.core.api.ServiceProviders;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Preferências do menu {@code /config} (MariaDB + cache por jogador).
 * <p>
 * Categorias e opções são registradas em {@link #registry()}. Valores refletem o que o jogador escolheu
 * no diálogo; não incluem regras extras (ex.: admin ignorando bloqueio de desconhecidos) —
 * use os métodos {@code canReceive*} para isso.
 */
public interface PlayerConfigAPI {

    static PlayerConfigAPI get() {
        return ServiceProviders.get(PlayerConfigAPI.class, "anarchy.minesky.core.api.playerconfig.PlayerConfigAPI");
    }

    PlayerConfigRegistry registry();

    /**
     * Leitura otimizada via handle registrado em {@link PlayerConfigRegistry}.
     */
    boolean getBoolean(PlayerConfigBooleanOption option, Player player);

    boolean getBoolean(PlayerConfigBooleanOption option, UUID playerId);

    boolean getBoolean(String key, Player player);

    boolean getBoolean(String key, UUID playerId);

    /**
     * Snapshot imutável de todas as opções booleanas registradas.
     */
    PlayerConfigValues values(Player player);

    PlayerConfigValues values(UUID playerId);

    /**
     * @deprecated use {@link #getBoolean(PlayerConfigBooleanOption, Player)} ou {@link PlayerConfigBooleanOption#get(Player)}
     */
    @Deprecated
    default boolean option(Player player, PlayerConfigOption option) {
        return option.read(this, player);
    }

    @Deprecated
    default boolean option(UUID playerId, PlayerConfigOption option) {
        return option.read(this, playerId);
    }

    boolean canReceiveTpaFrom(Player target, Player requester);

    boolean canReceiveTpaHereFrom(Player target, Player requester);

    boolean canReceiveTellFrom(Player target, Player sender);

    boolean canReceivePayFrom(Player target, Player sender);
}
