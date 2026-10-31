package anarchy.minesky.core;

import anarchy.minesky.core.command.LegacyBukkitBasicCommand;
import anarchy.minesky.core.command.ConfigCommand;
import anarchy.minesky.core.command.AnarchyCoreCommand;
import anarchy.minesky.core.command.HealCommand;
import anarchy.minesky.core.command.NvCommand;
import anarchy.minesky.core.command.PingCommand;
import anarchy.minesky.core.command.ReplyCommand;
import anarchy.minesky.core.command.TellCommand;
import anarchy.minesky.core.command.TpAutoCommand;
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
import anarchy.minesky.core.impl.playerprefs.EscPauseMenuSupport;
import anarchy.minesky.core.impl.playerprefs.PlayerConfigAPIImpl;
import anarchy.minesky.core.impl.playerprefs.PlayerPreferencesRepository;
import anarchy.minesky.core.impl.playerdata.PlayerDataAPIImpl;
import anarchy.minesky.core.impl.playerprefs.PlayerPreferencesService;
import anarchy.minesky.core.listener.PlayerPreferencesListener;
import anarchy.minesky.core.listener.PlayerEvents;
import anarchy.minesky.core.listener.PluginCommandBlockListener;
import anarchy.minesky.core.command.GuardCommandHandler;
import anarchy.minesky.core.impl.guard.GuardJoinListener;
import anarchy.minesky.core.impl.guard.GuardManager;
import anarchy.minesky.core.impl.guard.GuardPacketListener;
import anarchy.minesky.core.impl.guard.GuardPlayerSnapshot;
import anarchy.minesky.core.impl.guard.GuardRepository;
import anarchy.minesky.core.impl.guard.GuardSettings;
import anarchy.minesky.core.listener.TpaListener;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import com.github.retrooper.packetevents.PacketEvents;

