package anarchy.minesky.core;

import anarchy.minesky.core.impl.escmenu.EscMenuConfigLoader;
import anarchy.minesky.core.impl.escmenu.EscMenuDefinition;
import anarchy.minesky.core.impl.escmenu.EscMenuDialogFactory;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DialogKeys;
import io.papermc.paper.registry.keys.tags.DialogTagKeys;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import net.kyori.adventure.key.Key;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Registro do hub ESC {@code anarchycore:player_config} (requer {@code paper-plugin.yml}).
 */
public final class AnarchyCoreBootstrap implements PluginBootstrap {

    public static final Key PLAYER_CONFIG_DIALOG = Key.key("anarchycore", "player_config");
    public static final TypedKey<Dialog> PLAYER_CONFIG_DIALOG_KEY = DialogKeys.create(PLAYER_CONFIG_DIALOG);

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLogger().info("[AnarchyCore] Bootstrap — registrando hub ESC (esc-menu.yml).");

        final EscMenuDefinition menuDefinition = loadEscMenu(context);
        context.getLifecycleManager().registerEventHandler(RegistryEvents.DIALOG.compose(), event -> event.registry().register(
                PLAYER_CONFIG_DIALOG_KEY,
                builder -> EscMenuDialogFactory.applyDialog(builder, menuDefinition)
        ));

        context.getLifecycleManager().registerEventHandler(
                LifecycleEvents.TAGS.postFlatten(RegistryKey.DIALOG),
                event -> {
                    PostFlattenTagRegistrar<Dialog> registrar = event.registrar();
                    registrar.setTag(DialogTagKeys.PAUSE_SCREEN_ADDITIONS, Set.of(PLAYER_CONFIG_DIALOG_KEY));
                    context.getLogger().info(
                            "[AnarchyCore] Tag pause_screen_additions → anarchycore:player_config (hub 2 colunas).");
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

    private static EscMenuDefinition loadEscMenu(BootstrapContext context) {
        try {
            Path dataDirectory = context.getDataDirectory();
            EscMenuDefinition loaded = EscMenuConfigLoader.loadFromDataDirectory(dataDirectory);
            context.getLogger().info(
                    "[AnarchyCore] esc-menu.yml carregado ({} botões, {} colunas).",
                    loaded.buttons().size(),
                    loaded.columns());
            return loaded;
        } catch (IOException | RuntimeException error) {
            context.getLogger().error(
                    "[AnarchyCore] Falha ao carregar esc-menu.yml; usando padrão embutido.", error);
            try {
                return EscMenuConfigLoader.loadFromResourceOnly();
            } catch (IOException fallbackError) {
                throw new RuntimeException("esc-menu.yml embutido ausente", fallbackError);
            }
        }
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