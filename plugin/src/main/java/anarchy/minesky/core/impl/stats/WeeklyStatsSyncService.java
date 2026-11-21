package anarchy.minesky.core.impl.stats;

import java.util.UUID;

/**
 * Atualiza {@code ac_player_stats_weekly} no join/quit/flush (virada de semana + ganhos semanais).
 */
public final class WeeklyStatsSyncService {

    private final PlayerStatsWeeklyRepository repository;
    private final LeaderboardSettings settings;

    public WeeklyStatsSyncService(PlayerStatsWeeklyRepository repository, LeaderboardSettings settings) {
        this.repository = repository;
        this.settings = settings;
    }

    public void sync(UUID uuid, String playerName, PlayerStatsRecord progression, int kills, int deaths) {
        if (!repository.isActive() || uuid == null || progression == null) {
            return;
        }
        String weekKey = settings.currentWeekKey();
        String name = playerName != null ? playerName : progression.playerName();
        PlayerStatsWeeklyRepository.WeeklyRow existing =
                repository.find(uuid, weekKey).orElse(null);

        int baselineKills;
        int baselineDeaths;
        long baselinePlaytime;
        int baselinePlaced;
        int baselineBroken;
        int baselineMobs;

        int scoreKills;
        int scoreDeaths;
        long scorePlaytime;
        int scorePlaced;
        int scoreBroken;
        int scoreMobs;

        if (existing == null) {
            baselineKills = kills;
            baselineDeaths = deaths;
            baselinePlaytime = progression.playtimeSeconds();
            baselinePlaced = progression.blocksPlaced();
            baselineBroken = progression.blocksBroken();
            baselineMobs = progression.mobsKilled();
            scoreKills = 0;
            scoreDeaths = 0;
            scorePlaytime = 0;
            scorePlaced = 0;
            scoreBroken = 0;
            scoreMobs = 0;
        } else {
            baselineKills = existing.baselineKills();
            baselineDeaths = existing.baselineDeaths();
            baselinePlaytime = existing.baselinePlaytimeSeconds();
            baselinePlaced = existing.baselineBlocksPlaced();
            baselineBroken = existing.baselineBlocksBroken();
            baselineMobs = existing.baselineMobsKilled();

            scoreKills = (int) WeeklyStatMath.monotonicWeekly(kills, baselineKills);
            scoreDeaths = (int) WeeklyStatMath.monotonicWeekly(deaths, baselineDeaths);
            scorePlaytime = WeeklyStatMath.monotonicWeekly(progression.playtimeSeconds(), baselinePlaytime);
            scorePlaced = (int) WeeklyStatMath.monotonicWeekly(progression.blocksPlaced(), baselinePlaced);
            scoreBroken = (int) WeeklyStatMath.monotonicWeekly(progression.blocksBroken(), baselineBroken);
            scoreMobs = (int) WeeklyStatMath.monotonicWeekly(progression.mobsKilled(), baselineMobs);
        }

        repository.saveSync(new PlayerStatsWeeklyRepository.WeeklyRow(
                uuid,
                weekKey,
                name,
                0,
                0,
                baselineKills,
                baselineDeaths,
                baselinePlaytime,
                baselinePlaced,
                baselineBroken,
                baselineMobs,
                0,
                0,
                scoreKills,
                scoreDeaths,
                scorePlaytime,
                scorePlaced,
                scoreBroken,
                scoreMobs));
    }
}