package anarchy.minesky.core.impl.referral.economy;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Adapter isolado para creditar fragmentos (shards) via AnarchyEconomy.
 */
public interface FragmentEconomyProvider {

    boolean isAvailable();

    CompletableFuture<Double> depositShards(UUID targetUuid, String targetName, double amount, String auditKey);

    CompletableFuture<Void> ensureAccount(UUID uuid, String name);
}