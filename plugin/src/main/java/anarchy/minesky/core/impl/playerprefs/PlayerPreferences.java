package anarchy.minesky.core.impl.playerprefs;

import anarchy.minesky.core.api.playerconfig.PlayerConfigKeys;

import java.util.HashMap;
import java.util.Map;

public final class PlayerPreferences {

    private final Map<String, Boolean> configBooleans = new HashMap<>();
    private boolean tpaAuto = false;

    public static PlayerPreferences defaults() {
        PlayerPreferences prefs = new PlayerPreferences();
        prefs.configBooleans.put(PlayerConfigKeys.TPA_FROM_STRANGERS, true);
        prefs.configBooleans.put(PlayerConfigKeys.TPAHERE_FROM_STRANGERS, true);
        prefs.configBooleans.put(PlayerConfigKeys.PAY_FROM_STRANGERS, true);
        prefs.configBooleans.put(PlayerConfigKeys.PAYMENT_SOUNDS, true);
        prefs.configBooleans.put(PlayerConfigKeys.TELL_FROM_STRANGERS, true);
        prefs.configBooleans.put(PlayerConfigKeys.TELL_NOTIFICATION_SOUNDS, true);
        prefs.configBooleans.put(PlayerConfigKeys.SCOREBOARD_ENABLED, true);
        return prefs;
    }

    public PlayerPreferences copy() {
        PlayerPreferences copy = new PlayerPreferences();
        copy.configBooleans.putAll(configBooleans);
        copy.tpaAuto = tpaAuto;
        return copy;
    }

    Map<String, Boolean> configBooleans() {
        return configBooleans;
    }

    void putConfigBooleanIfAbsent(String key, boolean value) {
        configBooleans.putIfAbsent(key, value);
    }

    boolean getConfigBoolean(String key, boolean defaultValue) {
        return configBooleans.getOrDefault(key, defaultValue);
    }

    void setConfigBoolean(String key, boolean value) {
        configBooleans.put(key, value);
    }

    public boolean allowTpaFromStrangers() {
        return getConfigBoolean(PlayerConfigKeys.TPA_FROM_STRANGERS, true);
    }

    public void setAllowTpaFromStrangers(boolean value) {
        setConfigBoolean(PlayerConfigKeys.TPA_FROM_STRANGERS, value);
    }

    public boolean allowTpaHereFromStrangers() {
        return getConfigBoolean(PlayerConfigKeys.TPAHERE_FROM_STRANGERS, true);
    }

    public void setAllowTpaHereFromStrangers(boolean value) {
        setConfigBoolean(PlayerConfigKeys.TPAHERE_FROM_STRANGERS, value);
    }

    public boolean allowPayFromStrangers() {
        return getConfigBoolean(PlayerConfigKeys.PAY_FROM_STRANGERS, true);
    }

    public void setAllowPayFromStrangers(boolean value) {
        setConfigBoolean(PlayerConfigKeys.PAY_FROM_STRANGERS, value);
    }

    public boolean paymentSounds() {
        return getConfigBoolean(PlayerConfigKeys.PAYMENT_SOUNDS, true);
    }

    public void setPaymentSounds(boolean value) {
        setConfigBoolean(PlayerConfigKeys.PAYMENT_SOUNDS, value);
    }

    public boolean allowTellFromStrangers() {
        return getConfigBoolean(PlayerConfigKeys.TELL_FROM_STRANGERS, true);
    }

    public void setAllowTellFromStrangers(boolean value) {
        setConfigBoolean(PlayerConfigKeys.TELL_FROM_STRANGERS, value);
    }

    public boolean tellNotificationSounds() {
        return getConfigBoolean(PlayerConfigKeys.TELL_NOTIFICATION_SOUNDS, true);
    }

    public void setTellNotificationSounds(boolean value) {
        setConfigBoolean(PlayerConfigKeys.TELL_NOTIFICATION_SOUNDS, value);
    }

    public boolean scoreboardEnabled() {
        return getConfigBoolean(PlayerConfigKeys.SCOREBOARD_ENABLED, true);
    }

    public void setScoreboardEnabled(boolean value) {
        setConfigBoolean(PlayerConfigKeys.SCOREBOARD_ENABLED, value);
    }

    public boolean tpaAuto() {
        return tpaAuto;
    }

    public void setTpaAuto(boolean tpaAuto) {
        this.tpaAuto = tpaAuto;
    }
}
