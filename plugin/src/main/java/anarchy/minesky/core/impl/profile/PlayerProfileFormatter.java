package anarchy.minesky.core.impl.profile;

import anarchy.minesky.core.impl.leaderboards.LeaderboardFormatter;

import java.util.Locale;

public final class PlayerProfileFormatter {

    private PlayerProfileFormatter() {}

    public static String formatMoneyLine(double money) {
        return "&7Dinheiro: &a$ &f" + LeaderboardFormatter.formatCompactNumber(money);
    }

    public static String formatShardsLine(double shards) {
        if (shards == Math.rint(shards)) {
            return String.format(Locale.ROOT, "%.0f", shards);
        }
        return String.format(Locale.ROOT, "%.2f", shards);
    }

    public static String formatKillsLine(int kills) {
        return "&7Abates: &f" + LeaderboardFormatter.formatCompactNumber(kills);
    }

    public static String formatDeathsLine(int deaths) {
        return "&7Mortes: &f" + LeaderboardFormatter.formatCompactNumber(deaths);
    }

    public static String formatPlaytimeLine(long playtimeSeconds) {
        return "&7Tempo Jogado: &f" + LeaderboardFormatter.formatCompactPlaytime(playtimeSeconds);
    }

    public static String chestTitle(String nick) {
        if (nick == null || nick.isBlank()) {
            return "Stats";
        }
        return PlayerProfileGuiBuilder.toSmallCaps(nick) + " Stats";
    }
}