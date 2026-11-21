package anarchy.minesky.core.impl.economy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Converte entradas do ranking do AnarchyEconomy em linhas ordenadas. */
public final class EconomyTopMapper {

    private EconomyTopMapper() {}

    public static List<AnarchyEconomyQueryProvider.EconomyRankEntry> map(
            List<EconomyTopRow> rows, int limit) {
        if (rows == null || rows.isEmpty() || limit <= 0) {
            return List.of();
        }
        List<AnarchyEconomyQueryProvider.EconomyRankEntry> out = new ArrayList<>();
        int max = Math.min(limit, rows.size());
        for (int i = 0; i < max; i++) {
            EconomyTopRow row = rows.get(i);
            if (row == null || row.uuid() == null || row.balance() <= 0) {
                continue;
            }
            String name = row.name() != null && !row.name().isBlank() ? row.name() : "-";
            out.add(new AnarchyEconomyQueryProvider.EconomyRankEntry(out.size() + 1, row.uuid(), name, row.balance()));
        }
        return List.copyOf(out);
    }

    public record EconomyTopRow(UUID uuid, String name, double balance) {}
}