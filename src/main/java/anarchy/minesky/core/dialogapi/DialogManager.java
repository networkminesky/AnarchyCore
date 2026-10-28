package anarchy.minesky.core.dialogapi;

import anarchy.minesky.core.api.dialogapi.DialogAPI;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.dialogapi.renderers.BedrockDialogRenderer;
import anarchy.minesky.core.dialogapi.renderers.JavaDialogRenderer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

public final class DialogManager implements DialogAPI {

    private final Plugin plugin;

    public DialogManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getServicesManager().register(DialogAPI.class, this, plugin, ServicePriority.High);
    }

    public void stop() {
        plugin.getServer().getServicesManager().unregister(DialogAPI.class, this);
    }

    @Override
    public void show(Player player, MineSkyDialog dialog) {
        if (player == null || !player.isOnline() || dialog == null || !plugin.isEnabled()) return;

        player.getScheduler().run(plugin, task -> {
            if (!plugin.isEnabled()) return;
            if (isBedrock(player)) {
                BedrockDialogRenderer.renderAndShow(plugin, player, dialog);
            } else {
                JavaDialogRenderer.renderAndShow(plugin, player, dialog);
            }
        }, null);
    }

    @Override
    public MineSkyDialog.Builder createDialog() {
        return MineSkyDialog.builder();
    }

    @Override
    public MineSkyDialog.Builder createDialog(Component title) {
        return MineSkyDialog.builder(title);
    }

    @Override
    public MineSkyDialog.Builder createDialog(String title) {
        return MineSkyDialog.builder(title);
    }

    @Override
    public boolean isBedrock(Player player) {
        return BedrockPlatformDetector.isBedrock(player);
    }
}
