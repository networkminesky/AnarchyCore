package anarchy.minesky.core.impl.dialogapi;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.DialogResponse;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.dialogapi.inputs.TextInput;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import anarchy.minesky.core.api.text.MaterialTranslationsAPI;
import anarchy.minesky.core.impl.text.MaterialTranslationsImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.text.Normalizer;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class MaterialPickerDialog {

    private static final String INPUT_QUERY = "query";
    private static final int MAX_RESULTS = 48;

    private final MaterialTranslationsAPI translations;
    private final MaterialSearch searchIndex;

    public MaterialPickerDialog(MaterialTranslationsAPI translations) {
        this.translations = translations;
        this.searchIndex = new MaterialSearch(translations);
    }

    public void reloadSearchIndex() {
        searchIndex.reload(translations);
    }

    public void open(Player player, Component title, Consumer<Material> onSelected) {
        open(player, title, null, onSelected);
    }

    public void open(Player player, Component title, String initialQuery, Consumer<Material> onSelected) {
        if (player == null || !player.isOnline() || onSelected == null) {
            return;
        }
        if (initialQuery != null && !initialQuery.isBlank()) {
            showResults(player, title, initialQuery.trim(), onSelected);
            return;
        }
        openSearchForm(player, title, "", onSelected);
    }

    private void openSearchForm(Player player, Component title, String defaultQuery, Consumer<Material> onSelected) {
        MineSkyDialog.builder(title)
                .body(Component.text("Digite parte do nome e clique em Procurar."))
                .addInput(TextInput.builder(INPUT_QUERY, Component.text("Buscar material"))
                        .placeholder("Ex: diamante, espada, apple, bed")
                        .initial(defaultQuery != null ? defaultQuery : "")
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
        showResults(player, title, response.getText(INPUT_QUERY, "").trim(), onSelected);
    }

    private void showResults(Player player, Component title, String query, Consumer<Material> onSelected) {
        List<Material> matches = searchIndex.search(query, MAX_RESULTS);
        if (matches.isEmpty()) {
            openSearchForm(player, title, query, onSelected);
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
                .onClick(p -> openSearchForm(p, title, "", onSelected))
                .build());

        builder.show(player);
    }

    Component materialButtonLabel(Material material) {
        String displayName = searchIndex.getDisplayName(material);
        String english = translations.getEnglishName(material);

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

    static final class MaterialSearch {

        private final List<MaterialEntry> entries = new ArrayList<>();
        private final Map<Material, String> displayNameCache = new EnumMap<>(Material.class);

        MaterialSearch(MaterialTranslationsAPI translations) {
            reload(translations);
        }

        void reload(MaterialTranslationsAPI translations) {
            entries.clear();
            displayNameCache.clear();

            for (Material material : Material.values()) {
                // Só itens obtíveis no jogo (mesmo critério de WarpStorage.parseMaterial).
                // Blocos sem forma de item (bolo com vela, fogo, placas de parede, etc.) não devem aparecer.
                if (material.isLegacy() || material.isAir() || !material.isItem()) {
                    continue;
                }
                String localized = translations.getPortugueseName(material);
                String english = translations.getEnglishName(material);
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
            return displayNameCache.getOrDefault(material, MaterialTranslationsImpl.formatEnglishName(material));
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