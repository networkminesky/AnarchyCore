package anarchy.minesky.core.impl.escmenu.economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pagamentos MONEY via {@code EconomyAPI.pay} — lazy binding pelo classloader do AnarchyEconomy.
 */
public final class AnarchyEconomyMoneyProvider implements Listener {

    static final String PLUGIN_NAME = "AnarchyEconomy";
    private static final String API_CLASS = "anarchy.minesky.economy.api.AnarchyEconomyAPI";
    private static final String ECONOMY_CLASS = "anarchy.minesky.economy.api.EconomyAPI";
    private static final String TRANSFER_RESULT_CLASS = "anarchy.minesky.economy.api.model.TransferResult";

    private final Plugin plugin;
    private final Logger logger;
    private volatile Binding binding;

    public AnarchyEconomyMoneyProvider(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (!tryBind()) {
            logger.info("[PIX] AnarchyEconomy indisponível — pagamentos via menu ficarão em fallback de comando.");
        }
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
        if (tryBind()) {
            logger.info("[PIX] AnarchyEconomy habilitado — API de pagamento vinculada.");
        }
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
            Object ready = current.isAvailable.invoke();
            if (!Boolean.TRUE.equals(ready)) {
                return false;
            }
            return current.economy.invoke() != null;
        } catch (Throwable error) {
            logger.log(Level.WARNING, "[PIX] Falha ao consultar AnarchyEconomy", error);
            return false;
        }
    }

    public CompletableFuture<PayOutcome> pay(Player sender, Player receiver, double amount) {
        if (sender == null || receiver == null) {
            return CompletableFuture.completedFuture(PayOutcome.failed("invalid_target"));
        }
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            return CompletableFuture.completedFuture(PayOutcome.failed("invalid_amount"));
        }
        Binding current = binding;
        if (current == null || !isAvailable()) {
            return CompletableFuture.completedFuture(PayOutcome.unavailable());
        }
        try {
            Object api = current.economy.invoke();
            if (api == null) {
                return CompletableFuture.completedFuture(PayOutcome.unavailable());
            }
            Object raw = current.pay.invoke(api, sender, receiver, amount);
            if (!(raw instanceof CompletableFuture<?> future)) {
                return CompletableFuture.completedFuture(PayOutcome.failed("api_error"));
            }
            return future.thenApply(result -> mapTransferResult(result, amount))
                    .exceptionally(error -> {
                        logger.log(Level.WARNING, "[PIX] pay falhou " + sender.getName() + " -> " + receiver.getName(), error);
                        return PayOutcome.failed("exception");
                    });
        } catch (Throwable error) {
            logger.log(Level.WARNING, "[PIX] pay rejeitado", error);
            return CompletableFuture.completedFuture(PayOutcome.failed("exception"));
        }
    }

    private PayOutcome mapTransferResult(Object result, double amount) {
        if (result == null) {
            return PayOutcome.failed("null_result");
        }
        Binding current = binding;
        if (current == null) {
            return PayOutcome.unavailable();
        }
        try {
            boolean success = (boolean) current.transferSuccess.invoke(result);
            return success ? PayOutcome.success(amount) : PayOutcome.failed("insufficient");
        } catch (Throwable error) {
            logger.log(Level.WARNING, "[PIX] Falha ao ler TransferResult", error);
            return PayOutcome.failed("api_error");
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
            Class<?> transferClass = loader.loadClass(TRANSFER_RESULT_CLASS);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            MethodHandle isAvailable = lookup.unreflect(apiClass.getMethod("isAvailable"));
            MethodHandle economyMethod = lookup.unreflect(apiClass.getMethod("economy"));
            MethodHandle pay = lookup.unreflect(
                    economyClass.getMethod("pay", Player.class, Player.class, double.class));
            MethodHandle transferSuccess = lookup.unreflect(transferClass.getMethod("success"));
            binding = new Binding(economy, isAvailable, economyMethod, pay, transferSuccess);
            return true;
        } catch (Throwable error) {
            clearBinding();
            logger.log(Level.WARNING, "[PIX] Não foi possível vincular API do AnarchyEconomy", error);
            return false;
        }
    }

    private void clearBinding() {
        binding = null;
    }

    public enum PayOutcomeStatus {
        SUCCESS,
        FAILED,
        UNAVAILABLE
    }

    public record PayOutcome(PayOutcomeStatus status, double amount, String reason) {
        public static PayOutcome success(double amount) {
            return new PayOutcome(PayOutcomeStatus.SUCCESS, amount, null);
        }

        public static PayOutcome failed(String reason) {
            return new PayOutcome(PayOutcomeStatus.FAILED, 0, reason);
        }

        public static PayOutcome unavailable() {
            return new PayOutcome(PayOutcomeStatus.UNAVAILABLE, 0, "economy_offline");
        }
    }

    private record Binding(
            Plugin plugin,
            MethodHandle isAvailable,
            MethodHandle economy,
            MethodHandle pay,
            MethodHandle transferSuccess) {

        boolean matchesEnabledPlugin() {
            Plugin live = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            return live != null && live.isEnabled() && live == plugin;
        }
    }
}