package anarchy.minesky.core.impl.leaderboards;

public enum LeaderboardPeriod {
    ALL_TIME,
    WEEKLY;

    public LeaderboardPeriod toggle() {
        return this == ALL_TIME ? WEEKLY : ALL_TIME;
    }

    public String titleSuffix() {
        return this == ALL_TIME ? "Tudo" : "Semanal";
    }
}