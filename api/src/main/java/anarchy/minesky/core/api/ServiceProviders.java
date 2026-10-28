package anarchy.minesky.core.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;

/**
 * Resolve serviços registrados pelo AnarchyCore mesmo quando a API foi carregada
 * por outro classloader (ex.: JAR sombreado com {@code implementation} da API).
 */
public final class ServiceProviders {

    private static final String CORE_PLUGIN_NAME = "AnarchyCore";

    private ServiceProviders() {}

    public static <T> T get(Class<T> apiInterface, String binaryName) {
        ServicesManager services = Bukkit.getServicesManager();

        RegisteredServiceProvider<T> direct = services.getRegistration(apiInterface);
        if (direct != null) {
            return direct.getProvider();
        }

        Plugin core = Bukkit.getPluginManager().getPlugin(CORE_PLUGIN_NAME);
        if (core == null || !core.isEnabled()) {
            return null;
        }

        try {
            Class<?> coreType = Class.forName(binaryName, false, core.getClass().getClassLoader());
            RegisteredServiceProvider<?> fromCore = services.getRegistration(coreType);
            if (fromCore == null) {
                return null;
            }
            return apiInterface.cast(fromCore.getProvider());
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    public static boolean isRegistered(Class<?> apiInterface, String binaryName) {
        ServicesManager services = Bukkit.getServicesManager();
        if (services.getRegistration(apiInterface) != null) {
            return true;
        }
        Plugin core = Bukkit.getPluginManager().getPlugin(CORE_PLUGIN_NAME);
        if (core == null || !core.isEnabled()) {
            return false;
        }
        try {
            Class<?> coreType = Class.forName(binaryName, false, core.getClass().getClassLoader());
            return services.getRegistration(coreType) != null;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
