package anarchy.minesky.core.impl.dialogapi;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.DialogResponse;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.dialogapi.inputs.TextInput;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class MaterialPickerDialog {

    private static final String INPUT_QUERY = "query";
    private static final int MAX_RESULTS = 48;

    private final Plugin plugin;

    public MaterialPickerDialog(Plugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Component title, Consumer<Material> onSelected) {
        if (player == null || !player.isOnline() || onSelected == null) {
            return;
        }
        MineSkyDialog.builder(title)
                .body(Component.text("Digite parte do nome e clique em Procurar."))
                .addInput(TextInput.builder(INPUT_QUERY, Component.text("Buscar material"))
                        .placeholder("Ex: diamante, oak, bed")
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
        List<Material> matches = MaterialSearch.search(query, MAX_RESULTS);
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

    static Component materialButtonLabel(Material material) {
        return DialogComponentUtils.materialIconComponent(material)
                .append(Component.space())
                .append(Component.text(formatMaterialName(material)));
    }

    static String formatMaterialName(Material material) {
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    static final class MaterialSearch {

        private static final List<Material> CATALOG = java.util.Arrays.stream(Material.values())
                .filter(m -> !m.isAir() && (m.isItem() || m.isBlock()))
                .sorted(java.util.Comparator.comparing(Enum::name))
                .toList();

        private static final Map<String, String> PT_TO_EN = Map.ofEntries(
                Map.entry("diamante", "diamond"),
                Map.entry("ouro", "gold"),
                Map.entry("ferro", "iron"),
                Map.entry("esmeralda", "emerald"),
                Map.entry("netherite", "netherite"),
                Map.entry("madeira", "oak"),
                Map.entry("carvalho", "oak"),
                Map.entry("pedra", "stone"),
                Map.entry("cama", "bed"),
                Map.entry("bota", "boots"),
                Map.entry("capacete", "helmet"),
                Map.entry("peitoral", "chestplate"),
                Map.entry("calca", "leggings"),
                Map.entry("calça", "leggings"),
                Map.entry("espada", "sword"),
                Map.entry("picareta", "pickaxe"),
                Map.entry("machado", "axe"),
                Map.entry("pa", "shovel"),
                Map.entry("pá", "shovel"),
                Map.entry("bloco", "block"),
                Map.entry("vidro", "glass"),
                Map.entry("tijolo", "brick"),
                Map.entry("wool", "wool"),
                Map.entry("la", "wool"),
                Map.entry("lã", "wool")
        );

        private MaterialSearch() {
        }

        static List<Material> search(String rawQuery, int limit) {
            if (rawQuery == null || rawQuery.isBlank()) {
                return List.of();
            }
            String query = rawQuery.toLowerCase(Locale.ROOT).trim();
            String translated = PT_TO_EN.getOrDefault(query, query);
            String[] tokens = translated.split("\\s+");

            return CATALOG.stream()
                    .filter(m -> matchesTokens(m, tokens))
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        private static boolean matchesTokens(Material material, String[] tokens) {
            String haystack = (material.name() + " " + material.getKey().getKey()).toLowerCase(Locale.ROOT)
                    .replace('_', ' ');
            for (String token : tokens) {
                if (token.isEmpty()) {
                    continue;
                }
                if (!haystack.contains(token)) {
                    return false;
                }
            }
            return true;
        }
    }
}
