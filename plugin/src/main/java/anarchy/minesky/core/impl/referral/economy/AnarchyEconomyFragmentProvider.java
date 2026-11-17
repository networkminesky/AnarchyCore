package anarchy.minesky.core.impl.referral.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolve {@code AnarchyEconomyAPI} só pelo classloader do plugin AnarchyEconomy.
 * O bytecode deste adapter não referencia {@code anarchy.minesky.economy.*}.
 * Assinaturas (API 1.0.1): {@code isAvailable()}, {@code economy()},
 * {@code ensureAccount(OfflinePlayer)}, {@code deposit(OfflinePlayer, double, CurrencyType.SHARDS)}.
 * A tag {@code referral:<id>} não existe na API e fica apenas no log.
 */
public final class AnarchyEconomyFragmentProvider implements FragmentEconomyProvider, Listener {

    static final String PLUGIN_NAME = "AnarchyEconomy";
    private static final String API_CLASS = "anarchy.minesky.economy.api.AnarchyEconomyAPI";
    private static final String ECONOMY_CLASS = "anarchy.minesky.economy.api.EconomyAPI";
    private static final String CURRENCY_CLASS = "anarchy.minesky.economy.api.model.CurrencyType";

    private final Plugin plugin;
    private final Logger logger;
    private volatile Binding binding;
    private volatile Runnable onBound;

    public AnarchyEconomyFragmentProvider(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void start(Runnable onBound) {
        this.onBound = onBound;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (tryBind()) {
            scheduleBound();
        } else {
            logger.warning("AnarchyEconomy indisponível — recompensas de convite ficarão em fila até o plugin carregar.");
        }
    }

    @EventHandler
    public void onEconomyEnable(PluginEnableEvent event) {
        if (!PLUGIN_NAME.equals(event.getPlugin().getName())) {
            return;
        }
        clearBinding();
        if (tryBind()) {
            logger.info("[Referral] AnarchyEconomy habilitado — API de fragmentos vinculada.");
            scheduleBound();
        }
    }

    @EventHandler
    public void onEconomyDisable(PluginDisableEvent event) {
        if (!PLUGIN_NAME.equals(event.getPlugin().getName())) {
            return;
        }
        clearBinding();
        logger.info("[Referral] AnarchyEconomy desabilitado — cache da API limpo.");
    }

    @Override
    public boolean isAvailable() {
        Binding current = binding;
        if (current == null || !current.matchesEnabledPlugin()) {
            return false;
        }
        try {
            Object ready = current.isAvailable.invoke();
            if (!Boolean.TRUE.equals(ready)) {
                return false;
            }
            return current.economy.invoke() != null;
        } catch (Throwable error) {
            logger.log(Level.WARNING, "[Referral] Falha ao consultar AnarchyEconomy", error);
            return false;
        }
    }

    @Override
    public CompletableFuture<Void> ensureAccount(UUID uuid, String name) {
        if (uuid == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("UUID nulo"));
        }
        Binding current = binding;
        if (current == null || !isAvailable()) {
            return unavailable("ensureAccount", uuid, null);
        }
        try {
            Object api = current.economy.invoke();
            if (api == null) {
                return unavailable("ensureAccount", uuid, null);
            }
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            Object raw = current.ensureAccount.invoke(api, offline);
            if (!(raw instanceof CompletableFuture<?> future)) {
                return unavailable("ensureAccount", uuid, null);
            }
            return future.thenAccept(ignored -> { }).whenComplete((ignored, error) -> {
                if (error != null) {
                    logger.log(Level.WARNING, "[Referral] ensureAccount falhou uuid=" + uuid + " name=" + name, error);
                }
            });
        } catch (Throwable error) {
            logger.log(Level.WARNING, "[Referral] ensureAccount rejeitado uuid=" + uuid, error);
            return CompletableFuture.failedFuture(error);
        }
    }

