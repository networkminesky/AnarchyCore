package anarchy.minesky.core.impl.dialogapi;

import anarchy.minesky.core.api.dialogapi.DialogAPI;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.text.MaterialTranslationsAPI;
import anarchy.minesky.core.impl.dialogapi.renderers.BedrockDialogRenderer;
import anarchy.minesky.core.impl.dialogapi.renderers.JavaDialogRenderer;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class DialogManager implements DialogAPI {

    private final Plugin plugin;
    private final MaterialPickerDialog materialPicker;
    private final PlayerPickerDialog playerPicker;

    public DialogManager(Plugin plugin, MaterialTranslationsAPI materialTranslations) {
        this.plugin = plugin;
        this.materialPicker = new MaterialPickerDialog(materialTranslations);
        this.playerPicker = new PlayerPickerDialog();
    }

    public void reloadMaterialSearch() {
        materialPicker.reloadSearchIndex();
    }

    public Plugin plugin() {
        return plugin;
    }

    public PlayerPickerDialog playerPicker() {
        return playerPicker;
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

    @Override
    public void openMaterialPicker(Player player, String title, java.util.function.Consumer<Material> onSelected) {
        openMaterialPicker(player, DialogComponentUtils.parse(title), null, onSelected);
    }

    @Override
    public void openMaterialPicker(Player player, Component title, java.util.function.Consumer<Material> onSelected) {
        openMaterialPicker(player, title, null, onSelected);
    }

    @Override
    public void openMaterialPicker(Player player, String title, String initialQuery,
                                   java.util.function.Consumer<Material> onSelected) {
        openMaterialPicker(player, DialogComponentUtils.parse(title), initialQuery, onSelected);
    }

    @Override
    public void openMaterialPicker(Player player, Component title, String initialQuery,
                                   java.util.function.Consumer<Material> onSelected) {
        if (player == null || !player.isOnline() || onSelected == null || !plugin.isEnabled()) {
            return;
        }
        player.getScheduler().run(plugin, task -> {
            if (plugin.isEnabled()) {
                materialPicker.open(player, title, initialQuery, onSelected);
            }
        }, null);
    }

    @Override
    public void openPlayerPicker(
            Player player,
            String title,
            Predicate<Player> visible,
            String initialQuery,
            Consumer<String> onNickSelected) {
        openPlayerPicker(player, DialogComponentUtils.parse(title), visible, initialQuery, onNickSelected);
    }

    @Override
    public void openPlayerPicker(
            Player player,
            Component title,
            Predicate<Player> visible,
            String initialQuery,
            Consumer<String> onNickSelected) {
        if (player == null || !player.isOnline() || onNickSelected == null || !plugin.isEnabled()) {
            return;
        }
        player.getScheduler().run(plugin, task -> {
            if (plugin.isEnabled()) {
                playerPicker.open(player, title, visible, initialQuery, onNickSelected);
            }
        }, null);
    }
}
