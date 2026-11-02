package anarchy.minesky.core.impl.guard.fog;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

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
    private volatile boolean closed;

    private AnarchyTagsFogHook(
            Plugin plugin,
            Object api,
            Object filter,
            Method refreshVisibilityFor,
            Method refreshTag,
            Method unregisterVisibilityFilter
    ) {
        this.plugin = plugin;
        this.api = api;
        this.filter = filter;
        this.refreshVisibilityFor = refreshVisibilityFor;
        this.refreshTag = refreshTag;
        this.unregisterVisibilityFilter = unregisterVisibilityFilter;
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

        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshVisibilityFor.invoke(api, player);
        }
        plugin.getLogger().info("[AnarchyGuard] Fog of War integrado ao AnarchyTags.");
        return new AnarchyTagsFogHook(plugin, api, filter, refreshVisibilityFor, refreshTag, unregister);
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
}