    @Override
    public CompletableFuture<Double> depositShards(UUID targetUuid, String targetName, double amount, String auditKey) {
        String trace = auditKey != null && !auditKey.isBlank() ? auditKey : "referral:unknown";
        if (targetUuid == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("UUID nulo"));
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount < 0) {
            logger.warning("[Referral] Valor de fragmentos inválido (" + trace + "): " + amount);
            return CompletableFuture.failedFuture(new IllegalArgumentException("Valor de fragmentos inválido"));
        }
        if (amount == 0) {
            return CompletableFuture.completedFuture(0.0);
        }
        Binding current = binding;
        if (current == null || !isAvailable()) {
            return unavailable("deposit", targetUuid, trace);
        }
        try {
            Object api = current.economy.invoke();
            if (api == null) {
                return unavailable("deposit", targetUuid, trace);
            }
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetUuid);
            logger.info("[Referral] Crédito local " + trace + " uuid=" + targetUuid
                    + " name=" + targetName + " amount=" + amount
                    + " (AnarchyEconomy não recebe a tag)");
            Object ensured = current.ensureAccount.invoke(api, offline);
            if (!(ensured instanceof CompletableFuture<?> ensureFuture)) {
                return unavailable("deposit", targetUuid, trace);
            }
            return ensureFuture.thenCompose(ignored -> invokeDeposit(current, api, offline, amount, targetUuid, trace));
        } catch (Throwable error) {
            logger.log(Level.WARNING, "[Referral] deposit rejeitado (" + trace + ") uuid=" + targetUuid, error);
            return CompletableFuture.failedFuture(error);
        }
    }

    private CompletableFuture<Double> invokeDeposit(
            Binding current, Object api, OfflinePlayer offline, double amount, UUID targetUuid, String trace) {
        try {
            Object raw = current.deposit.invoke(api, offline, amount, current.shards);
            if (!(raw instanceof CompletableFuture<?> future)) {
                return unavailable("deposit", targetUuid, trace);
            }
            return future.thenApply(value -> value instanceof Number number ? number.doubleValue() : 0.0)
                    .whenComplete((balance, error) -> {
                        if (error != null) {
                            logger.log(Level.WARNING, "[Referral] Falha ao depositar fragmentos (" + trace
                                    + ") uuid=" + targetUuid, error);
                        } else {
                            logger.info("[Referral] Depositados " + amount + " fragmentos para " + targetUuid
                                    + " (" + trace + "), saldo=" + balance);
                        }
                    });
        } catch (Throwable error) {
            logger.log(Level.WARNING, "[Referral] deposit rejeitado (" + trace + ") uuid=" + targetUuid, error);
            return CompletableFuture.failedFuture(error);
        }
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
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            MethodHandle isAvailable = lookup.unreflect(apiClass.getMethod("isAvailable"));
            MethodHandle economyMethod = lookup.unreflect(apiClass.getMethod("economy"));
            MethodHandle ensureAccount = lookup.unreflect(
                    economyClass.getMethod("ensureAccount", OfflinePlayer.class));
            MethodHandle deposit = lookup.unreflect(economyClass.getMethod(
                    "deposit", OfflinePlayer.class, double.class, currencyClass));
            Object shards = currencyClass.getField("SHARDS").get(null);
            binding = new Binding(economy, isAvailable, economyMethod, ensureAccount, deposit, shards);
            return true;
        } catch (Throwable error) {
            clearBinding();
            logger.log(Level.WARNING, "[Referral] Não foi possível vincular a API do AnarchyEconomy", error);
            return false;
        }
    }

    private void clearBinding() {
        binding = null;
    }

    private void scheduleBound() {
        Runnable callback = onBound;
        if (callback == null) {
            return;
        }
        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
            if (isAvailable()) {
                callback.run();
            }
        });
    }

    private <T> CompletableFuture<T> unavailable(String action, UUID uuid, String auditKey) {
        String trace = auditKey != null ? " (" + auditKey + ")" : "";
        logger.warning("[Referral] AnarchyEconomy indisponível em " + action + " uuid=" + uuid + trace);
        return CompletableFuture.failedFuture(new IllegalStateException("AnarchyEconomy indisponível"));
    }

    private record Binding(
            Plugin plugin,
            MethodHandle isAvailable,
            MethodHandle economy,
            MethodHandle ensureAccount,
            MethodHandle deposit,
            Object shards) {

        boolean matchesEnabledPlugin() {
            Plugin live = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            return live != null && live.isEnabled() && live == plugin;
        }
    }
}
