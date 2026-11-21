package anarchy.minesky.core.impl.economy;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Consulta saldos e tops do AnarchyEconomy via classloader do plugin (sem import direto).
 */
public final class AnarchyEconomyQueryProvider implements Listener {

    static final String PLUGIN_NAME = "AnarchyEconomy";
    private static final String API_CLASS = "anarchy.minesky.economy.api.AnarchyEconomyAPI";
    private static final String ECONOMY_CLASS = "anarchy.minesky.economy.api.EconomyAPI";
    private static final String CURRENCY_CLASS = "anarchy.minesky.economy.api.model.CurrencyType";
    private static final String BALANCE_ENTRY_CLASS = "anarchy.minesky.economy.api.model.BalanceEntry";

    private final Plugin plugin;
    private final Logger logger;
    private volatile Binding binding;

    public AnarchyEconomyQueryProvider(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        tryBind();
    }

    public void shutdown() {
        binding = null;
    }

    @EventHandler
    public void onEconomyEnable(PluginEnableEvent event) {
        if (!PLUGIN_NAME.equals(event.getPlugin().getName())) {
            return;
        }
        clearBinding();
        tryBind();
    }

    @EventHandler
    public void onEconomyDisable(PluginDisableEvent event) {
        if (!PLUGIN_NAME.equals(event.getPlugin().getName())) {
            return;
        }
        clearBinding();
    }

    public boolean isAvailable() {
        Binding current = binding;
        if (current == null || !current.matchesEnabledPlugin()) {
            return false;
        }
        try {
            if (!Boolean.TRUE.equals(current.isAvailable.invoke())) {
                return false;
            }
            return current.economy.invoke() != null;
        } catch (Throwable error) {
            return false;
        }
    }

    public CompletableFuture<Double> balanceMoney(UUID uuid) {
        return balance(uuid, false);
    }

    public CompletableFuture<Double> balanceShards(UUID uuid) {
        return balance(uuid, true);
    }

    public CompletableFuture<List<EconomyRankEntry>> topMoney(int limit) {
        return top(false, limit);
    }

    public CompletableFuture<List<EconomyRankEntry>> topShards(int limit) {
        return top(true, limit);
    }

    private CompletableFuture<Double> balance(UUID uuid, boolean shards) {
        Binding current = binding;
        if (current == null || !isAvailable() || uuid == null) {
            return CompletableFuture.completedFuture(0.0);
        }
        try {
            Object api = current.economy.invoke();
            Object raw = shards
                    ? current.getBalanceCurrency.invoke(api, uuid, current.shards)
                    : current.getBalanceUuid.invoke(api, uuid);
            if (!(raw instanceof CompletableFuture<?> future)) {
                return CompletableFuture.completedFuture(0.0);
            }
            return future.thenApply(value -> value instanceof Number number ? number.doubleValue() : 0.0);
        } catch (Throwable error) {
            logger.log(Level.WARNING, "[Leaderboards] balance falhou uuid=" + uuid, error);
            return CompletableFuture.completedFuture(0.0);
        }
    }

    private CompletableFuture<List<EconomyRankEntry>> top(boolean shards, int limit) {
        Binding current = binding;
        if (current == null || !isAvailable()) {
            return CompletableFuture.completedFuture(List.of());
        }
        int safeLimit = Math.max(1, Math.min(limit, 500));
        try {
            Object api = current.economy.invoke();
            Object raw = shards
                    ? invokeTopShards(current, api, safeLimit)
                    : current.topBalances.invoke(api);
            if (!(raw instanceof CompletableFuture<?> future)) {
                logger.warning("[Leaderboards] top não retornou CompletableFuture shards=" + shards);
                return CompletableFuture.completedFuture(List.of());
            }
            return future.thenApply(list -> parseTop(list, safeLimit))
                    .exceptionally(error -> {
                        logger.log(Level.WARNING, "[Leaderboards] top falhou shards=" + shards, error);
                        return List.of();
                    });
        } catch (Throwable error) {
            logger.log(Level.WARNING, "[Leaderboards] top indisponível shards=" + shards, error);
            return CompletableFuture.completedFuture(List.of());
        }
    }

