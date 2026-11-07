package anarchy.minesky.core.impl.kits;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
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

public final class KitStorage {

    private final Plugin plugin;
    private final File file;
    private final Map<String, KitDefinition> kits = new LinkedHashMap<>();

    public KitStorage(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kits.yml");
    }

    public void load() {
        kits.clear();
        if (!file.exists()) {
            saveQuietly();
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("kits");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection kitSection = section.getConfigurationSection(id);
            if (kitSection == null) {
                continue;
            }
            String displayName = kitSection.getString("display-name", id);
            Material icon = parseMaterial(kitSection.getString("icon"), Material.CHEST);
            int cooldown = kitSection.getInt("cooldown-seconds", 0);
            ItemStack[] contents = readContents(kitSection.getConfigurationSection("contents"));
            kits.put(id.toLowerCase(Locale.ROOT), new KitDefinition(
                    id.toLowerCase(Locale.ROOT), displayName, icon, cooldown, contents));
        }
    }

    public void save() throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        for (KitDefinition kit : kits.values()) {
            String path = "kits." + kit.id();
            yaml.set(path + ".display-name", kit.displayName());
            yaml.set(path + ".icon", kit.iconMaterial().name());
            yaml.set(path + ".cooldown-seconds", kit.cooldownSeconds());
            writeContents(yaml, path + ".contents", kit.contents());
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
            plugin.getLogger().severe("Falha ao salvar kits.yml: " + e.getMessage());
        }
    }

    public void put(KitDefinition kit) {
        kits.put(kit.id(), kit);
        saveQuietly();
    }

    public boolean remove(String id) {
        if (id == null) {
            return false;
        }
        KitDefinition removed = kits.remove(id.toLowerCase(Locale.ROOT));
        if (removed == null) {
            return false;
        }
        saveQuietly();
        return true;
    }

    public Optional<KitDefinition> get(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(kits.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<KitDefinition> all() {
        return Collections.unmodifiableCollection(kits.values());
    }

    public List<String> ids() {
        return new ArrayList<>(kits.keySet());
    }

    private static ItemStack[] readContents(ConfigurationSection section) {
        ItemStack[] contents = KitDefinition.emptyContents();
        if (section == null) {
            return contents;
        }
        for (String key : section.getKeys(false)) {
            int slot;
            try {
                slot = Integer.parseInt(key);
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (slot < 0 || slot >= KitDefinition.CONTENT_SLOT_COUNT) {
                continue;
            }
            ItemStack stack = section.getItemStack(key);
            if (stack != null && !stack.getType().isAir()) {
                contents[slot] = stack;
            }
        }
        return contents;
    }

    private static void writeContents(YamlConfiguration yaml, String path, ItemStack[] contents) {
        for (int i = 0; i < KitDefinition.CONTENT_SLOT_COUNT; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            yaml.set(path + "." + i, stack);
        }
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
}
