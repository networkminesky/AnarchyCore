package anarchy.minesky.core.impl.dialogapi;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.DialogResponse;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.dialogapi.inputs.TextInput;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class MaterialPickerDialog {

    private static final String INPUT_QUERY = "query";
    private static final int MAX_RESULTS = 48;

    private final Plugin plugin;
    private final Map<String, String> translations;
    private final MaterialSearch searchIndex;

    public MaterialPickerDialog(Plugin plugin) {
        this.plugin = plugin;
        this.translations = loadTranslations(plugin);
        this.searchIndex = new MaterialSearch(this.translations);
    }

    public void open(Player player, Component title, Consumer<Material> onSelected) {
        if (player == null || !player.isOnline() || onSelected == null) {
            return;
        }
        MineSkyDialog.builder(title)
                .body(Component.text("Digite parte do nome e clique em Procurar."))
                .addInput(TextInput.builder(INPUT_QUERY, Component.text("Buscar material"))
                        .placeholder("Ex: diamante, espada, apple, bed")
                        .maxLength(48)
                        .build())
                .button(DialogButton.builder()
                        .label(Component.text("Procurar"))
                        .width(200)
                        .onClick((p, response) -> showResults(p, title, response, onSelected))
                        .build())
                .button(DialogButton.builder()
                        .label(Component.text("Cancelar"))
                        .width(150)
                        .onClick(p -> { })
                        .build())
                .show(player);
    }

    private void showResults(Player player, Component title, DialogResponse response, Consumer<Material> onSelected) {
        String query = response.getText(INPUT_QUERY, "").trim();
        List<Material> matches = searchIndex.search(query, MAX_RESULTS);
        if (matches.isEmpty()) {
            open(player, title, onSelected);
            return;
        }

        MineSkyDialog.Builder builder = MineSkyDialog.builder(title)
                .body(Component.text("Resultados: " + matches.size() + (matches.size() >= MAX_RESULTS ? "+" : "")))
                .multiActionColumns(2);

        for (Material material : matches) {
            builder.button(DialogButton.builder()
                    .label(materialButtonLabel(material))
                    .width(240)
                    .onClick(p -> onSelected.accept(material))
                    .build());
        }

        builder.button(DialogButton.builder()
                .label(Component.text("Nova busca"))
                .width(200)
                .onClick(p -> open(p, title, onSelected))
                .build());

        builder.show(player);
    }

    Component materialButtonLabel(Material material) {
        String displayName = searchIndex.getDisplayName(material);
        String english = formatMaterialName(material);

        Component label = DialogComponentUtils.materialIconComponent(material)
                .append(Component.space())
                .append(Component.text(displayName));

        if (!displayName.equalsIgnoreCase(english)) {
            label = label.hoverEvent(HoverEvent.showText(
                    Component.text(english, NamedTextColor.GRAY)
            ));
        }

        return label;
    }

    public static String formatMaterialName(Material material) {
        if (material == null) return "";
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static Map<String, String> loadTranslations(Plugin plugin) {
        Map<String, String> result = new HashMap<>();

        // 1. plugins/<Plugin>/lang/pt_br.json
        File langFile = new File(plugin.getDataFolder(), "lang/pt_br.json");

        // 2. plugins/<Plugin>/pt_br.json
        if (!langFile.exists()) {
            File alt = new File(plugin.getDataFolder(), "pt_br.json");
            if (alt.exists()) {
                langFile = alt;
            }
        }

        // 3. ./lang/pt_br.json na raiz do servidor
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

        // 4. Recursos embutidos no jar do plugin
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

    static final class MaterialSearch {

        private final List<MaterialEntry> entries = new ArrayList<>();
        private final Map<Material, String> displayNameCache = new EnumMap<>(Material.class);

        MaterialSearch(Map<String, String> translations) {
            reload(translations);
        }

        void reload(Map<String, String> translations) {
            entries.clear();
            displayNameCache.clear();

            for (Material material : Material.values()) {
                // Ignora materiais legados (LEGACY_*), ar e materiais que não são blocos nem itens
                if (material.isLegacy() || material.isAir() || (!material.isItem() && !material.isBlock())) {
                    continue;
                }
                String localized = resolveLocalizedName(material, translations);
                String english = formatMaterialName(material);
                displayNameCache.put(material, localized);

                String normLoc = stripAccents(localized);
                String normEng = stripAccents(english);
                String rawLocLower = localized.toLowerCase(Locale.ROOT);
                String rawName = material.name().toLowerCase(Locale.ROOT);

                String key;
                try {
                    key = material.getKey().getKey().replace('_', ' ');
                } catch (Throwable ignored) {
                    key = rawName.replace('_', ' ');
                }

                String haystack = (normLoc + " " + rawLocLower + " " + normEng + " " + rawName + " " + key).trim();
                String[] words = (normLoc + " " + normEng).split("\\s+");

                entries.add(new MaterialEntry(material, localized, english, normLoc, haystack, words));
            }
        }

        String getDisplayName(Material material) {
            return displayNameCache.getOrDefault(material, formatMaterialName(material));
        }

        List<Material> search(String rawQuery, int limit) {
            if (rawQuery == null || rawQuery.isBlank()) {
                return List.of();
            }
            String normalizedQuery = stripAccents(rawQuery.trim());
            String[] tokens = normalizedQuery.split("\\s+");

            return entries.stream()
                    .filter(entry -> matchesAllTokens(entry, tokens))
                    .sorted(Comparator
                            .<MaterialEntry>comparingInt(e -> matchScore(e, normalizedQuery))
                            .thenComparing(e -> e.localizedName, String.CASE_INSENSITIVE_ORDER))
                    .limit(limit)
                    .map(e -> e.material)
                    .collect(Collectors.toList());
        }

        private static boolean matchesAllTokens(MaterialEntry entry, String[] tokens) {
            for (String token : tokens) {
                if (token.isEmpty()) continue;
                if (!entry.normalizedHaystack.contains(token)) {
                    return false;
                }
            }
            return true;
        }

        private static int matchScore(MaterialEntry entry, String query) {
            if (entry.normalizedLocalized.equals(query) || entry.englishName.equals(query)) {
                return 0; // Correspondência exata
            }
            if (entry.normalizedLocalized.startsWith(query) || entry.englishName.startsWith(query)) {
                return 1; // Início do nome
            }
            for (String word : entry.words) {
                if (word.startsWith(query)) {
                    return 2; // Início de palavra interna
                }
            }
            return 3; // Presente no texto
        }

        private static String resolveLocalizedName(Material material, Map<String, String> translations) {
            if (material.isLegacy()) {
                return formatMaterialName(material);
            }
            if (translations != null && !translations.isEmpty()) {
                String key;
                try {
                    key = material.getKey().getKey();
                } catch (Throwable ignored) {
                    key = material.name().toLowerCase(Locale.ROOT);
                }
                String name = null;

                // 1. material.translationKey() (Translatable do Paper/Adventure)
                try {
                    name = translations.get(material.translationKey());
                } catch (Throwable ignored) {
                }

                // 2. item.minecraft.<key>
                if (name == null || name.isBlank()) {
                    name = translations.get("item.minecraft." + key);
                }

                // 3. block.minecraft.<key>
                if (name == null || name.isBlank()) {
                    name = translations.get("block.minecraft." + key);
                }

                // 4. Chave direta
                if (name == null || name.isBlank()) {
                    name = translations.get(key);
                }

                if (name != null && !name.isBlank()) {
                    return name;
                }
            }

            return formatMaterialName(material);
        }

        private static String stripAccents(String input) {
            if (input == null || input.isEmpty()) {
                return "";
            }
            String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
            return normalized.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        }
    }

    private static final class MaterialEntry {
        final Material material;
        final String localizedName;
        final String englishName;
        final String normalizedLocalized;
        final String normalizedHaystack;
        final String[] words;

        MaterialEntry(Material material, String localizedName, String englishName,
                      String normalizedLocalized, String normalizedHaystack, String[] words) {
            this.material = material;
            this.localizedName = localizedName;
            this.englishName = englishName;
            this.normalizedLocalized = normalizedLocalized;
            this.normalizedHaystack = normalizedHaystack;
            this.words = words;
        }
    }
}