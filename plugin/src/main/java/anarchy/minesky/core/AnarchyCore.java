package anarchy.minesky.core;

import anarchy.minesky.core.impl.dialogapi.DialogManager;
import anarchy.minesky.core.impl.feedback.FeedbackListener;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.locator.LocatorManager;
import anarchy.minesky.core.listener.PlayerEvents;
import org.bukkit.plugin.java.JavaPlugin;

public final class AnarchyCore extends JavaPlugin {

    private DialogManager dialogManager;
    private LocatorManager locatorManager;
    private FeedbackManager feedbackManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.locatorManager = new LocatorManager(this);
        this.locatorManager.start();

        this.dialogManager = new DialogManager(this);
        this.dialogManager.start();

        this.feedbackManager = new FeedbackManager(this);
        this.feedbackManager.start();
        getServer().getPluginManager().registerEvents(new FeedbackListener(feedbackManager), this);

        getServer().getPluginManager().registerEvents(new PlayerEvents(), this);
    }

    @Override
    public void onDisable() {
        if (feedbackManager != null) {
            feedbackManager.stop();
            feedbackManager = null;
        }
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
