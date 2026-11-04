package anarchy.minesky.core.utils;

public final class PlaytimeFormat {

    private PlaytimeFormat() {}

    /** {@link org.bukkit.Statistic#PLAY_ONE_MINUTE} — tempo de jogo em ticks (20 ticks = 1s). */
    public static String fromPlayTicks(int ticks) {
        long totalSeconds = Math.max(0L, ticks / 20L);
        long days = totalSeconds / 86_400L;
        long hours = (totalSeconds % 86_400L) / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;

        StringBuilder out = new StringBuilder();
        appendUnit(out, days, 'd');
        appendUnit(out, hours, 'h');
        appendUnit(out, minutes, 'm');
        appendUnit(out, seconds, 's');
        if (out.isEmpty()) {
            return "0s";
        }
        return out.toString().trim();
    }

    private static void appendUnit(StringBuilder out, long value, char unit) {
        if (value <= 0) {
            return;
        }
        if (!out.isEmpty()) {
            out.append(' ');
        }
        out.append(value).append(unit);
    }
}