    private Object invokeTopShards(Binding current, Object api, int limit) throws Throwable {
        if (current.topShards == null) {
            logger.warning("[Leaderboards] EconomyAPI sem topShards(int); atualize AnarchyEconomy API >= 1.0.3");
            return CompletableFuture.completedFuture(List.of());
        }
        return current.topShards.invoke(api, limit);
    }

    private List<EconomyRankEntry> parseTop(Object rawList, int limit) {
        if (!(rawList instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        Binding current = binding;
        if (current == null) {
            return List.of();
        }
        List<EconomyTopMapper.EconomyTopRow> rows = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Object entry = list.get(i);
            if (entry == null) {
                continue;
            }
            try {
                UUID uuid = (UUID) current.entryUuid.invoke(entry);
                String name = (String) current.entryUsername.invoke(entry);
                Object balanceRaw = current.entryBalance.invoke(entry);
                double bal = balanceRaw instanceof Number number ? number.doubleValue() : 0.0;
                if (uuid == null) {
                    logger.warning("[Leaderboards] entrada do top sem UUID (índice " + i + ")");
                    continue;
                }
                rows.add(new EconomyTopMapper.EconomyTopRow(uuid, name, bal));
            } catch (Throwable parseError) {
                logger.log(Level.WARNING, "[Leaderboards] falha ao interpretar entrada do top (índice " + i + ")", parseError);
            }
        }
        return EconomyTopMapper.map(rows, limit);
    }

    private boolean tryBind() {
        Plugin economy = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (economy == null || !economy.isEnabled()) {
            return false;
        }
        try {
            ClassLoader loader = economy.getClass().getClassLoader();
            Class<?> apiClass = loader.loadClass(API_CLASS);
            Class<?> economyClass = loader.loadClass(ECONOMY_CLASS);
            Class<?> currencyClass = loader.loadClass(CURRENCY_CLASS);
            Class<?> entryClass = loader.loadClass(BALANCE_ENTRY_CLASS);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            MethodHandle isAvailable = lookup.unreflect(apiClass.getMethod("isAvailable"));
            MethodHandle economyMethod = lookup.unreflect(apiClass.getMethod("economy"));
            MethodHandle getBalanceUuid = lookup.unreflect(economyClass.getMethod("getBalance", UUID.class));
            MethodHandle getBalanceCurrency = lookup.unreflect(
                    economyClass.getMethod("getBalance", UUID.class, currencyClass));
            MethodHandle topBalances = lookup.unreflect(economyClass.getMethod("topBalances"));
            MethodHandle topShards = null;
            try {
                topShards = lookup.unreflect(economyClass.getMethod("topShards", int.class));
            } catch (NoSuchMethodException missing) {
                logger.warning("[Leaderboards] EconomyAPI sem topShards(int) — fragmentos no ranking ficarão vazios");
            }
            Object money = currencyClass.getField("MONEY").get(null);
            Object shards = currencyClass.getField("SHARDS").get(null);
            MethodHandle entryUuid = lookup.unreflect(entryClass.getMethod("uuid"));
            MethodHandle entryUsername = lookup.unreflect(entryClass.getMethod("username"));
            MethodHandle entryBalance = lookup.unreflect(entryClass.getMethod("balance"));
            binding = new Binding(
                    economy,
                    isAvailable,
                    economyMethod,
                    getBalanceUuid,
                    getBalanceCurrency,
                    topBalances,
                    topShards,
                    money,
                    shards,
                    entryUuid,
                    entryUsername,
                    entryBalance);
            return true;
        } catch (Throwable error) {
            clearBinding();
            logger.log(Level.WARNING, "[Leaderboards] Não foi possível vincular AnarchyEconomy", error);
            return false;
        }
    }

    private void clearBinding() {
        binding = null;
    }

    public record EconomyRankEntry(int position, UUID uuid, String name, double value) {}

    private record Binding(
            Plugin plugin,
            MethodHandle isAvailable,
            MethodHandle economy,
            MethodHandle getBalanceUuid,
            MethodHandle getBalanceCurrency,
            MethodHandle topBalances,
            MethodHandle topShards,
            Object money,
            Object shards,
            MethodHandle entryUuid,
            MethodHandle entryUsername,
            MethodHandle entryBalance) {

        boolean matchesEnabledPlugin() {
            Plugin live = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            return live != null && live.isEnabled() && live == plugin;
        }
    }
}