package anarchy.minesky.core.impl.dialogapi;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.DialogResponse;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.dialogapi.inputs.TextInput;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import anarchy.minesky.core.api.text.MaterialTranslationsAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.Player;

import java.text.Normalizer;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class MaterialPickerDialog {

    private static final String INPUT_QUERY = "query";
    private static final int MAX_RESULTS = 48;
    static final String PERMISSION_OPERATIVE = "anarchycore.materialpicker.operative";
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");

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
        List<MaterialSearch.MaterialEntry> matches = searchIndex.search(query, MAX_RESULTS, player);
        if (matches.isEmpty()) {
            openSearchForm(player, title, query, onSelected);
            return;
        }

        MineSkyDialog.Builder builder = MineSkyDialog.builder(title)
                .body(Component.text("Resultados: " + matches.size() + (matches.size() >= MAX_RESULTS ? "+" : "")))
                .multiActionColumns(2);

        for (MaterialSearch.MaterialEntry entry : matches) {
            builder.button(DialogButton.builder()
                    .label(entry.buttonLabel)
                    .width(240)
                    .onClick(p -> onSelected.accept(entry.material))
                    .build());
        }

        builder.button(DialogButton.builder()
                .label(Component.text("[+] Nova busca").color(TextColor.fromHexString("#7BED9F")))
                .width(200)
                .onClick(p -> openSearchForm(p, title, "", onSelected))
                .build());

        builder.show(player);
    }

    static boolean isOperativeMaterial(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        if (name.endsWith("_SPAWN_EGG")) {
            return true;
        }
        return switch (material) {
            case COMMAND_BLOCK, CHAIN_COMMAND_BLOCK, REPEATING_COMMAND_BLOCK,
                 JIGSAW, BARRIER, STRUCTURE_BLOCK, STRUCTURE_VOID,
                 DEBUG_STICK, LIGHT, TEST_INSTANCE_BLOCK, TEST_BLOCK,
                 KNOWLEDGE_BOOK -> true;
            default -> false;
        };
    }

    static final class MaterialSearch {

        private volatile List<MaterialEntry> entries = List.of();

        MaterialSearch(MaterialTranslationsAPI translations) {
            reload(translations);
        }

        void reload(MaterialTranslationsAPI translations) {
            List<MaterialEntry> next = new ArrayList<>();

            for (Material material : Registry.MATERIAL) {
                // Só itens obtíveis no jogo (mesmo critério de WarpStorage.parseMaterial).
                // Blocos sem forma de item (bolo com vela, fogo, placas de parede, etc.) não devem aparecer.
                if (material.isAir() || !material.isItem()) {
                    continue;
                }
                String localized = translations.getPortugueseName(material);
                String english = translations.getEnglishName(material);

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

                next.add(new MaterialEntry(
                        material,
                        localized,
                        english,
                        normLoc,
                        haystack,
                        words,
                        isOperativeMaterial(material),
                        buttonLabel(material, localized, english)));
            }
            entries = List.copyOf(next);
        }

        List<MaterialEntry> search(String rawQuery, int limit, Player player) {
            if (rawQuery == null || rawQuery.isBlank() || limit <= 0) {
                return List.of();
            }
            String normalizedQuery = stripAccents(rawQuery.trim());
            String[] tokens = normalizedQuery.split("\\s+");
            boolean allowOperative = player.hasPermission(PERMISSION_OPERATIVE);
            List<MaterialEntry> snapshot = entries;

            PriorityQueue<Ranked> best = new PriorityQueue<>(
                    Math.min(limit, 64),
                    (left, right) -> compareRank(right, left));

            for (MaterialEntry entry : snapshot) {
                if (entry.operative && !allowOperative) {
                    continue;
                }
                if (!matchesAllTokens(entry, tokens)) {
                    continue;
                }
                Ranked candidate = new Ranked(entry, matchScore(entry, normalizedQuery));
                if (best.size() < limit) {
                    best.add(candidate);
                    continue;
                }
                Ranked worst = best.peek();
                if (worst != null && compareRank(candidate, worst) < 0) {
                    best.poll();
                    best.add(candidate);
                }
            }
            if (best.isEmpty()) {
                return List.of();
            }
            List<Ranked> ranked = new ArrayList<>(best);
            ranked.sort(MaterialSearch::compareRank);
            List<MaterialEntry> result = new ArrayList<>(ranked.size());
            for (Ranked rankedEntry : ranked) {
                result.add(rankedEntry.entry);
            }
            return result;
        }

        private static int compareRank(Ranked left, Ranked right) {
            int score = Integer.compare(left.score, right.score);
            if (score != 0) {
                return score;
            }
            return String.CASE_INSENSITIVE_ORDER.compare(left.entry.localizedName, right.entry.localizedName);
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
            return DIACRITICS.matcher(normalized).replaceAll("").toLowerCase(Locale.ROOT);
        }

        private static Component buttonLabel(Material material, String displayName, String english) {
            Component label = DialogComponentUtils.materialIconComponent(material)
                    .append(Component.space())
                    .append(Component.text(displayName));
            if (!displayName.equalsIgnoreCase(english)) {
                label = label.hoverEvent(HoverEvent.showText(
                        Component.text(english, NamedTextColor.GRAY)));
            }
            return label;
        }

        private static final class Ranked {
            final MaterialEntry entry;
            final int score;

            Ranked(MaterialEntry entry, int score) {
                this.entry = entry;
                this.score = score;
            }
        }

        static final class MaterialEntry {
            final Material material;
            final String localizedName;
            final String englishName;
            final String normalizedLocalized;
            final String normalizedHaystack;
            final String[] words;
            final boolean operative;
            final Component buttonLabel;

            MaterialEntry(Material material, String localizedName, String englishName,
                          String normalizedLocalized, String normalizedHaystack, String[] words,
                          boolean operative, Component buttonLabel) {
                this.material = material;
                this.localizedName = localizedName;
                this.englishName = englishName;
                this.normalizedLocalized = normalizedLocalized;
                this.normalizedHaystack = normalizedHaystack;
                this.words = words;
                this.operative = operative;
                this.buttonLabel = buttonLabel;
            }
        }
    }
}
