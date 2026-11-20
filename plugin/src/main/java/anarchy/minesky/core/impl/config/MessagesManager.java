package anarchy.minesky.core.impl.config;

import anarchy.minesky.core.api.text.DialogComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        // Chaves novas do jar continuam valendo se o messages.yml do servidor for antigo.
        try (InputStream defaults = plugin.getResource("messages.yml")) {
            if (defaults != null) {
                YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaults, StandardCharsets.UTF_8));
                loaded.setDefaults(bundled);
                loaded.options().copyDefaults(true);
                boolean dirty = false;
                if (needsReferralMerge(file)) {
                    String legacyRoot = "&cUso: /convite [usar <codigo>|codigo|status]";
                    if (legacyRoot.equals(diskString(file, "referral.usage.root"))) {
                        String updated = bundled.getString("referral.usage.root");
                        if (updated != null) {
                            loaded.set("referral.usage.root", updated);
                        }
                    }
                    loaded.save(file);
                    dirty = true;
                }
                if (dirty) {
                    plugin.getLogger().info("[Referral] Chaves novas de referral gravadas em messages.yml.");
                }
            }
        } catch (IOException ignored) {
            plugin.getLogger().warning("[Referral] Não foi possível mesclar messages.yml; chaves novas ficam só em memória.");
        }
        messages = loaded;
    }

    private static boolean needsReferralMerge(File file) throws IOException {
        YamlConfiguration disk = YamlConfiguration.loadConfiguration(file);
        if (!disk.contains("referral.custom.success") || !disk.contains("referral.dialog.skipped")) {
            return true;
        }
        return "&cUso: /convite [usar <codigo>|codigo|status]".equals(disk.getString("referral.usage.root"));
    }

    private static String diskString(File file, String path) throws IOException {
        YamlConfiguration disk = YamlConfiguration.loadConfiguration(file);
        return disk.getString(path);
    }

    public Component componentOr(String path, String fallback, String... placeholders) {
        String text = raw(path);
        if (text == null || text.equals(path)) {
            text = fallback != null ? fallback : path;
            if (placeholders != null) {
                for (int i = 0; i + 1 < placeholders.length; i += 2) {
                    text = text.replace("{" + placeholders[i] + "}", Objects.toString(placeholders[i + 1], ""));
                }
            }
            return DialogComponentUtils.parse(text);
        }
        return component(path, placeholders);
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
