package anarchy.minesky.core.impl.guard.fog;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Ponte opcional com o AnarchyTags. O AnarchyTags depende do AnarchyCore e
 * habilita depois; a ligação acontece no PluginEnableEvent, pela classloader dele.
 */
interface FogNametagIntegration {

    FogNametagIntegration NONE = new FogNametagIntegration() {};

    default void start() {}

    default void stop() {}

    default void onVisibilityChanged(Player target) {}

    static FogNametagIntegration open(Plugin plugin, FogOfWarManager fog) {
        Plugin tags = Bukkit.getPluginManager().getPlugin("AnarchyTags");
        if (tags == null || !tags.isEnabled()) {
            return NONE;
        }
        try {
            return AnarchyTagsFogHook.attach(plugin, fog, tags);
        } catch (Throwable error) {
            plugin.getLogger().warning("[AnarchyGuard] Falha ao ligar o fog of war no AnarchyTags: " + error.getMessage());
            return NONE;
        }
    }
}
