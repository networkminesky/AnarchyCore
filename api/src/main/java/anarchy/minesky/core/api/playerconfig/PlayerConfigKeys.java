package anarchy.minesky.core.api.playerconfig;

/**
 * Chaves estáveis das opções padrão registradas pelo AnarchyCore.
 */
public final class PlayerConfigKeys {

    public static final String CATEGORY_TPA = "anarchycore:tpa";
    public static final String CATEGORY_TELL = "anarchycore:tell";
    public static final String CATEGORY_SCOREBOARD = "anarchycore:scoreboard";

    public static final String TPA_FROM_STRANGERS = "anarchycore:tpa_strangers";
    public static final String TPAHERE_FROM_STRANGERS = "anarchycore:tpahere_strangers";
    /**
     * Reservado para plugins de economia registrarem no {@code /config} (não exposto pelo core).
     */
    public static final String PAY_FROM_STRANGERS = "anarchycore:pay_strangers";
    /**
     * Reservado para plugins de economia registrarem no {@code /config} (não exposto pelo core).
     */
    public static final String PAYMENT_SOUNDS = "anarchycore:payment_sounds";
    public static final String TELL_FROM_STRANGERS = "anarchycore:tell_strangers";
    public static final String TELL_NOTIFICATION_SOUNDS = "anarchycore:tell_sounds";
    public static final String SCOREBOARD_ENABLED = "anarchycore:scoreboard_enabled";

    /** Pref {@code /tpauto}; persistida junto ao snapshot mas fora do menu {@code /config}. */
    public static final String TPA_AUTO = "anarchycore:tpa_auto";

    private PlayerConfigKeys() {}
}
