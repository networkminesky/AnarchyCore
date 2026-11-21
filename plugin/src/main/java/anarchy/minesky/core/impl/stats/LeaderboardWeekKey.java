package anarchy.minesky.core.impl.stats;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.Locale;

/** Chave ISO de semana ({@code 2026-W47}) no fuso configurado. */
public final class LeaderboardWeekKey {

    private LeaderboardWeekKey() {}

    public static String current(ZoneId zone) {
        ZoneId safe = zone != null ? zone : ZoneId.of("America/Sao_Paulo");
        LocalDate today = LocalDate.now(safe);
        WeekFields fields = WeekFields.of(Locale.ROOT);
        int week = today.get(fields.weekOfWeekBasedYear());
        int year = today.get(fields.weekBasedYear());
        return String.format(Locale.ROOT, "%d-W%02d", year, week);
    }

    public static ZoneId zone(String configured) {
        if (configured == null || configured.isBlank()) {
            return ZoneId.of("America/Sao_Paulo");
        }
        try {
            return ZoneId.of(configured.trim());
        } catch (Exception ignored) {
            return ZoneId.of("America/Sao_Paulo");
        }
    }
}