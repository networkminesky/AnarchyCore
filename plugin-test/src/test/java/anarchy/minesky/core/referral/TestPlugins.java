package anarchy.minesky.core.referral;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.logging.Logger;

/** Plugin mínimo para testes JDBC sem Mockito em interfaces Bukkit. */
final class TestPlugins {

    private TestPlugins() {}

    static Plugin create() {
        return create(new YamlConfiguration());
    }

    static Plugin create(FileConfiguration config) {
        Logger logger = Logger.getLogger("referral-test");
        File dataFolder = new File(".");
        @SuppressWarnings("deprecation")
        PluginDescriptionFile description = new PluginDescriptionFile("test", "0", "");

        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getLogger" -> logger;
                    case "getDataFolder" -> dataFolder;
                    case "getDescription" -> description;
                    case "getConfig" -> config;
                    case "isEnabled" -> true;
                    case "getName" -> "referral-test";
                    case "onTabComplete" -> List.of();
                    case "toString" -> "referral-test-plugin";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> args != null && args.length == 1 && proxy == args[0];
                    default -> defaultReturnValue(method.getReturnType());
                });
    }

    private static Object defaultReturnValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0.0;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }
}
