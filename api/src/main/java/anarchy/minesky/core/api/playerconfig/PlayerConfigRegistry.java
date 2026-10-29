package anarchy.minesky.core.api.playerconfig;

import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Optional;

/**
 * Registro dinâmico de categorias e opções do {@code /config}.
 * Plugins devem registrar durante {@code onEnable}; o AnarchyCore usa a mesma API para as prefs padrão.
 */
public interface PlayerConfigRegistry {

    PlayerConfigCategory registerCategory(Plugin owner, PlayerConfigCategorySpec spec);

    PlayerConfigBooleanOption registerBooleanOption(Plugin owner, PlayerConfigBooleanOptionSpec spec);

    Collection<PlayerConfigCategory> categories();

    Optional<PlayerConfigCategory> category(String id);

    Optional<PlayerConfigBooleanOption> booleanOption(String key);
}
