package anarchy.minesky.core.impl.stats;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/**
 * Chave da semana no fuso configurado: data do domingo de início ({@code 2026-11-22}),
 * alinhada ao AnarchyClans (virada domingo 00:00).
 */
public final class LeaderboardWeekKey {

    private LeaderboardWeekKey() {}

    public static String current(ZoneId zone) {
        ZoneId safe = zone != null ? zone : ZoneId.of("America/Sao_Paulo");
        return forDate(LocalDate.now(safe), safe);
    }

    public static String forDate(LocalDate date, ZoneId zone) {
        if (date == null) {
            return current(zone);
        }
        LocalDate sunday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        return sunday.toString();
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