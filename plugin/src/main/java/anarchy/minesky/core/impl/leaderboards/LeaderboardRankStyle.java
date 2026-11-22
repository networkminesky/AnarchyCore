package anarchy.minesky.core.impl.leaderboards;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public final class LeaderboardRankStyle {

    public static final TextColor GOLD_FIRST = TextColor.color(0xFFD700);
    public static final TextColor BRONZE_THIRD = TextColor.color(0xFF8C00);
    public static final TextColor GRAY_RANK = TextColor.color(0xAAAAAA);

    private LeaderboardRankStyle() {}

    public static TextColor positionColor(int position) {
        return switch (position) {
            case 1 -> GOLD_FIRST;
            case 2 -> NamedTextColor.WHITE;
            case 3 -> BRONZE_THIRD;
            default -> GRAY_RANK;
        };
    }

    public static TextColor nameColor(int position) {
        return switch (position) {
            case 1 -> GOLD_FIRST;
            case 2 -> NamedTextColor.WHITE;
            case 3 -> BRONZE_THIRD;
            default -> NamedTextColor.WHITE;
        };
    }
}