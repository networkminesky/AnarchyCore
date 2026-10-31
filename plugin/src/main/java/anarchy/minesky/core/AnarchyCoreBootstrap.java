package anarchy.minesky.core;

import anarchy.minesky.core.api.text.DialogComponentUtils;
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
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Registro do botão {@code /config} no menu ESC (requer {@code paper-plugin.yml}).
 */
public final class AnarchyCoreBootstrap implements PluginBootstrap {

    public static final Key PLAYER_CONFIG_DIALOG = Key.key("anarchycore", "player_config");
    public static final TypedKey<Dialog> PLAYER_CONFIG_DIALOG_KEY = DialogKeys.create(PLAYER_CONFIG_DIALOG);
    public static final Key OPEN_CONFIG_KEY = Key.key("anarchycore", "config/open");

    /** Sem {@code Material} — o bootstrap roda antes dos registries Bukkit. */
    private static final Component ESC_MENU_LABEL =
            DialogComponentUtils.itemSpriteLabel("end_crystal", "Configurações");

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLogger().info("[AnarchyCore] Bootstrap — registrando menu ESC (/config).");

        context.getLifecycleManager().registerEventHandler(RegistryEvents.DIALOG.compose(), event -> event.registry().register(
                PLAYER_CONFIG_DIALOG_KEY,
                builder -> builder
                        .base(DialogBase.builder(ESC_MENU_LABEL)
                                .externalTitle(ESC_MENU_LABEL)
                                .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
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
                event -> {
                    PostFlattenTagRegistrar<Dialog> registrar = event.registrar();
                    registrar.setTag(DialogTagKeys.PAUSE_SCREEN_ADDITIONS, Set.of(PLAYER_CONFIG_DIALOG_KEY));
                    context.getLogger().info(
                            "[AnarchyCore] Tag pause_screen_additions → anarchycore:player_config (API).");
                });

        context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {
            try {
                URI packUri = resolveEscDatapackUri(context);
                if (packUri == null) {
                    context.getLogger().warn(
                            "[AnarchyCore] Datapack ESC ausente no JAR; menu ESC depende só da tag via API.");
                    return;
                }
                event.registrar().discoverPack(packUri, "provided");
                context.getLogger().info("[AnarchyCore] Datapack ESC descoberto: {}", packUri);
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException("Falha ao registrar datapack do menu ESC", e);
            }
        });
    }

    private static URI resolveEscDatapackUri(BootstrapContext context) throws URISyntaxException, IOException {
        URL marker = AnarchyCoreBootstrap.class.getResource("/datapack/pack.mcmeta");
        if (marker != null) {
            String url = marker.toString();
            if (url.startsWith("jar:")) {
                int sep = url.indexOf("!/datapack/");
                if (sep > 0) {
                    return URI.create(url.substring(0, sep + "!/datapack".length()));
                }
            } else if (url.endsWith("/pack.mcmeta")) {
                return URI.create(url.substring(0, url.length() - "/pack.mcmeta".length()));
            }
        }

        Path source = context.getPluginSource();
        if (source != null && Files.isDirectory(source)) {
            Path packDir = source.resolve("datapack");
            return Files.isDirectory(packDir) ? packDir.toUri() : null;
        }
        if (source != null && Files.isRegularFile(source)) {
            return URI.create("jar:" + source.toUri().getSchemeSpecificPart() + "!/datapack");
        }
        return null;
    }
}
