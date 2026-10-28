package anarchy.minesky.core.impl.config;

import anarchy.minesky.core.api.text.DialogComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Objects;

public final class MessagesManager {

    private final Plugin plugin;
    private FileConfiguration messages;

    public MessagesManager(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public String raw(String path) {
        if (messages == null) {
            return path;
        }
        String value = messages.getString(path);
        return value != null ? value : path;
    }

    public String format(String path, String... placeholders) {
        String text = raw(path);
        if (placeholders != null) {
            for (int i = 0; i + 1 < placeholders.length; i += 2) {
                String key = placeholders[i];
                String value = Objects.toString(placeholders[i + 1], "");
                text = text.replace("{" + key + "}", value);
            }
        }
        return text;
    }

    public Component component(String path, String... placeholders) {
        return DialogComponentUtils.parse(format(path, placeholders));
    }
}
