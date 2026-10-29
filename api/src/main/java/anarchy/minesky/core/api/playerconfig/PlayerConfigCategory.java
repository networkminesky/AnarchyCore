package anarchy.minesky.core.api.playerconfig;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Categoria do menu {@code /config} (botão na tela principal + diálogo de opções).
 */
public interface PlayerConfigCategory {

    String id();

    Component title();

    Component buttonLabel();

    Material iconMaterial();

    int sortOrder();

    Plugin owner();

    List<PlayerConfigBooleanOption> booleanOptions();
}
