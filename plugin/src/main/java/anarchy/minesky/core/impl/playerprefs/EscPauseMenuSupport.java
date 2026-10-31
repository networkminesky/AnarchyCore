package anarchy.minesky.core.impl.playerprefs;

import anarchy.minesky.core.AnarchyCoreBootstrap;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.plugin.Plugin;

public final class EscPauseMenuSupport {

    private EscPauseMenuSupport() {}

    public static void logRegistryStatus(Plugin plugin) {
        try {
            var registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG);
            Dialog dialog = registry.get(AnarchyCoreBootstrap.PLAYER_CONFIG_DIALOG_KEY);
            if (dialog == null) {
                plugin.getLogger().warning(
                        "[AnarchyCore] Menu ESC: diálogo anarchycore:player_config ausente no registry. "
                                + "Confira se o JAR contém paper-plugin.yml com bootstrapper e reinicie o servidor.");
            } else {
                plugin.getLogger().info("[AnarchyCore] Menu ESC: diálogo registrado (anarchycore:player_config).");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[AnarchyCore] Menu ESC: não foi possível inspecionar registry: " + t.getMessage());
        }
    }
}
