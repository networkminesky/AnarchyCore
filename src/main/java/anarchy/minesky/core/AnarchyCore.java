package anarchy.minesky.core;

import anarchy.minesky.core.dialogapi.DialogManager;
import anarchy.minesky.core.listener.PlayerEvents;
import anarchy.minesky.core.locator.LocatorManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class AnarchyCore extends JavaPlugin {

    private DialogManager dialogManager;
    private LocatorManager locatorManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.locatorManager = new LocatorManager(this);
        this.locatorManager.start();

        this.dialogManager = new DialogManager(this);
        this.dialogManager.start();

        getServer().getPluginManager().registerEvents(new PlayerEvents(), this);
    }

    @Override
    public void onDisable() {
        if (dialogManager != null) {
            dialogManager.stop();
            dialogManager = null;
        }
        if (locatorManager != null) {
            locatorManager.stop();
            locatorManager = null;
        }
    }
}
