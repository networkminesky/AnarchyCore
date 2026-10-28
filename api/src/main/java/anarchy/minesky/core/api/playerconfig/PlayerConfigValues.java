package anarchy.minesky.core.api.playerconfig;

/**
 * Snapshot imutável das preferências do {@code /config} para um jogador.
 */
public record PlayerConfigValues(
        boolean allowTpaFromStrangers,
        boolean allowTpaHereFromStrangers,
        boolean allowPayFromStrangers,
        boolean paymentSounds,
        boolean allowTellFromStrangers,
        boolean tellNotificationSounds,
        boolean scoreboardEnabled
) {
}
