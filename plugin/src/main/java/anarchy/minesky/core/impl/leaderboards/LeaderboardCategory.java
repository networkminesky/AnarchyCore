package anarchy.minesky.core.impl.leaderboards;

import anarchy.minesky.core.impl.stats.PlayerStatsRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public enum LeaderboardCategory {
    MONEY("&fDinheiro", NamedTextColor.WHITE, true, false),
    SHARDS("&fFragmentos", NamedTextColor.WHITE, true, true),
    KILLS("&fKills", NamedTextColor.WHITE, false, false),
    DEATHS("&fMortes", NamedTextColor.WHITE, false, false),
    PLAYTIME("&fTempo Jogado", NamedTextColor.WHITE, false, false),
    BLOCKS_PLACED("&fBlocos Colocados", NamedTextColor.WHITE, false, false),
    BLOCKS_BROKEN("&fBlocos Quebrados", NamedTextColor.WHITE, false, false),
    MOBS_KILLED("&fMobs Mortos", NamedTextColor.WHITE, false, false);

    private final String labelLegacy;
    private final NamedTextColor color;
    private final boolean economy;
    private final boolean shards;

    LeaderboardCategory(String labelLegacy, NamedTextColor color, boolean economy, boolean shards) {
        this.labelLegacy = labelLegacy;
        this.color = color;
        this.economy = economy;
        this.shards = shards;
    }

    public Component buttonLabel() {
        return Component.text(LeaderboardFormatter.stripLegacy(labelLegacy), color);
    }

    public String title() {
        return LeaderboardFormatter.stripLegacy(labelLegacy);
    }

    public boolean economy() {
        return economy;
    }

    public boolean shards() {
        return shards;
    }

    /** Dinheiro e fragmentos usam ranking all-time da economia (sem semanal). */
    public boolean supportsWeekly() {
        return !economy;
    }

    public PlayerStatsRepository.SortColumn statsColumn() {
        return switch (this) {
            case KILLS -> PlayerStatsRepository.SortColumn.KILLS;
            case DEATHS -> PlayerStatsRepository.SortColumn.DEATHS;
            case PLAYTIME -> PlayerStatsRepository.SortColumn.PLAYTIME;
            case BLOCKS_PLACED -> PlayerStatsRepository.SortColumn.BLOCKS_PLACED;
            case BLOCKS_BROKEN -> PlayerStatsRepository.SortColumn.BLOCKS_BROKEN;
            case MOBS_KILLED -> PlayerStatsRepository.SortColumn.MOBS_KILLED;
            default -> null;
        };
    }
}