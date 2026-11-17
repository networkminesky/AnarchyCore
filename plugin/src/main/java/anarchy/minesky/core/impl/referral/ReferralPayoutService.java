package anarchy.minesky.core.impl.referral;

import anarchy.minesky.core.impl.referral.economy.FragmentEconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class ReferralPayoutService {

    private final Plugin plugin;
    private final Logger logger;
    private final ReferralSettings settings;
    private final ReferralRepository repository;
    private final ReferralAuditRepository audit;
    private final FragmentEconomyProvider economy;
    private final Map<Long, Object> payoutLocks = new ConcurrentHashMap<>();

    public ReferralPayoutService(
            Plugin plugin,
            ReferralSettings settings,
            ReferralRepository repository,
            ReferralAuditRepository audit,
            FragmentEconomyProvider economy) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.settings = settings;
        this.repository = repository;
        this.audit = audit;
        this.economy = economy;
    }

    public boolean isEconomyAvailable() {
        return economy.isAvailable();
    }

    public void processPayout(ReferralRecord initial, Runnable onReferrerNotify) {
        if (initial == null) {
            return;
        }
        Object lock = payoutLocks.computeIfAbsent(initial.id(), id -> new Object());
        synchronized (lock) {
            ReferralRecord record = resolveForAutomatic(initial.id());
            if (record == null || !economy.isAvailable()) {
                return;
            }
            long payoutId = record.id();
            String idempotency = "referral:" + payoutId;
            if (!repository.reservePayout(payoutId, idempotency)) {
                return;
            }
            if (!economy.isAvailable()) {
                repository.releasePayoutToRetry(payoutId);
                return;
            }
            if (!repository.markPayoutSent(payoutId)) {
                logger.warning("[Referral] payout id=" + payoutId + " não marcado como enviado; crédito abortado.");
                return;
            }
            dispatch(record, idempotency, onReferrerNotify);
        }
    }

    public void reprocessPayout(long referralId) {
        Object lock = payoutLocks.computeIfAbsent(referralId, id -> new Object());
        synchronized (lock) {
            ReferralRecord record = repository.getById(referralId);
            if (record == null || record.status() == ReferralStatus.REWARDED) {
                return;
            }
            String key = record.payoutIdempotencyKey();
            boolean alreadySent = key != null && key.endsWith(":sent");
            if (record.status() == ReferralStatus.PROCESSING_PAYOUT && alreadySent) {
                return;
            }
            if (record.status() == ReferralStatus.PAYOUT_FAILED || record.status() == ReferralStatus.PROCESSING_PAYOUT) {
                if (!repository.releasePayoutToRetry(referralId)) {
                    return;
                }
            }
            ReferralRecord fresh = repository.getById(referralId);
            if (fresh == null || fresh.status() != ReferralStatus.QUALIFIED_PENDING_PAYOUT) {
                return;
            }
            if (!economy.isAvailable()) {
                return;
            }
            String idempotency = "referral:" + referralId;
            if (!repository.reservePayout(referralId, idempotency)) {
                return;
            }
            if (!repository.markPayoutSent(referralId)) {
                return;
            }
            dispatch(fresh, idempotency, null);
        }
    }

    private void dispatch(ReferralRecord record, String idempotency, Runnable onReferrerNotify) {
        long payoutId = record.id();
        UUID referrer = record.referrerUuid();
        UUID invitee = record.inviteeUuid();
        String auditReferrer = idempotency + ":referrer";
        String auditInvitee = idempotency + ":invitee";
        economy.depositShards(referrer, offlineName(referrer), settings.referrerRewardShards(), auditReferrer)
                .thenCompose(balance -> {
                    if (settings.inviteeBonusShards() > 0) {
                        return economy.depositShards(invitee, offlineName(invitee), settings.inviteeBonusShards(), auditInvitee)
                                .thenApply(ignored -> balance);
                    }
                    return java.util.concurrent.CompletableFuture.completedFuture(balance);
                })
                .whenComplete((balance, error) -> {
                    if (error != null) {
                        repository.markPayoutFailed(payoutId);
                        audit.logAsync(referrer, payoutId, "PAYOUT_FAILED", ReferralRejectReason.ECONOMY_UNAVAILABLE,
                                error.getMessage());
                        logger.warning("[Referral] payout falhou id=" + payoutId + ": " + error.getMessage());
                        return;
                    }
                    long now = System.currentTimeMillis();
                    if (!repository.markRewarded(payoutId, now)) {
                        logger.severe("[Referral] CRÍTICO: economy creditou mas markRewarded falhou id=" + payoutId);
                        return;
                    }
                    audit.logAsync(referrer, payoutId, "PAYOUT_SUCCESS", null, "shards credited");
                    if (Bukkit.getServer() == null || onReferrerNotify == null) {
                        return;
                    }
                    Player onlineReferrer = Bukkit.getPlayer(referrer);
                    if (onlineReferrer != null && onReferrerNotify != null) {
                        onlineReferrer.getScheduler().run(plugin, task -> onReferrerNotify.run(), null);
                    }
                });
    }

    private ReferralRecord resolveForAutomatic(long id) {
        ReferralRecord record = repository.getById(id);
        if (record == null) {
            return null;
        }
        if (record.status() == ReferralStatus.QUALIFIED_PENDING_PAYOUT) {
            return record;
        }
        String key = record.payoutIdempotencyKey();
        boolean alreadySent = key != null && key.endsWith(":sent");
        if (alreadySent) {
            return null;
        }
        if (record.status() == ReferralStatus.PAYOUT_FAILED || record.status() == ReferralStatus.PROCESSING_PAYOUT) {
            if (!repository.releasePayoutToRetry(id)) {
                return null;
            }
            ReferralRecord fresh = repository.getById(id);
            if (fresh != null && fresh.status() == ReferralStatus.QUALIFIED_PENDING_PAYOUT) {
                return fresh;
            }
        }
        return null;
    }

    private static String offlineName(UUID uuid) {
        if (Bukkit.getServer() == null) {
            return uuid.toString().substring(0, 8);
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() != null ? offline.getName() : uuid.toString().substring(0, 8);
    }
}
