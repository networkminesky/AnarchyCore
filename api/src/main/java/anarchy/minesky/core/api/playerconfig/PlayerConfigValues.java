package anarchy.minesky.core.api.playerconfig;

import java.util.Map;

/**
 * Snapshot imutável das preferências do {@code /config} para um jogador.
 */
public record PlayerConfigValues(Map<String, Boolean> booleans) {

    public boolean getBoolean(String key) {
        return booleans.getOrDefault(key, false);
    }

    public boolean allowTpaFromStrangers() {
        return getBoolean(PlayerConfigKeys.TPA_FROM_STRANGERS);
    }

    public boolean allowTpaHereFromStrangers() {
        return getBoolean(PlayerConfigKeys.TPAHERE_FROM_STRANGERS);
    }

    public boolean allowPayFromStrangers() {
        return getBoolean(PlayerConfigKeys.PAY_FROM_STRANGERS);
    }

    public boolean paymentSounds() {
        return getBoolean(PlayerConfigKeys.PAYMENT_SOUNDS);
    }

    public boolean allowTellFromStrangers() {
        return getBoolean(PlayerConfigKeys.TELL_FROM_STRANGERS);
    }

    public boolean tellNotificationSounds() {
        return getBoolean(PlayerConfigKeys.TELL_NOTIFICATION_SOUNDS);
    }

    public boolean scoreboardEnabled() {
        return getBoolean(PlayerConfigKeys.SCOREBOARD_ENABLED);
    }

    /**
     * Pref do comando {@code /tpauto} (não faz parte do menu {@code /config}).
     */
    public boolean tpaAuto() {
        return getBoolean(PlayerConfigKeys.TPA_AUTO);
    }
}
