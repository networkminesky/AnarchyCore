package anarchy.minesky.core;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DialogKeys;
import io.papermc.paper.registry.keys.tags.DialogTagKeys;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

public final class AnarchyCoreBootstrap implements PluginBootstrap {

    public static final Key PLAYER_CONFIG_DIALOG = Key.key("anarchycore", "player_config");
    public static final TypedKey<Dialog> PLAYER_CONFIG_DIALOG_KEY = DialogKeys.create(PLAYER_CONFIG_DIALOG);
    public static final Key OPEN_CONFIG_KEY = Key.key("anarchycore", "config/open");

    private static final Component ESC_BUTTON = Component.text("Configurações MineSky", NamedTextColor.GREEN, TextDecoration.BOLD);

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.DIALOG.compose(), event -> event.registry().register(
                DialogKeys.create(PLAYER_CONFIG_DIALOG),
                builder -> builder
                        .base(DialogBase.builder(Component.text("Configurações MineSky", NamedTextColor.GREEN))
                                .externalTitle(ESC_BUTTON)
                                .build())
                        .type(DialogType.notice(
                                ActionButton.builder(Component.text("Abrir menu", NamedTextColor.GREEN))
                                        .width(280)
                                        .action(DialogAction.customClick(OPEN_CONFIG_KEY, null))
                                        .build()
                        ))
        ));

        context.getLifecycleManager().registerEventHandler(
                LifecycleEvents.TAGS.postFlatten(RegistryKey.DIALOG),
                event -> event.registrar().setTag(
                        DialogTagKeys.PAUSE_SCREEN_ADDITIONS,
                        List.of(PLAYER_CONFIG_DIALOG_KEY)));
    }
}
