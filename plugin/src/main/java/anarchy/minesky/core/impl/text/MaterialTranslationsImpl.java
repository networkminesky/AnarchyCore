package anarchy.minesky.core.impl.text;

import anarchy.minesky.core.api.text.MaterialTranslationsAPI;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MaterialTranslationsImpl implements MaterialTranslationsAPI {

    private final Plugin plugin;
    private Map<String, String> translationTable = Map.of();
    private volatile Map<Material, String> materialPortugueseCache = Map.of();
    private volatile Map<Material, String> materialEnglishCache = Map.of();
    private volatile Map<EntityType, String> entityPortugueseCache = Map.of();
    private volatile Map<EntityType, String> entityEnglishCache = Map.of();

    public MaterialTranslationsImpl(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void register() {
        plugin.getServer().getServicesManager().register(
                MaterialTranslationsAPI.class, this, plugin, ServicePriority.Normal);
    }

    public void unregister() {
        plugin.getServer().getServicesManager().unregister(MaterialTranslationsAPI.class, this);
    }

    @Override
    public void reload() {
        translationTable = loadTranslations(plugin);
        Map<Material, String> portugueseMaterials = new EnumMap<>(Material.class);
        Map<Material, String> englishMaterials = new EnumMap<>(Material.class);
        for (Material material : Registry.MATERIAL) {
            if (material.isAir()) {
                continue;
            }
            portugueseMaterials.put(material, resolveMaterialPortugueseName(material));
            englishMaterials.put(material, formatEnglishName(material));
        }
        Map<EntityType, String> portugueseEntities = new EnumMap<>(EntityType.class);
        Map<EntityType, String> englishEntities = new EnumMap<>(EntityType.class);
        for (EntityType entityType : EntityType.values()) {
            portugueseEntities.put(entityType, resolveEntityPortugueseName(entityType));
            englishEntities.put(entityType, formatEnglishName(entityType));
        }
        materialPortugueseCache = portugueseMaterials;
        materialEnglishCache = englishMaterials;
        entityPortugueseCache = portugueseEntities;
        entityEnglishCache = englishEntities;
    }

    @Override
    public String getPortugueseName(Material material) {
        if (material == null || material.isAir()) {
            return "";
        }
        String cached = materialPortugueseCache.get(material);
        return cached != null ? cached : "";
    }

    @Override
    public String getPortugueseName(EntityType entityType) {
        if (entityType == null) {
            return "";
        }
        String cached = entityPortugueseCache.get(entityType);
        return cached != null ? cached : "";
    }

    @Override
    public String getEnglishName(Material material) {
        if (material == null) {
            return "";
        }
        String cached = materialEnglishCache.get(material);
        return cached != null ? cached : formatEnglishName(material);
    }

    @Override
    public String getEnglishName(EntityType entityType) {
        if (entityType == null) {
            return "";
        }
        String cached = entityEnglishCache.get(entityType);
        return cached != null ? cached : formatEnglishName(entityType);
    }

    private String resolveMaterialPortugueseName(Material material) {
        if (!translationTable.isEmpty()) {
            String key;
            try {
                key = material.getKey().getKey();
            } catch (Throwable ignored) {
                key = material.name().toLowerCase(Locale.ROOT);
            }
            String name = null;

            try {
                name = translationTable.get(material.translationKey());
            } catch (Throwable ignored) {
            }

            if (name == null || name.isBlank()) {
                name = translationTable.get("item.minecraft." + key);
            }
            if (name == null || name.isBlank()) {
                name = translationTable.get("block.minecraft." + key);
            }
            if (name == null || name.isBlank()) {
                name = translationTable.get(key);
            }
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return formatEnglishName(material);
    }

    private String resolveEntityPortugueseName(EntityType entityType) {
        if (!translationTable.isEmpty()) {
            String key;
            try {
                key = entityType.getKey().getKey();
            } catch (Throwable ignored) {
                key = entityType.name().toLowerCase(Locale.ROOT);
            }
            String name = null;

            try {
                name = translationTable.get(entityType.translationKey());
            } catch (Throwable ignored) {
            }

            if (name == null || name.isBlank()) {
                name = translationTable.get("entity.minecraft." + key);
            }
            if (name == null || name.isBlank()) {
                name = translationTable.get(key);
            }
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return formatEnglishName(entityType);
    }

    public static String formatEnglishName(Material material) {
        if (material == null) {
            return "";
        }
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    public static String formatEnglishName(EntityType entityType) {
        if (entityType == null) {
            return "";
        }
        return entityType.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    static Map<String, String> loadTranslations(Plugin plugin) {
        Map<String, String> result = new HashMap<>();

        File langFile = new File(plugin.getDataFolder(), "lang/pt_br.json");
        if (!langFile.exists()) {
            File alt = new File(plugin.getDataFolder(), "pt_br.json");
            if (alt.exists()) {
                langFile = alt;
            }
        }
        if (!langFile.exists()) {
            File rootLang = new File("lang/pt_br.json");
            if (rootLang.exists()) {
                langFile = rootLang;
            }
        }

        Map<String, String> bundled = loadBundled(plugin);
        if (langFile.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(langFile), StandardCharsets.UTF_8)) {
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> parsed = new Gson().fromJson(reader, type);
                result.putAll(bundled);
                if (parsed != null) {
                    result.putAll(parsed);
                }
                plugin.getLogger().info("Carregadas " + result.size() + " traduções pt_br de " + langFile.getPath());
                return result;
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar traduções de " + langFile.getPath() + ": " + e.getMessage());
            }
        }

        result.putAll(bundled);
        if (!result.isEmpty()) {
            plugin.getLogger().info("Carregadas " + result.size() + " traduções pt_br dos recursos internos.");
        }
        return result;
    }

    private static Map<String, String> loadBundled(Plugin plugin) {
        Map<String, String> bundled = new HashMap<>();
        InputStream in = plugin.getResource("lang/pt_br.json");
        if (in == null) {
            in = plugin.getResource("pt_br.json");
        }
        if (in == null) {
            return bundled;
        }
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> parsed = new Gson().fromJson(reader, type);
            if (parsed != null) {
                bundled.putAll(parsed);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao carregar traduções do recurso pt_br.json: " + e.getMessage());
        }
        return bundled;
    }
}