import java.util.List;

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
    private GuardSettings guardSettings;
    private GuardManager guardManager;
    private GuardCommandHandler guardCommandHandler;
    private GuardPacketListener guardPacketListener;

    @Override
    public void onEnable() {
        EscPauseMenuSupport.logRegistryStatus(this);
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
        this.playerConfigApi.registerCoreDefaults(messagesManager);
        this.playerConfigApi.register();
        this.playerDataApi = new PlayerDataAPIImpl(this, databaseManager);
        this.playerDataApi.register();
        this.configManager = new ConfigManager(
                messagesManager, feedbackManager, playerPreferencesService, playerConfigApi.registry(), uiSoundsApi);

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

        this.guardSettings = new GuardSettings(this);
        GuardRepository guardRepository = new GuardRepository(this, databaseManager, guardSettings);
        GuardPlayerSnapshot guardSnapshots = new GuardPlayerSnapshot();
        this.guardManager = new GuardManager(this, guardSettings, guardRepository, guardSnapshots);
        this.guardPacketListener = new GuardPacketListener(this, guardManager);
        if (PacketEvents.getAPI() == null) {
            getLogger().severe("PacketEvents API indisponível — AnarchyGuard desativado. Instale o plugin packetevents.");
        } else {
            PacketEvents.getAPI().getEventManager().registerListener(guardPacketListener);
        }
        getServer().getPluginManager().registerEvents(new GuardJoinListener(guardManager), this);
        this.guardCommandHandler = new GuardCommandHandler(guardManager, messagesManager, feedbackManager);

        registerCommands();

        getServer().getPluginManager().registerEvents(new PlayerEvents(), this);
        getServer().getPluginManager().registerEvents(
                new PlayerPreferencesListener(this, playerPreferencesService, configManager, playerDataApi, tpaManager), this);
        getServer().getOnlinePlayers().forEach(p -> playerPreferencesService.loadPlayer(p.getUniqueId()));
        getServer().getPluginManager().registerEvents(new PluginCommandBlockListener(), this);
    }

    private void registerCommands() {
        PingCommand pingCommand = new PingCommand(messagesManager, feedbackManager);
        bind("ping", "Mostra seu ping ou o de outro jogador", List.of(), pingCommand, pingCommand);

        HealCommand healCommand = new HealCommand(messagesManager, feedbackManager);
        bind("heal", "Restaura sua vida ou a de outro jogador", List.of(), healCommand, healCommand);

        NvCommand nvCommand = new NvCommand(messagesManager, feedbackManager);
        bind("nv", "Ativa ou desativa visão noturna", List.of(), nvCommand, nvCommand);

        TellCommand tellCommand = new TellCommand(messagesManager, feedbackManager, privateMessageService);
        bind("tell", "Envia mensagem privada", List.of("msg", "w", "whisper"), tellCommand, tellCommand);

        bind("r", "Responde a última mensagem privada", List.of("reply"), new ReplyCommand(messagesManager, privateMessageService), null);

        AnarchyCoreCommand coreCommand = new AnarchyCoreCommand(this, messagesManager, feedbackManager, guardCommandHandler);
        bind("anarchycore", "Comandos administrativos do AnarchyCore", List.of("acore", "minesky"), coreCommand, coreCommand);

        TpaCommand tpaCommand = new TpaCommand(messagesManager, tpaManager);
        bind("tpa", "Pede teleporte até um jogador", List.of(), tpaCommand, tpaCommand);

        TpaHereCommand tpaHereCommand = new TpaHereCommand(messagesManager, tpaManager);
        bind("tpaqui", "Pede para puxar um jogador até você", List.of("tpahere"), tpaHereCommand, tpaHereCommand);

        TpaAcceptCommand tpaAcceptCommand = new TpaAcceptCommand(messagesManager, tpaManager);
        bind("tpaceitar", "Aceita um pedido de TPA", List.of("tpaccept"), tpaAcceptCommand, tpaAcceptCommand);

        TpaDenyCommand tpaDenyCommand = new TpaDenyCommand(messagesManager, tpaManager);
        bind("tpanegar", "Nega um pedido de TPA", List.of("tpdeny"), tpaDenyCommand, tpaDenyCommand);

        TpAutoCommand tpAutoCommand = new TpAutoCommand(messagesManager, tpaManager);
        bind("tpauto", "Aceita pedidos de TPA automaticamente", List.of(), tpAutoCommand, tpAutoCommand);

        WarpCommand warpCommand = new WarpCommand(messagesManager, warpManager);
        bind("warp", "Lista ou teleporta para uma warp", List.of("warps"), warpCommand, warpCommand);

        bind("spawn", "Teleporta para a warp spawn", List.of(), new SpawnCommand(messagesManager, warpManager), null);

        SetWarpCommand setWarpCommand = new SetWarpCommand(messagesManager, warpManager);
        bind("setwarp", "Cria uma warp na sua posição", List.of(), setWarpCommand, setWarpCommand);

        EditWarpCommand editWarpCommand = new EditWarpCommand(messagesManager, warpManager);
        bind("editwarp", "Edita nome e ícone de uma warp", List.of(), editWarpCommand, editWarpCommand);

        HomeCommand homeCommand = new HomeCommand(messagesManager, homeManager);
        bind("home", "Lista ou teleporta para uma home", List.of("homes"), homeCommand, homeCommand);

        SetHomeCommand setHomeCommand = new SetHomeCommand(messagesManager, homeManager);
        bind("sethome", "Cria ou atualiza uma home", List.of(), setHomeCommand, setHomeCommand);

        DelHomeCommand delHomeCommand = new DelHomeCommand(messagesManager, homeManager);
        bind("delhome", "Remove uma home", List.of(), delHomeCommand, delHomeCommand);

        bind(
                "config",
                "Abre suas preferências MineSky",
                List.of("configurar", "personalizar", "preferencias", "settings"),
                new ConfigCommand(messagesManager, configManager),
                null);
    }

    private void bind(
            String name,
            String description,
            List<String> aliases,
            CommandExecutor executor,
            TabCompleter tabCompleter) {
        registerCommand(name, description, aliases, new LegacyBukkitBasicCommand(name, executor, tabCompleter));
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
        if (guardSettings != null) {
            guardSettings.reload();
        }
    }

    public GuardManager guardManager() {
        return guardManager;
    }

    public PlayerPreferencesService playerPreferences() {
        return playerPreferencesService;
    }

    public PlayerDataAPIImpl playerData() {
        return playerDataApi;
    }

    @Override
    public void onDisable() {
        if (guardManager != null) {
            guardManager.shutdown();
            guardManager = null;
        }
        if (guardPacketListener != null && PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(guardPacketListener);
            guardPacketListener = null;
        }
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
        if (tpaManager != null) {
            tpaManager.shutdown();
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
            if (playerPreferencesService != null) {
                getServer().getOnlinePlayers().forEach(p -> playerPreferencesService.unloadPlayer(p.getUniqueId()));
                playerPreferencesService.flushAllCachedSync();
            }
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
