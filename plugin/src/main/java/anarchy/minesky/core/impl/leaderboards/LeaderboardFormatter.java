package anarchy.minesky.core.impl.leaderboards;

import anarchy.minesky.core.impl.leaderboards.LeaderboardService.LeaderboardLine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Locale;

public final class LeaderboardFormatter {

    private static final double COMPACT_K = 1_000d;
    private static final double COMPACT_M = 1_000_000d;
    private static final double COMPACT_B = 1_000_000_000d;
    private static final double COMPACT_T = 1_000_000_000_000d;

    private LeaderboardFormatter() {}

    public static String stripLegacy(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("&[0-9a-fk-orA-FK-OR]", "").replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }

    public static String formatValue(LeaderboardCategory category, double value) {
        if (category != null && category.economy()) {
            return formatMoney(value);
        }
        if (category == LeaderboardCategory.PLAYTIME) {
            return formatPlaytime((long) value);
        }
        if (value == Math.rint(value)) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static String formatMoney(double value) {
        if (value >= 1_000_000) {
            return String.format(Locale.ROOT, "$%,.0f", value);
        }
        if (value == Math.rint(value)) {
            return String.format(Locale.ROOT, "$%.0f", value);
        }
        return String.format(Locale.ROOT, "$%,.2f", value);
    }

    public static String formatPlaytime(long totalSeconds) {
        return formatCompactPlaytime(totalSeconds);
    }

    public static String formatCompactPlaytime(long totalSeconds) {
        if (totalSeconds < 0) {
            totalSeconds = 0;
        }
        long days = totalSeconds / 86_400;
        if (days >= 1) {
            long hours = (totalSeconds % 86_400) / 3600;
            return days + "d " + hours + "h";
        }
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    public static String formatCompactNumber(double value) {
        double abs = Math.abs(value);
        if (abs >= COMPACT_T) {
            return formatSuffix(value / COMPACT_T, "T");
        }
        if (abs >= COMPACT_B) {
            return formatSuffix(value / COMPACT_B, "B");
        }
        if (abs >= COMPACT_M) {
            return formatSuffix(value / COMPACT_M, "M");
        }
        if (abs >= COMPACT_K) {
            return formatSuffix(value / COMPACT_K, "K");
        }
        if (value == Math.rint(value)) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatSuffix(double scaled, String suffix) {
        double rounded = Math.round(scaled * 10d) / 10d;
        if (rounded == Math.rint(rounded)) {
            return String.format(Locale.ROOT, "%.0f%s", rounded, suffix);
        }
        return String.format(Locale.ROOT, "%.1f%s", rounded, suffix);
    }

    public static String formatCompactMoneyValue(double value) {
        return "$ " + formatCompactNumber(value);
    }

    public static String formatCompactShardsValue(double value) {
        return "◆ " + formatCompactNumber(value);
    }

    public static Component compactValueComponent(LeaderboardCategory category, double value) {
        if (category == LeaderboardCategory.MONEY) {
            return Component.text("$ ", NamedTextColor.GREEN)
                    .append(Component.text(formatCompactNumber(value), NamedTextColor.WHITE));
        }
        if (category == LeaderboardCategory.SHARDS) {
            return Component.text("⚗ ", NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text(formatCompactNumber(value), NamedTextColor.WHITE));
        }
        if (category == LeaderboardCategory.PLAYTIME) {
            return Component.text(formatCompactPlaytime((long) value), NamedTextColor.WHITE);
        }
        return Component.text(formatCompactNumber(value), NamedTextColor.WHITE);
    }

    public static String formatLine(LeaderboardLine line, LeaderboardCategory category) {
        return "#" + line.position() + " " + line.name() + " — " + formatValue(category, line.value());
    }

    public static String periodToggleLabel(LeaderboardPeriod period) {
        return period == LeaderboardPeriod.WEEKLY ? "Ver Tudo" : "Ver Semanal";
    }

    public static String previewTitle(LeaderboardCategory category, LeaderboardPeriod period) {
        return "Top " + stripLegacy(category.title()) + " • " + period.titleSuffix();
    }

    public static String fullTitle(LeaderboardCategory category, LeaderboardPeriod period) {
        return "Top " + stripLegacy(category.title()) + " (Top 200 • " + period.titleSuffix() + ")";
    }

    public static int viewerRankOrMinusOne(java.util.List<LeaderboardLine> lines, java.util.UUID viewer) {
        if (viewer == null || lines == null) {
            return -1;
        }
        for (LeaderboardLine line : lines) {
            if (viewer.equals(line.uuid())) {
                return line.position();
            }
        }
        return -1;
    }
}