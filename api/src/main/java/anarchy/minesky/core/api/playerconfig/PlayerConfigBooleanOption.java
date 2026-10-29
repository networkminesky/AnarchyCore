package anarchy.minesky.core.api.playerconfig;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Handle de uma opção booleana registrada. Use {@link #get(Player)} / {@link #get(UUID)} após o registro.
 */
public interface PlayerConfigBooleanOption {

    String key();

    Component label();

    boolean defaultValue();

    PlayerConfigCategory category();

    PlayerConfigOptionType type();

    boolean get(Player player);

    boolean get(UUID playerId);
}
