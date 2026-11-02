package anarchy.minesky.core.impl.guard.fog;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.IntConsumer;

/**
 * Liga o fog na API do AnarchyTags por reflexão. O AnarchyTags depende do
 * AnarchyCore e só habilita depois — o classloader do core não enxerga as classes dele.
 */
final class AnarchyTagsFogHook implements FogNametagIntegration {

    private final Plugin plugin;
    private final Object api;
    private final Object filter;
    private final Method refreshVisibilityFor;
    private final Method refreshTag;
    private final Method unregisterVisibilityFilter;
    private final Method getNametag;
    private final Method hideForViewer;
    private final Method presentTo;
    private final Method displayEntityId;
    private volatile boolean closed;

    private AnarchyTagsFogHook(
            Plugin plugin,
            Object api,
            Object filter,
            Method refreshVisibilityFor,
            Method refreshTag,
            Method unregisterVisibilityFilter,
            Method getNametag,
            Method hideForViewer,
            Method presentTo,
            Method displayEntityId
    ) {
        this.plugin = plugin;
        this.api = api;
        this.filter = filter;
        this.refreshVisibilityFor = refreshVisibilityFor;
        this.refreshTag = refreshTag;
        this.unregisterVisibilityFilter = unregisterVisibilityFilter;
        this.getNametag = getNametag;
        this.hideForViewer = hideForViewer;
        this.presentTo = presentTo;
        this.displayEntityId = displayEntityId;
    }

    static AnarchyTagsFogHook attach(Plugin plugin, FogOfWarManager fog, Plugin tagsPlugin) throws ReflectiveOperationException {
        ClassLoader loader = tagsPlugin.getClass().getClassLoader();
        Class<?> provider = Class.forName("anarchy.minesky.tags.api.AnarchyTagsProvider", true, loader);
        Object api = provider.getMethod("get").invoke(null);
        if (api == null) {
            throw new IllegalStateException("AnarchyTagsProvider.get() retornou null");
        }

        Class<?> filterType = Class.forName("anarchy.minesky.tags.api.NametagVisibilityFilter", true, loader);
        Object filter = Proxy.newProxyInstance(loader, new Class<?>[]{filterType}, handler(fog));
        api.getClass().getMethod("registerVisibilityFilter", filterType).invoke(api, filter);

        Method refreshVisibilityFor = api.getClass().getMethod("refreshVisibilityFor", Player.class);
        Method refreshTag = api.getClass().getMethod("refreshTag", Player.class);
        Method unregister = api.getClass().getMethod("unregisterVisibilityFilter", filterType);
        Method getNametag = api.getClass().getMethod("get", Player.class);
        Class<?> nametagClass = Class.forName("anarchy.minesky.tags.nametags.Nametag", true, loader);
        Method hideForViewer = nametagClass.getMethod("hide", Player.class);
        Method presentTo = nametagClass.getMethod("presentTo", Player.class);
        Method displayEntityId = nametagClass.getMethod("getDisplayEntityId");

        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshVisibilityFor.invoke(api, player);
        }
        plugin.getLogger().info("[AnarchyGuard] Fog of War integrado ao AnarchyTags.");
        return new AnarchyTagsFogHook(
                plugin,
                api,
                filter,
                refreshVisibilityFor,
                refreshTag,
                unregister,
                getNametag,
                hideForViewer,
                presentTo,
                displayEntityId);
    }

    private static InvocationHandler handler(FogOfWarManager fog) {
        return (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "FogNametagFilter";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                };
            }
            return switch (method.getName()) {
                case "shouldShow" -> fog.allowsExternalNametag((Player) args[0], (Player) args[1]);
                case "priority" -> 100;
                default -> null;
            };
        };
    }

    @Override
    public void stop() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            unregisterVisibilityFilter.invoke(api, filter);
        } catch (ReflectiveOperationException error) {
            plugin.getLogger().warning("[AnarchyGuard] Falha ao remover o filtro de tag: " + error.getMessage());
        }
    }

    @Override
    public void onVisibilityChanged(Player target) {
        if (closed || target == null || !target.isOnline()) {
            return;
        }
        try {
            refreshVisibilityFor.invoke(api, target);
            refreshTag.invoke(api, target);
        } catch (ReflectiveOperationException error) {
            plugin.getLogger().warning("[AnarchyGuard] Falha ao atualizar tag do AnarchyTags: " + error.getMessage());
        }
    }

    @Override
    public void floatTag(Player viewer, Player target, IntConsumer onSpawned) {
        if (closed || viewer == null || target == null || !target.isOnline() || onSpawned == null) {
            return;
        }
        target.getScheduler().run(plugin, task -> spawnFloatingTag(viewer, target, onSpawned), null);
    }

    @Override
    public void hideTagFrom(Player viewer, Player target) {
        if (closed || viewer == null || target == null || !target.isOnline()) {
            return;
        }
        target.getScheduler().run(plugin, task -> {
            try {
                Object nametag = getNametag.invoke(api, target);
                if (nametag != null) {
                    hideForViewer.invoke(nametag, viewer);
                }
            } catch (ReflectiveOperationException error) {
                plugin.getLogger().warning("[AnarchyGuard] Falha ao esconder tag do AnarchyTags: " + error.getMessage());
            }
        }, null);
    }

    private void spawnFloatingTag(Player viewer, Player target, IntConsumer onSpawned) {
        if (closed || !target.isOnline()) {
            return;
        }
        try {
            Object nametag = getNametag.invoke(api, target);
            if (nametag == null) {
                return;
            }
            hideForViewer.invoke(nametag, viewer);
            Object shown = presentTo.invoke(nametag, viewer);
            if (!(shown instanceof Boolean spawned) || !spawned) {
                return;
            }
            Object id = displayEntityId.invoke(nametag);
            if (id instanceof Integer entityId && entityId > 0) {
                onSpawned.accept(entityId);
            }
        } catch (ReflectiveOperationException error) {
            plugin.getLogger().warning("[AnarchyGuard] Falha ao soltar a tag do AnarchyTags: " + error.getMessage());
        }
    }
}
