package anarchy.minesky.core.impl.warps;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class WarpStorage {

    private final Plugin plugin;
    private final File file;
    private final Map<String, WarpDefinition> warps = new LinkedHashMap<>();

    public WarpStorage(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "warps.yml");
    }

    public void load() {
        warps.clear();
        if (!file.exists()) {
            saveQuietly();
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("warps");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection warpSection = section.getConfigurationSection(id);
            if (warpSection == null) {
                continue;
            }
            String displayName = warpSection.getString("display-name", id);
            Material icon = parseMaterial(warpSection.getString("icon"), Material.COMPASS);
            String world = warpSection.getString("world");
            if (world == null || world.isBlank()) {
                continue;
            }
            double x = warpSection.getDouble("x");
            double y = warpSection.getDouble("y");
            double z = warpSection.getDouble("z");
            float yaw = (float) warpSection.getDouble("yaw");
            float pitch = (float) warpSection.getDouble("pitch");
            warps.put(id.toLowerCase(Locale.ROOT), new WarpDefinition(
                    id.toLowerCase(Locale.ROOT), displayName, icon, world, x, y, z, yaw, pitch));
        }
    }

    public void save() throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        for (WarpDefinition warp : warps.values()) {
            String path = "warps." + warp.id();
            yaml.set(path + ".display-name", warp.displayName());
            yaml.set(path + ".icon", warp.iconMaterial().name());
            yaml.set(path + ".world", warp.worldName());
            yaml.set(path + ".x", warp.x());
            yaml.set(path + ".y", warp.y());
            yaml.set(path + ".z", warp.z());
            yaml.set(path + ".yaw", warp.yaw());
            yaml.set(path + ".pitch", warp.pitch());
        }
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IOException("Não foi possível criar pasta de dados do plugin");
        }
        yaml.save(file);
    }

    private void saveQuietly() {
        try {
            save();
        } catch (IOException e) {
            plugin.getLogger().severe("Falha ao salvar warps.yml: " + e.getMessage());
        }
    }

    public void put(WarpDefinition warp) {
        warps.put(warp.id(), warp);
        saveQuietly();
    }

    public Optional<WarpDefinition> get(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(warps.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<WarpDefinition> all() {
        return Collections.unmodifiableCollection(warps.values());
    }

    public List<WarpDefinition> accessibleBy(org.bukkit.entity.Player player) {
        List<WarpDefinition> list = new ArrayList<>();
        for (WarpDefinition warp : warps.values()) {
            if (player.hasPermission(warp.permissionNode())) {
                list.add(warp);
            }
        }
        return list;
    }

    public static boolean isValidId(String id) {
        if (id == null || id.isBlank() || id.length() > 32) {
            return false;
        }
        String lower = id.toLowerCase(Locale.ROOT);
        if (!Character.isLetter(lower.charAt(0))) {
            return false;
        }
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                continue;
            }
            return false;
        }
        return true;
    }

    public static String normalizeId(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }

    public static Material parseMaterial(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(name.trim().toUpperCase(Locale.ROOT));
        return material != null && material.isItem() ? material : fallback;
    }

    public static WarpDefinition createAt(String id, String displayName, Material icon, Location location) {
        return new WarpDefinition(normalizeId(id), displayName, icon, location);
    }
}
