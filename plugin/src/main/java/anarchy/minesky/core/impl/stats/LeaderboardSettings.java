package anarchy.minesky.core.impl.stats;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.time.ZoneId;

public final class LeaderboardSettings {

    private final ZoneId weekZone;

    public LeaderboardSettings(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();
        this.weekZone = LeaderboardWeekKey.zone(config.getString("leaderboards.weekly.zone", "America/Sao_Paulo"));
    }

    public ZoneId weekZone() {
        return weekZone;
    }

    public String currentWeekKey() {
        return LeaderboardWeekKey.current(weekZone);
    }
}