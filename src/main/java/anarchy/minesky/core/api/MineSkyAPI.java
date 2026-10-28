package anarchy.minesky.core.api;

import anarchy.minesky.core.api.dialogapi.DialogAPI;
import anarchy.minesky.core.api.locator.LocatorAPI;
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
        return DialogAPI.get() != null && LocatorAPI.get() != null;
    }

    public static LocatorAPI getLocator() {
        return LocatorAPI.get();
    }

    public static DialogAPI getDialog() {
        return DialogAPI.get();
    }

    public static DialogAPI dialog() { return DialogAPI.get(); }
    public static LocatorAPI locator() { return LocatorAPI.get(); }
}