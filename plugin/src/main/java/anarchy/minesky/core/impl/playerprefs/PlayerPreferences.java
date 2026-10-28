package anarchy.minesky.core.impl.playerprefs;

import java.util.UUID;

public final class PlayerPreferences {

    private boolean allowTpaFromStrangers = true;
    private boolean allowTpaHereFromStrangers = true;
    private boolean allowPayFromStrangers = true;
    private boolean paymentSounds = true;
    private boolean allowTellFromStrangers = true;
    private boolean tellNotificationSounds = true;
    private boolean scoreboardEnabled = true;

    public static PlayerPreferences defaults() {
        return new PlayerPreferences();
    }

    public PlayerPreferences copy() {
        PlayerPreferences copy = new PlayerPreferences();
        copy.allowTpaFromStrangers = allowTpaFromStrangers;
        copy.allowTpaHereFromStrangers = allowTpaHereFromStrangers;
        copy.allowPayFromStrangers = allowPayFromStrangers;
        copy.paymentSounds = paymentSounds;
        copy.allowTellFromStrangers = allowTellFromStrangers;
        copy.tellNotificationSounds = tellNotificationSounds;
        copy.scoreboardEnabled = scoreboardEnabled;
        return copy;
    }

    public boolean allowTpaFromStrangers() {
        return allowTpaFromStrangers;
    }

    public void setAllowTpaFromStrangers(boolean allowTpaFromStrangers) {
        this.allowTpaFromStrangers = allowTpaFromStrangers;
    }

    public boolean allowTpaHereFromStrangers() {
        return allowTpaHereFromStrangers;
    }

    public void setAllowTpaHereFromStrangers(boolean allowTpaHereFromStrangers) {
        this.allowTpaHereFromStrangers = allowTpaHereFromStrangers;
    }

    public boolean allowPayFromStrangers() {
        return allowPayFromStrangers;
    }

    public void setAllowPayFromStrangers(boolean allowPayFromStrangers) {
        this.allowPayFromStrangers = allowPayFromStrangers;
    }

    public boolean paymentSounds() {
        return paymentSounds;
    }

    public void setPaymentSounds(boolean paymentSounds) {
        this.paymentSounds = paymentSounds;
    }

    public boolean allowTellFromStrangers() {
        return allowTellFromStrangers;
    }

    public void setAllowTellFromStrangers(boolean allowTellFromStrangers) {
        this.allowTellFromStrangers = allowTellFromStrangers;
    }

    public boolean tellNotificationSounds() {
        return tellNotificationSounds;
    }

    public void setTellNotificationSounds(boolean tellNotificationSounds) {
        this.tellNotificationSounds = tellNotificationSounds;
    }

    public boolean scoreboardEnabled() {
        return scoreboardEnabled;
    }

    public void setScoreboardEnabled(boolean scoreboardEnabled) {
        this.scoreboardEnabled = scoreboardEnabled;
    }
}
