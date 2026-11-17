package anarchy.minesky.core.impl.referral.economy;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class UnavailableFragmentEconomyProvider implements FragmentEconomyProvider {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public CompletableFuture<Void> ensureAccount(UUID uuid, String name) {
        return CompletableFuture.failedFuture(new IllegalStateException("AnarchyEconomy indisponível"));
    }

    @Override
    public CompletableFuture<Double> depositShards(UUID targetUuid, String targetName, double amount, String auditKey) {
        return CompletableFuture.failedFuture(new IllegalStateException("AnarchyEconomy indisponível"));
    }
}