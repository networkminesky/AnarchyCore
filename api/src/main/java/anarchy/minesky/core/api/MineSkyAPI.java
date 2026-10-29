package anarchy.minesky.core.api;

import anarchy.minesky.core.api.dialogapi.DialogAPI;
import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.core.api.locator.LocatorAPI;
import anarchy.minesky.core.api.playerconfig.PlayerConfigAPI;
import anarchy.minesky.core.api.playerdata.PlayerDataAPI;
import anarchy.minesky.core.api.sounds.UiSoundsAPI;
import org.bukkit.Bukkit;

public final class MineSkyAPI {

    private static final String CORE_PLUGIN_NAME = "AnarchyCore";

    private MineSkyAPI() {}

    /**
     * {@code true} quando o AnarchyCore está habilitado e expõe as APIs via ServicesManager.
     * Use antes de chamar métodos da API após reload (PlugMan) ou em tarefas assíncronas.
     */
    public static boolean isAvailable() {
        if (!Bukkit.getPluginManager().isPluginEnabled(CORE_PLUGIN_NAME)) {
            return false;
        }
        return ServiceProviders.isRegistered(DialogAPI.class, "anarchy.minesky.core.api.dialogapi.DialogAPI")
                && ServiceProviders.isRegistered(LocatorAPI.class, "anarchy.minesky.core.api.locator.LocatorAPI")
                && ServiceProviders.isRegistered(FeedbackAPI.class, "anarchy.minesky.core.api.feedback.FeedbackAPI")
                && ServiceProviders.isRegistered(PlayerConfigAPI.class, "anarchy.minesky.core.api.playerconfig.PlayerConfigAPI")
                && ServiceProviders.isRegistered(PlayerDataAPI.class, "anarchy.minesky.core.api.playerdata.PlayerDataAPI")
                && ServiceProviders.isRegistered(UiSoundsAPI.class, UiSoundsAPI.SERVICE_NAME);
    }

    public static LocatorAPI getLocator() {
        return ServiceProviders.get(LocatorAPI.class, "anarchy.minesky.core.api.locator.LocatorAPI");
    }

    public static DialogAPI getDialog() {
        return ServiceProviders.get(DialogAPI.class, "anarchy.minesky.core.api.dialogapi.DialogAPI");
    }

    public static DialogAPI dialog() {
        return getDialog();
    }

    public static LocatorAPI locator() {
        return getLocator();
    }

    public static FeedbackAPI feedback() {
        return ServiceProviders.get(FeedbackAPI.class, "anarchy.minesky.core.api.feedback.FeedbackAPI");
    }

    public static PlayerConfigAPI playerConfig() {
        return PlayerConfigAPI.get();
    }

    public static PlayerDataAPI playerData() {
        return PlayerDataAPI.get();
    }

    public static UiSoundsAPI getUiSounds() {
        return UiSoundsAPI.get();
    }

    public static UiSoundsAPI uiSounds() {
        return getUiSounds();
    }
}