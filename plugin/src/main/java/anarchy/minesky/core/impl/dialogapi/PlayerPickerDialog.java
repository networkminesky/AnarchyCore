package anarchy.minesky.core.impl.dialogapi;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.DialogResponse;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.dialogapi.inputs.TextInput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Busca por nome entre jogadores online (filtrados por predicado), no estilo {@link MaterialPickerDialog}.
 */
public final class PlayerPickerDialog {

    private static final String INPUT_QUERY = "query";
    private static final int MAX_RESULTS = 48;

    public PlayerPickerDialog() {
    }

    public void open(
            Player viewer,
            Component title,
            Predicate<Player> visible,
            Consumer<String> onNickSelected) {
        open(viewer, title, visible, null, onNickSelected);
    }

    public void open(
            Player viewer,
            Component title,
            Predicate<Player> visible,
            String initialQuery,
            Consumer<String> onNickSelected) {
        if (viewer == null || !viewer.isOnline() || onNickSelected == null || visible == null) {
            return;
        }
        if (initialQuery != null && !initialQuery.isBlank()) {
            showResults(viewer, title, visible, initialQuery.trim(), onNickSelected);
            return;
        }
        openSearchForm(viewer, title, visible, "", onNickSelected);
    }

    private void openSearchForm(
            Player viewer,
            Component title,
            Predicate<Player> visible,
            String defaultQuery,
            Consumer<String> onNickSelected) {
        MineSkyDialog.builder(title)
                .body(Component.text("Digite parte do nick e clique em Procurar.", NamedTextColor.GRAY))
                .addInput(TextInput.builder(INPUT_QUERY, Component.text("Buscar jogador"))
                        .placeholder("Ex: nome, parte do nick")
                        .initial(defaultQuery != null ? defaultQuery : "")
                        .maxLength(16)
                        .build())
                .button(DialogButton.builder()
                        .label(Component.text("Procurar"))
                        .width(200)
                        .onClick((p, response) -> showResults(p, title, visible, response, onNickSelected))
                        .build())
                .button(DialogButton.builder()
                        .label(Component.text("Cancelar"))
                        .width(150)
                        .onClick(p -> { })
                        .build())
                .show(viewer);
    }

    private void showResults(Player viewer, Component title, Predicate<Player> visible, DialogResponse response,
                             Consumer<String> onNickSelected) {
        showResults(viewer, title, visible, response.getText(INPUT_QUERY, "").trim(), onNickSelected);
    }

    private void showResults(
            Player viewer,
            Component title,
            Predicate<Player> visible,
            String query,
            Consumer<String> onNickSelected) {
        List<PlayerEntry> matches = searchOnline(visible, query);
        if (matches.isEmpty()) {
            openSearchForm(viewer, title, visible, query, onNickSelected);
            return;
        }

        MineSkyDialog.Builder builder = MineSkyDialog.builder(title)
                .body(Component.text(
                        "Resultados: " + matches.size() + (matches.size() >= MAX_RESULTS ? "+" : ""),
                        NamedTextColor.GRAY))
                .multiActionColumns(2);

        for (PlayerEntry entry : matches) {
            builder.button(DialogButton.builder()
                    .label(Component.text(entry.name()))
                    .width(240)
                    .onClick(p -> onNickSelected.accept(entry.name()))
                    .build());
        }

        builder.button(DialogButton.builder()
                .label(Component.text("Nova busca"))
                .width(200)
                .onClick(p -> openSearchForm(p, title, visible, "", onNickSelected))
                .build());

        builder.show(viewer);
    }

    private List<PlayerEntry> searchOnline(Predicate<Player> visible, String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return List.of();
        }
        String normalizedQuery = stripAccents(rawQuery.trim());
        String[] tokens = normalizedQuery.split("\\s+");

        List<Player> candidates = Bukkit.getOnlinePlayers().stream()
                .filter(visible)
                .collect(Collectors.toList());

        List<PlayerEntry> entries = new ArrayList<>();
        for (Player p : candidates) {
            String name = p.getName();
            String normName = stripAccents(name);
            String haystack = (normName + " " + name.toLowerCase(Locale.ROOT)).trim();
            if (!matchesAllTokens(haystack, tokens)) {
                continue;
            }
            entries.add(new PlayerEntry(name, normName, matchScore(normName, normalizedQuery, tokens)));
        }

        entries.sort(Comparator
                .comparingInt(PlayerEntry::score)
                .thenComparing(e -> e.name().toLowerCase(Locale.ROOT)));
        return entries.stream().limit(MAX_RESULTS).toList();
    }

    private static boolean matchesAllTokens(String haystack, String[] tokens) {
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

    private static int matchScore(String normalizedName, String query, String[] tokens) {
        if (normalizedName.equals(query)) {
            return 0;
        }
        if (normalizedName.startsWith(query)) {
            return 1;
        }
        for (String token : tokens) {
            if (!token.isEmpty() && normalizedName.startsWith(token)) {
                return 2;
            }
        }
        return 3;
    }

    private static String stripAccents(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private record PlayerEntry(String name, String normalizedName, int score) {}
}
