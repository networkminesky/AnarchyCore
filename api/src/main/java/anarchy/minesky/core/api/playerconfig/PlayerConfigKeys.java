package anarchy.minesky.core.api.playerconfig;

/**
 * Chaves estáveis das opções padrão registradas pelo AnarchyCore.
 */
public final class PlayerConfigKeys {

    public static final String CATEGORY_TPA = "anarchycore:tpa";
    public static final String CATEGORY_TELL = "anarchycore:tell";
    public static final String CATEGORY_FRIENDS = "anarchycore:friends";
    public static final String CATEGORY_CHAT = "anarchycore:chat";

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
    /** Aceitar receber pedidos de amizade (enviados por outros jogadores). */
    public static final String FRIENDS_ALLOW_INCOMING_REQUESTS = "anarchycore:friends_allow_incoming_requests";
    /** Som ao receber um pedido de amizade. */
    public static final String FRIENDS_REQUEST_SOUNDS = "anarchycore:friends_request_sounds";
    /** Aviso no feedback quando um amigo entra no servidor. */
    public static final String FRIENDS_NOTIFY_ON_FRIEND_JOIN = "anarchycore:friends_notify_on_friend_join";
    /** Som quando um amigo entra no servidor. */
    public static final String FRIENDS_JOIN_SOUNDS = "anarchycore:friends_join_sounds";
    public static final String CHAT_GLOBAL_ENABLED = "anarchycore:chat_global_enabled";
    public static final String CHAT_HEADS_ENABLED = "anarchycore:chat_heads_enabled";

    /** Pref {@code /tpauto}; persistida junto ao snapshot mas fora do menu {@code /config}. */
    public static final String TPA_AUTO = "anarchycore:tpa_auto";

    private PlayerConfigKeys() {}
}
