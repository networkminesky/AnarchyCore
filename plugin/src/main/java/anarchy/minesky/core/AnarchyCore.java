package anarchy.minesky.core;

import anarchy.minesky.core.command.ConfigCommand;
import anarchy.minesky.core.command.AnarchyCoreCommand;
import anarchy.minesky.core.command.PingCommand;
import anarchy.minesky.core.command.ReplyCommand;
import anarchy.minesky.core.command.TellCommand;
import anarchy.minesky.core.command.TpaAcceptCommand;
import anarchy.minesky.core.command.TpaCommand;
import anarchy.minesky.core.command.TpaDenyCommand;
import anarchy.minesky.core.command.TpaHereCommand;
import anarchy.minesky.core.command.EditWarpCommand;
import anarchy.minesky.core.command.DelHomeCommand;
import anarchy.minesky.core.command.HomeCommand;
import anarchy.minesky.core.command.SetHomeCommand;
import anarchy.minesky.core.command.SetWarpCommand;
import anarchy.minesky.core.command.SpawnCommand;
import anarchy.minesky.core.command.WarpCommand;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.dialogapi.DialogManager;
import anarchy.minesky.core.impl.feedback.FeedbackListener;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.sounds.GlobalSoundSettings;
import anarchy.minesky.core.impl.sounds.UiSoundsAPIImpl;
import anarchy.minesky.core.impl.locator.LocatorManager;
import anarchy.minesky.core.impl.messaging.PrivateMessageService;
import anarchy.minesky.core.impl.tpa.TpaManager;
import anarchy.minesky.core.impl.tpa.TpaSettings;
import anarchy.minesky.core.impl.warps.WarpManager;
import anarchy.minesky.core.impl.warps.WarpSettings;
import anarchy.minesky.core.impl.warps.WarpStorage;
import anarchy.minesky.core.listener.ChatAntifloodListener;
import anarchy.minesky.core.impl.homes.HomeManager;
import anarchy.minesky.core.impl.homes.HomeRepository;
import anarchy.minesky.core.impl.homes.HomeSettings;
import anarchy.minesky.core.listener.HomeListener;
import anarchy.minesky.core.listener.WarpListener;
import anarchy.minesky.core.impl.database.DatabaseManager;
import anarchy.minesky.core.impl.database.DatabaseSettings;
import anarchy.minesky.core.impl.playerprefs.ConfigManager;
import anarchy.minesky.core.impl.playerprefs.PlayerConfigAPIImpl;
import anarchy.minesky.core.impl.playerprefs.PlayerPreferencesRepository;
import anarchy.minesky.core.impl.playerdata.PlayerDataAPIImpl;
import anarchy.minesky.core.impl.playerprefs.PlayerPreferencesService;
import anarchy.minesky.core.listener.PlayerPreferencesListener;
import anarchy.minesky.core.listener.PlayerEvents;
import anarchy.minesky.core.listener.PluginCommandBlockListener;
import anarchy.minesky.core.listener.TpaListener;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class AnarchyCore extends JavaPlugin {

    private MessagesManager messagesManager;
    private DialogManager dialogManager;
    private LocatorManager locatorManager;
    private FeedbackManager feedbackManager;
    private GlobalSoundSettings globalSoundSettings;
    private UiSoundsAPIImpl uiSoundsApi;
    private PrivateMessageService privateMessageService;
    private ChatAntifloodListener chatAntifloodListener;
    private TpaSettings tpaSettings;
    private TpaManager tpaManager;
    private WarpSettings warpSettings;
    private WarpStorage warpStorage;
    private WarpManager warpManager;
    private HomeSettings homeSettings;
    private HomeRepository homeRepository;
    private HomeManager homeManager;
    private DatabaseManager databaseManager;
    private PlayerPreferencesService playerPreferencesService;
    private PlayerConfigAPIImpl playerConfigApi;
    private PlayerDataAPIImpl playerDataApi;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messagesManager = new MessagesManager(this);
        this.tpaSettings = new TpaSettings(this);
        this.warpSettings = new WarpSettings(this);
        this.warpStorage = new WarpStorage(this);
        this.warpStorage.load();

        this.locatorManager = new LocatorManager(this);
        this.locatorManager.start();

        this.dialogManager = new DialogManager(this);
        this.dialogManager.start();

        this.globalSoundSettings = new GlobalSoundSettings(this);
        this.uiSoundsApi = new UiSoundsAPIImpl(this, globalSoundSettings);
        this.uiSoundsApi.register();

        this.feedbackManager = new FeedbackManager(this, uiSoundsApi);
        this.feedbackManager.start();
        getServer().getPluginManager().registerEvents(new FeedbackListener(feedbackManager), this);

        this.databaseManager = new DatabaseManager(this);
        DatabaseSettings databaseSettings = new DatabaseSettings(this);
        try {
            databaseManager.start(databaseSettings);
        } catch (Exception e) {
            getLogger().severe("Falha ao iniciar database: " + e.getMessage());
        }

        PlayerPreferencesRepository preferencesRepository = new PlayerPreferencesRepository(this, databaseManager);
        this.playerPreferencesService = new PlayerPreferencesService(preferencesRepository);
        this.playerConfigApi = new PlayerConfigAPIImpl(this, playerPreferencesService);
        this.playerConfigApi.register();
        this.playerDataApi = new PlayerDataAPIImpl(this, databaseManager);
        this.playerDataApi.register();
        this.configManager = new ConfigManager(
                this, messagesManager, feedbackManager, playerPreferencesService, uiSoundsApi);

        this.privateMessageService = new PrivateMessageService(
                messagesManager, feedbackManager, playerPreferencesService, configManager);
        this.chatAntifloodListener = new ChatAntifloodListener(
                this, messagesManager, feedbackManager, privateMessageService);
        getServer().getPluginManager().registerEvents(chatAntifloodListener, this);

        this.tpaManager = new TpaManager(this, messagesManager, feedbackManager, tpaSettings, playerPreferencesService);
        getServer().getPluginManager().registerEvents(new TpaListener(tpaManager), this);

        this.warpManager = new WarpManager(this, messagesManager, feedbackManager, warpSettings, warpStorage);
        getServer().getPluginManager().registerEvents(new WarpListener(warpManager), this);

        this.homeSettings = new HomeSettings(this);
        this.homeRepository = new HomeRepository(this, databaseManager, homeSettings);
        this.homeManager = new HomeManager(this, messagesManager, feedbackManager, homeSettings, homeRepository);
        getServer().getPluginManager().registerEvents(new HomeListener(homeManager), this);

        registerCommands();

        getServer().getPluginManager().registerEvents(new PlayerEvents(), this);
        getServer().getPluginManager().registerEvents(
                new PlayerPreferencesListener(this, playerPreferencesService, configManager, playerDataApi), this);
        getServer().getOnlinePlayers().forEach(p -> playerPreferencesService.loadPlayer(p.getUniqueId()));
        getServer().getPluginManager().registerEvents(new PluginCommandBlockListener(), this);
    }

    private void registerCommands() {
        PingCommand pingCommand = new PingCommand(messagesManager, feedbackManager);
        bind("ping", pingCommand, pingCommand);

        TellCommand tellCommand = new TellCommand(messagesManager, feedbackManager, privateMessageService);
        bind("tell", tellCommand, tellCommand);

        bind("r", new ReplyCommand(messagesManager, privateMessageService), null);

        AnarchyCoreCommand coreCommand = new AnarchyCoreCommand(this, messagesManager, feedbackManager);
        bind("anarchycore", coreCommand, coreCommand);

        TpaCommand tpaCommand = new TpaCommand(messagesManager, tpaManager);
        bind("tpa", tpaCommand, tpaCommand);

        TpaHereCommand tpaHereCommand = new TpaHereCommand(messagesManager, tpaManager);
        bind("tpaqui", tpaHereCommand, tpaHereCommand);

        TpaAcceptCommand tpaAcceptCommand = new TpaAcceptCommand(messagesManager, tpaManager);
        bind("tpaceitar", tpaAcceptCommand, tpaAcceptCommand);

        TpaDenyCommand tpaDenyCommand = new TpaDenyCommand(messagesManager, tpaManager);
        bind("tpanegar", tpaDenyCommand, tpaDenyCommand);

        WarpCommand warpCommand = new WarpCommand(messagesManager, warpManager);
        bind("warp", warpCommand, warpCommand);

        bind("spawn", new SpawnCommand(messagesManager, warpManager), null);

        SetWarpCommand setWarpCommand = new SetWarpCommand(messagesManager, warpManager);
        bind("setwarp", setWarpCommand, setWarpCommand);

        EditWarpCommand editWarpCommand = new EditWarpCommand(messagesManager, warpManager);
        bind("editwarp", editWarpCommand, editWarpCommand);

        HomeCommand homeCommand = new HomeCommand(messagesManager, homeManager);
        bind("home", homeCommand, homeCommand);

        SetHomeCommand setHomeCommand = new SetHomeCommand(messagesManager, homeManager);
        bind("sethome", setHomeCommand, setHomeCommand);

        DelHomeCommand delHomeCommand = new DelHomeCommand(messagesManager, homeManager);
        bind("delhome", delHomeCommand, delHomeCommand);

        bind("config", new ConfigCommand(messagesManager, configManager), null);
    }

    private void bind(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Comando '" + name + "' não definido em plugin.yml");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(tabCompleter);
    }

    public void reloadPlugin() {
        reloadConfig();
        messagesManager.reload();
        if (tpaSettings != null) {
            tpaSettings.reload();
        }
        if (warpSettings != null) {
            warpSettings.reload();
        }
        if (warpManager != null) {
            warpManager.reloadStorage();
        }
        if (homeSettings != null) {
            homeSettings.reload();
        }
        if (globalSoundSettings != null) {
            globalSoundSettings.reload(this);
        }
        if (feedbackManager != null) {
            feedbackManager.reloadConfig();
        }
        if (chatAntifloodListener != null) {
            chatAntifloodListener.reloadConfig();
        }
        if (locatorManager != null) {
            locatorManager.reloadConfig();
        }
    }

    public PlayerPreferencesService playerPreferences() {
        return playerPreferencesService;
    }

    public PlayerDataAPIImpl playerData() {
        return playerDataApi;
    }

    @Override
    public void onDisable() {
        if (uiSoundsApi != null) {
            uiSoundsApi.unregister();
            uiSoundsApi = null;
        }
        globalSoundSettings = null;
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
        tpaManager = null;
        tpaSettings = null;
        warpManager = null;
        warpStorage = null;
        warpSettings = null;
        homeManager = null;
        homeRepository = null;
        homeSettings = null;
        if (databaseManager != null) {
            getServer().getOnlinePlayers().forEach(p -> {
                if (playerPreferencesService != null) {
                    playerPreferencesService.unloadPlayer(p.getUniqueId());
                }
            });
            databaseManager.shutdown();
            databaseManager = null;
        }
        if (playerConfigApi != null) {
            playerConfigApi.unregister();
            playerConfigApi = null;
        }
        if (playerDataApi != null) {
            playerDataApi.unregister();
            playerDataApi = null;
        }
        playerPreferencesService = null;
        configManager = null;
    }
}
