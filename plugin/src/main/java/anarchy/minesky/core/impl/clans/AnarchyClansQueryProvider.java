package anarchy.minesky.core.impl.clans;

import anarchy.minesky.core.impl.combat.CombatStatsRepository;
import anarchy.minesky.core.impl.leaderboards.LeaderboardService.LeaderboardLine;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abates/mortes oficiais do AnarchyClans ({@code PlayerDataManager}) via classloader do plugin.
 */
public final class AnarchyClansQueryProvider implements Listener {

    static final String PLUGIN_NAME = "AnarchyClans";
    private static final String CLANS_CLASS = "anarchy.minesky.AnarchyClans";
    private static final String MANAGER_CLASS = "anarchy.minesky.database.PlayerDataManager";
    private static final String CACHE_CLASS = "anarchy.minesky.database.PlayerStatsLeaderboardCache";
    private static final String BOARD_CLASS = CACHE_CLASS + "$Board";
    private static final String ENTRY_CLASS = CACHE_CLASS + "$Entry";

    private static final long STAT_TIMEOUT_MS = 8_000L;

    private final Plugin plugin;
    private final Logger logger;
    private final CombatStatsRepository legacyCombat;
    private volatile Binding binding;

    public AnarchyClansQueryProvider(Plugin plugin, CombatStatsRepository legacyCombat) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.legacyCombat = legacyCombat;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        tryBind();
    }

    public void shutdown() {
        binding = null;
    }

    @EventHandler
    public void onClansEnable(PluginEnableEvent event) {
        if (!PLUGIN_NAME.equals(event.getPlugin().getName())) {
            return;
        }
        clearBinding();
        tryBind();
    }

    @EventHandler
    public void onClansDisable(PluginDisableEvent event) {
        if (!PLUGIN_NAME.equals(event.getPlugin().getName())) {
            return;
        }
        clearBinding();
    }

    public boolean isClansAvailable() {
        Binding current = binding;
        if (current == null || !current.matchesEnabledPlugin()) {
            return false;
        }
        try {
            return current.playerData() != null;
        } catch (Throwable error) {
            return false;
        }
    }

    public boolean isActive() {
        return isClansAvailable() || (legacyCombat != null && legacyCombat.isActive());
    }

    public CompletableFuture<Integer> kills(UUID uuid) {
        return stat(uuid, true);
    }

    public CompletableFuture<Integer> deaths(UUID uuid) {
        return stat(uuid, false);
    }

    public int killsSync(UUID uuid) {
        if (isClansAvailable()) {
            try {
                return Math.max(0, (int) binding.getKillsSync.invoke(binding.playerData(), uuid));
            } catch (Throwable error) {
                logger.log(Level.FINE, "[Combat] getKillsSync falhou uuid=" + uuid, error);
            }
        }
        return legacyCombat != null && legacyCombat.isActive() ? legacyCombat.kills(uuid) : 0;
    }

    public int deathsSync(UUID uuid) {
        if (isClansAvailable()) {
            try {
                return Math.max(0, (int) binding.getDeathsSync.invoke(binding.playerData(), uuid));
            } catch (Throwable error) {
                logger.log(Level.FINE, "[Combat] getDeathsSync falhou uuid=" + uuid, error);
            }
        }
        return legacyCombat != null && legacyCombat.isActive() ? legacyCombat.deaths(uuid) : 0;
    }

    public List<LeaderboardLine> topKills(int limit) {
        return topBoard("ALLTIME_KILLS", limit);
    }

    public List<LeaderboardLine> topDeaths(int limit) {
        return topBoard("ALLTIME_DEATHS", limit);
    }

    public List<LeaderboardLine> topWeeklyKills(int limit) {
        return topBoard("WEEKLY_KILLS", limit);
    }

    public List<LeaderboardLine> topWeeklyDeaths(int limit) {
        return topBoard("WEEKLY_DEATHS", limit);
    }

    private static final int RANK_SCAN_LIMIT = 200;

    public int rankKills(UUID viewerId, int value) {
        return rankInTop(topKills(RANK_SCAN_LIMIT), viewerId, value);
    }

    public int rankDeaths(UUID viewerId, int value) {
        return rankInTop(topDeaths(RANK_SCAN_LIMIT), viewerId, value);
    }

    private int rankInTop(List<LeaderboardLine> top, UUID viewerId, int value) {
        if (value <= 0) {
            return -1;
        }
        for (LeaderboardLine line : top) {
            if (viewerId.equals(line.uuid())) {
                return line.position();
            }
        }
        return -1;
    }

    private CompletableFuture<Integer> stat(UUID uuid, boolean kills) {
        if (uuid == null) {
            return CompletableFuture.completedFuture(0);
        }
        Binding current = binding;
        if (current == null || !isClansAvailable()) {
            if (legacyCombat != null && legacyCombat.isActive()) {
                return CompletableFuture.completedFuture(kills ? legacyCombat.kills(uuid) : legacyCombat.deaths(uuid));
            }
            return CompletableFuture.completedFuture(0);
        }
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            MethodHandle callbackMethod = kills ? current.getKills : current.getDeaths;
            Consumer<Integer> callback = value -> future.complete(value != null ? Math.max(0, value) : 0);
            callbackMethod.invoke(current.playerData(), uuid, callback);
        } catch (Throwable error) {
            logger.log(Level.WARNING, "[Combat] Falha ao solicitar stat Clans uuid=" + uuid + " kills=" + kills, error);
            future.complete(legacyCombat != null && legacyCombat.isActive()
                    ? (kills ? legacyCombat.kills(uuid) : legacyCombat.deaths(uuid))
                    : 0);
        }
        return future
                .orTimeout(STAT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .exceptionally(error -> {
                    if (error instanceof TimeoutException || (error.getCause() instanceof TimeoutException)) {
                        logger.warning("[Combat] Timeout ao ler stat do AnarchyClans uuid=" + uuid);
                    } else {
                        logger.log(Level.WARNING, "[Combat] stat Clans falhou uuid=" + uuid, error);
                    }
                    return legacyCombat != null && legacyCombat.isActive()
                            ? (kills ? legacyCombat.kills(uuid) : legacyCombat.deaths(uuid))
                            : 0;
                });
    }

    private List<LeaderboardLine> topBoard(String boardConstant, int limit) {
        Binding current = binding;
        if (current == null || !isClansAvailable() || limit <= 0) {
            return fallbackTop(killsBoard(boardConstant), limit);
        }
        try {
            Object manager = current.playerData();
            Object cache = current.getLeaderboardCache.invoke(manager);
            if (cache == null) {
                return fallbackTop(killsBoard(boardConstant), limit);
            }
            @SuppressWarnings("unchecked")
            Map<Object, List<?>> boardMap = current.readCacheMap(cache);
            Object boardKey = current.boardConstant(boardConstant);
            List<?> raw = boardMap != null ? boardMap.get(boardKey) : null;
            List<AnarchyClansLeaderboardMapper.ClansLeaderboardEntry> mapped = mapEntries(raw, current);
            if (mapped.isEmpty()) {
                return fallbackTop(killsBoard(boardConstant), limit);
            }
            return AnarchyClansLeaderboardMapper.toLines(mapped, limit);
        } catch (Throwable error) {
            logger.log(Level.WARNING, "[Leaderboards] Top Clans falhou board=" + boardConstant, error);
            return fallbackTop(killsBoard(boardConstant), limit);
        }
    }

    private boolean killsBoard(String boardConstant) {
        return "ALLTIME_KILLS".equals(boardConstant);
    }

    private List<LeaderboardLine> fallbackTop(boolean kills, int limit) {
        if (legacyCombat == null || !legacyCombat.isActive()) {
            return List.of();
        }
        return kills ? legacyCombat.topKills(limit) : legacyCombat.topDeaths(limit);
    }

    private List<AnarchyClansLeaderboardMapper.ClansLeaderboardEntry> mapEntries(List<?> raw, Binding current)
            throws Throwable {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<AnarchyClansLeaderboardMapper.ClansLeaderboardEntry> out = new ArrayList<>(raw.size());
        for (Object entry : raw) {
            if (entry == null) {
                continue;
            }
            String nick = (String) current.entryNickname.invoke(entry);
            int value = (int) current.entryValue.invoke(entry);
            if (value <= 0) {
                continue;
            }
            out.add(new AnarchyClansLeaderboardMapper.ClansLeaderboardEntry(nick, value));
        }
        return List.copyOf(out);
    }

    private boolean tryBind() {
        Plugin clans = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (clans == null || !clans.isEnabled()) {
            return false;
        }
        try {
            ClassLoader loader = clans.getClass().getClassLoader();
            Class<?> clansClass = loader.loadClass(CLANS_CLASS);
            Class<?> managerClass = loader.loadClass(MANAGER_CLASS);
            Class<?> cacheClass = loader.loadClass(CACHE_CLASS);
            Class<?> boardClass = loader.loadClass(BOARD_CLASS);
            Class<?> entryClass = loader.loadClass(ENTRY_CLASS);

            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            MethodHandle getPlayerData = lookup.unreflect(clansClass.getMethod("getPlayerData"));
            MethodHandle getKills = lookup.unreflect(managerClass.getMethod("getKills", UUID.class, Consumer.class));
            MethodHandle getDeaths = lookup.unreflect(managerClass.getMethod("getDeaths", UUID.class, Consumer.class));
            MethodHandle getKillsSync = lookup.unreflect(managerClass.getMethod("getKillsSync", UUID.class));
            MethodHandle getDeathsSync = lookup.unreflect(managerClass.getMethod("getDeathsSync", UUID.class));
            MethodHandle getLeaderboardCache = lookup.unreflect(managerClass.getMethod("getLeaderboardCache"));
            MethodHandle entryNickname = lookup.unreflect(entryClass.getMethod("nickname"));
            MethodHandle entryValue = lookup.unreflect(entryClass.getMethod("value"));

            Field cacheField = cacheClass.getDeclaredField("cache");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) boardClass;
            MethodHandle cacheFieldGetter = lookup.unreflectGetter(cacheField);

            binding = new Binding(
                    clans,
                    getPlayerData,
                    getKills,
                    getDeaths,
                    getKillsSync,
                    getDeathsSync,
                    getLeaderboardCache,
                    cacheFieldGetter,
                    enumClass,
                    entryNickname,
                    entryValue);
            return true;
        } catch (Throwable error) {
            clearBinding();
            logger.log(Level.WARNING, "[Combat] Não foi possível vincular AnarchyClans", error);
            return false;
        }
    }

    private void clearBinding() {
        binding = null;
    }

    private record Binding(
            Plugin plugin,
            MethodHandle getPlayerData,
            MethodHandle getKills,
            MethodHandle getDeaths,
            MethodHandle getKillsSync,
            MethodHandle getDeathsSync,
            MethodHandle getLeaderboardCache,
            MethodHandle cacheFieldGetter,
            Class<? extends Enum<?>> boardClass,
            MethodHandle entryNickname,
            MethodHandle entryValue) {

        boolean matchesEnabledPlugin() {
            Plugin live = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            return live != null && live.isEnabled() && live == plugin;
        }

        Object playerData() throws Throwable {
            return getPlayerData.invoke(plugin);
        }

        @SuppressWarnings("unchecked")
        Map<Object, List<?>> readCacheMap(Object cache) throws Throwable {
            AtomicReference<Map<Object, List<?>>> ref =
                    (AtomicReference<Map<Object, List<?>>>) cacheFieldGetter.invoke(cache);
            Map<Object, List<?>> map = ref != null ? ref.get() : null;
            return map != null ? map : Map.of();
        }

        Object boardConstant(String name) {
            return Enum.valueOf((Class) boardClass, name);
        }
    }
}