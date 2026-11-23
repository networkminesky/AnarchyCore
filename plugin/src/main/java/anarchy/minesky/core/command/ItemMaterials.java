package anarchy.minesky.core.command;

import anarchy.minesky.core.api.MineSkyAPI;
import org.bukkit.Material;
import org.bukkit.Registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Índice estático dos itens dáveis. Nomes são indexados com e sem underscore
 * para que prefixos curtos ({@code pap}, {@code diamondsword}) resolvam sem varrer
 * o registro a cada tab.
 */
final class ItemMaterials {

    static final int SUGGESTION_LIMIT = 40;

    private static final List<Entry> ENTRIES;
    private static final Map<String, Material> EXACT;

    static {
        List<Entry> entries = new ArrayList<>();
        Map<String, Material> exact = HashMap.newHashMap(512);
        for (Material material : Registry.MATERIAL) {
            if (material.isAir() || !material.isItem()) {
                continue;
            }
            String name = material.name().toLowerCase(Locale.ROOT);
            String compact = name.replace("_", "");
            entries.add(new Entry(material, name, compact));
            exact.put(name, material);
            exact.putIfAbsent(compact, material);
        }
        ENTRIES = List.copyOf(entries);
        EXACT = Map.copyOf(exact);
    }

    private ItemMaterials() {}

    enum Status {
        FOUND,
        AMBIGUOUS,
        MISSING
    }

    record Match(Status status, Material material) {
        static Match found(Material material) {
            return new Match(Status.FOUND, material);
        }

        static final Match AMBIGUOUS = new Match(Status.AMBIGUOUS, null);
        static final Match MISSING = new Match(Status.MISSING, null);
    }

    /**
     * Correspondência exata tem prioridade. Senão, aceita um único prefixo
     * (com ou sem underscore). Vários prefixos são ambiguidade.
     */
    static Match resolve(String raw) {
        String query = normalize(raw);
        if (query.isEmpty()) {
            return Match.MISSING;
        }
        Material exact = EXACT.get(query);
        if (exact != null) {
            return Match.found(exact);
        }

        Material prefix = null;
        boolean prefixAmbiguous = false;
        Material compact = null;
        boolean compactAmbiguous = false;
        for (Entry entry : ENTRIES) {
            if (!prefixAmbiguous && entry.name.startsWith(query)) {
                if (prefix != null) {
                    prefixAmbiguous = true;
                    prefix = null;
                } else {
                    prefix = entry.material;
                }
            } else if (!compactAmbiguous && entry.compact.startsWith(query)) {
                if (compact != null) {
                    compactAmbiguous = true;
                    compact = null;
                } else {
                    compact = entry.material;
                }
            }
            if (prefixAmbiguous && compactAmbiguous) {
                break;
            }
        }
        if (prefixAmbiguous) {
            return Match.AMBIGUOUS;
        }
        if (prefix != null) {
            return Match.found(prefix);
        }
        if (compact != null) {
            return Match.found(compact);
        }
        return compactAmbiguous ? Match.AMBIGUOUS : Match.MISSING;
    }

    static List<String> complete(String raw) {
        String query = normalize(raw);
        if (query.isEmpty()) {
            return List.of();
        }
        List<String> matches = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (entry.name.startsWith(query) || entry.compact.startsWith(query)) {
                matches.add(entry.name);
            }
        }
        matches.sort((left, right) -> {
            int byLength = Integer.compare(left.length(), right.length());
            return byLength != 0 ? byLength : left.compareTo(right);
        });
        if (matches.size() > SUGGESTION_LIMIT) {
            return List.copyOf(matches.subList(0, SUGGESTION_LIMIT));
        }
        return matches;
    }

    static String displayName(Material material) {
        String localized = MineSkyAPI.getPortugueseName(material);
        if (localized != null && !localized.isBlank()) {
            return localized;
        }
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String query = raw.toLowerCase(Locale.ROOT).trim();
        int colon = query.indexOf(':');
        if (colon >= 0) {
            query = query.substring(colon + 1);
        }
        return query.replace("_", "").replace(" ", "").replace("-", "");
    }

    private record Entry(Material material, String name, String compact) {}
}
