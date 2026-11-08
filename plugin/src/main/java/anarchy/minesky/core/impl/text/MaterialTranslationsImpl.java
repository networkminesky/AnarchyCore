package anarchy.minesky.core.impl.text;

import anarchy.minesky.core.api.text.MaterialTranslationsAPI;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Material;
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
    private final Map<Material, String> portugueseCache = new EnumMap<>(Material.class);

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
        portugueseCache.clear();
        translationTable = loadTranslations(plugin);
    }

    @Override
    public String getPortugueseName(Material material) {
        if (material == null || material.isAir()) {
            return "";
        }
        return portugueseCache.computeIfAbsent(material, this::resolvePortugueseName);
    }

    @Override
    public String getEnglishName(Material material) {
        return formatEnglishName(material);
    }

    private String resolvePortugueseName(Material material) {
        if (material.isLegacy()) {
            return formatEnglishName(material);
        }
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

    public static String formatEnglishName(Material material) {
        if (material == null) {
            return "";
        }
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
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

        if (langFile.exists()) {
            try (Reader reader = new InputStreamReader(new FileInputStream(langFile), StandardCharsets.UTF_8)) {
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> parsed = new Gson().fromJson(reader, type);
                if (parsed != null) {
                    result.putAll(parsed);
                }
                plugin.getLogger().info("Carregadas " + result.size() + " traduções pt_br de " + langFile.getPath());
                return result;
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar traduções de " + langFile.getPath() + ": " + e.getMessage());
            }
        }

        InputStream in = plugin.getResource("lang/pt_br.json");
        if (in == null) {
            in = plugin.getResource("pt_br.json");
        }
        if (in != null) {
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> parsed = new Gson().fromJson(reader, type);
                if (parsed != null) {
                    result.putAll(parsed);
                }
                plugin.getLogger().info("Carregadas " + result.size() + " traduções pt_br dos recursos internos.");
            } catch (Exception e) {
                plugin.getLogger().warning("Erro ao carregar traduções do recurso pt_br.json: " + e.getMessage());
            }
        }

        return result;
    }
}
