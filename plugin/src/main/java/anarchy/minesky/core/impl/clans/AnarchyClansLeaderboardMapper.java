package anarchy.minesky.core.impl.clans;

import anarchy.minesky.core.impl.leaderboards.LeaderboardService.LeaderboardLine;
import org.bukkit.Bukkit;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Converte entradas do {@code PlayerStatsLeaderboardCache} em linhas do AnarchyCore. */
public final class AnarchyClansLeaderboardMapper {

    private AnarchyClansLeaderboardMapper() {}

    public static List<LeaderboardLine> toLines(List<ClansLeaderboardEntry> entries, int limit) {
        if (entries == null || entries.isEmpty() || limit <= 0) {
            return List.of();
        }
        int max = Math.min(limit, entries.size());
        List<LeaderboardLine> out = new ArrayList<>(max);
        for (int i = 0; i < max; i++) {
            ClansLeaderboardEntry row = entries.get(i);
            if (row == null || row.value() <= 0) {
                continue;
            }
            UUID uuid = resolveUuid(row.nickname());
            String name = row.nickname() != null && !row.nickname().isBlank() ? row.nickname() : "-";
            out.add(new LeaderboardLine(out.size() + 1, uuid, name, row.value()));
        }
        return List.copyOf(out);
    }

    public static UUID resolveUuid(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return new UUID(0L, 0L);
        }
        String clean = nickname.trim();
        if (Bukkit.getServer() != null) {
            try {
                return Bukkit.getOfflinePlayer(clean).getUniqueId();
            } catch (Throwable ignored) {
                // fallback abaixo (ex.: ambiente de teste sem servidor)
            }
        }
        return UUID.nameUUIDFromBytes(clean.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    public record ClansLeaderboardEntry(String nickname, int value) {}
}