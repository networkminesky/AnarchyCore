package anarchy.minesky.core.api.playerconfig;

/**
 * Opções booleanas padrão do AnarchyCore ({@link PlayerConfigKeys}).
 *
 * @deprecated prefira {@link PlayerConfigBooleanOption} registrado ou {@link PlayerConfigKeys} + {@link PlayerConfigAPI#getBoolean(String, org.bukkit.entity.Player)}
 */
@Deprecated
public enum PlayerConfigOption {

    TPA_FROM_STRANGERS(PlayerConfigKeys.TPA_FROM_STRANGERS),
    TPAHERE_FROM_STRANGERS(PlayerConfigKeys.TPAHERE_FROM_STRANGERS),
    PAY_FROM_STRANGERS(PlayerConfigKeys.PAY_FROM_STRANGERS),
    PAYMENT_SOUNDS(PlayerConfigKeys.PAYMENT_SOUNDS),
    TELL_FROM_STRANGERS(PlayerConfigKeys.TELL_FROM_STRANGERS),
    TELL_NOTIFICATION_SOUNDS(PlayerConfigKeys.TELL_NOTIFICATION_SOUNDS),
    SCOREBOARD_ENABLED(PlayerConfigKeys.SCOREBOARD_ENABLED);

    private final String key;

    PlayerConfigOption(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public boolean read(PlayerConfigAPI api, org.bukkit.entity.Player player) {
        return api.getBoolean(key, player);
    }

    public boolean read(PlayerConfigAPI api, java.util.UUID playerId) {
        return api.getBoolean(key, playerId);
    }

    @Deprecated
    public boolean read(PlayerConfigValues values) {
        return values.getBoolean(key);
    }
}
