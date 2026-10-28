package anarchy.minesky.core.api.playerconfig;

/**
 * Opções booleanas expostas no menu {@code /config} (preferências persistidas por jogador).
 */
public enum PlayerConfigOption {

    /** Aceitar pedidos {@code /tpa} de jogadores marcados como desconhecidos. */
    TPA_FROM_STRANGERS,

    /** Aceitar pedidos {@code /tpaqui} de desconhecidos. */
    TPAHERE_FROM_STRANGERS,

    /** Aceitar pagamentos de desconhecidos. */
    PAY_FROM_STRANGERS,

    /** Tocar sons ao receber pagamentos. */
    PAYMENT_SOUNDS,

    /** Aceitar {@code /tell} de desconhecidos. */
    TELL_FROM_STRANGERS,

    /** Tocar som ao receber mensagem privada. */
    TELL_NOTIFICATION_SOUNDS,

    /** Exibir scoreboard (quando integrado). */
    SCOREBOARD_ENABLED;

    public boolean read(PlayerConfigValues values) {
        return switch (this) {
            case TPA_FROM_STRANGERS -> values.allowTpaFromStrangers();
            case TPAHERE_FROM_STRANGERS -> values.allowTpaHereFromStrangers();
            case PAY_FROM_STRANGERS -> values.allowPayFromStrangers();
            case PAYMENT_SOUNDS -> values.paymentSounds();
            case TELL_FROM_STRANGERS -> values.allowTellFromStrangers();
            case TELL_NOTIFICATION_SOUNDS -> values.tellNotificationSounds();
            case SCOREBOARD_ENABLED -> values.scoreboardEnabled();
        };
    }
}
