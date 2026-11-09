package anarchy.minesky.core.impl.tab;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TabSettings {

    public record TabGroup(String id, String permission, List<String> commands) {
    }

    private static final String BYPASS_KEY = "bypass-permission";

    private final Plugin plugin;

    private String bypassPermission = "minesky.admin";
    private List<TabGroup> groups = List.of();

    public TabSettings(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "tab.yml");
        if (!file.exists()) {
            plugin.saveResource("tab.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        bypassPermission = config.getString(BYPASS_KEY, bypassPermission);
        if (bypassPermission == null) {
            bypassPermission = "";
        }

        Map<String, TabGroup> loaded = new LinkedHashMap<>();
        for (String key : config.getKeys(false)) {
            if (BYPASS_KEY.equalsIgnoreCase(key)) {
                continue;
            }
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            String permission = section.getString("permission", "");
            if (permission == null) {
                permission = "";
            }
            List<String> rawList = section.getStringList("list");
            List<String> commands = normalizeCommandList(rawList);
            loaded.put(key.toLowerCase(Locale.ROOT), new TabGroup(key, permission, commands));
        }
        groups = List.copyOf(loaded.values());
    }

    private static List<String> normalizeCommandList(Collection<String> raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            return List.of();
        }
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String normalized = normalizeCommandRoot(entry);
            if (!normalized.isEmpty()) {
                out.add(normalized);
            }
        }
        return List.copyOf(out);
    }

    public static String normalizeCommandRoot(String raw) {
        if (raw == null) {
            return "";
        }
        String label = raw.trim();
        if (label.startsWith("/")) {
            label = label.substring(1);
        }
        int colon = label.indexOf(':');
        if (colon >= 0 && colon < label.length() - 1) {
            label = label.substring(colon + 1);
        }
        int space = label.indexOf(' ');
        if (space >= 0) {
            label = label.substring(0, space);
        }
        return label.toLowerCase(Locale.ROOT);
    }

    /**
     * Raízes permitidas para tab e / do cliente.
     * {@code null} = sem filtro (apenas lista de bloqueio hardcoded).
     */
    public @org.jetbrains.annotations.Nullable Set<String> resolveAllowedRoots(Player player) {
        if (player == null) {
            return Set.of();
        }
        if (!bypassPermission.isBlank() && player.hasPermission(bypassPermission)) {
            return null;
        }
        if (player.hasPermission("anarchycore.admin")) {
            return null;
        }

        Set<String> allowed = new HashSet<>();
        for (TabGroup group : groups) {
            String perm = group.permission();
            if (perm.isEmpty() || player.hasPermission(perm)) {
                allowed.addAll(group.commands());
            }
        }
        return allowed;
    }

    public String bypassPermission() {
        return bypassPermission;
    }

    public List<TabGroup> groups() {
        return groups;
    }
}
