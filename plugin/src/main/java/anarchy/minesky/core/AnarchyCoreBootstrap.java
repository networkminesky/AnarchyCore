package anarchy.minesky.core;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DialogKeys;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class AnarchyCoreBootstrap implements PluginBootstrap {

    public static final Key OPEN_CONFIG_KEY = Key.key("anarchycore", "config/open");

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.DIALOG.compose(), event -> event.registry().register(
                DialogKeys.create(Key.key("anarchycore", "player_config")),
                builder -> builder
                        .base(DialogBase.builder(Component.text("Configurações MineSky", NamedTextColor.GREEN))
                                .externalTitle(Component.text("Configurações MineSky", NamedTextColor.GREEN))
                                .build())
                        .type(DialogType.notice(
                                ActionButton.builder(Component.text("Abrir", NamedTextColor.GREEN))
                                        .action(DialogAction.customClick(OPEN_CONFIG_KEY, null))
                                        .build()
                        ))
        ));
    }
}
