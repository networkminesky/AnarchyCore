package anarchy.minesky.core.impl.referral;

import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.logging.Level;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReferralManager {

    public static final String PERMISSION = "anarchycore.convite";
    public static final String ADMIN_PERMISSION = "anarchycore.convite.admin";

    private final Plugin plugin;
    private final ReferralSettings settings;
    private final ReferralRepository repository;
    private final ReferralAuditRepository audit;
    private final ReferralAntiAltChecker antiAlt;
    private final ReferralQualificationTracker qualification;
    private final ReferralPayoutService payout;
    private final PlayerIdentityRepository identity;
    private final IpHistoryRepository ipHistory;
    private ReferralCodeGenerator codeGenerator;
    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final Map<UUID, Long> lastRedeemAttempt = new ConcurrentHashMap<>();
    private final Map<UUID, Object> redeemLocks = new ConcurrentHashMap<>();
    private final Map<UUID, Object> referrerLocks = new ConcurrentHashMap<>();
    private final Map<String, Object> subnetLocks = new ConcurrentHashMap<>();

    public ReferralManager(
            Plugin plugin,
            ReferralSettings settings,
            ReferralRepository repository,
            ReferralAuditRepository audit,
            ReferralAntiAltChecker antiAlt,
            ReferralQualificationTracker qualification,
            ReferralPayoutService payout,
            PlayerIdentityRepository identity,
            IpHistoryRepository ipHistory,
            MessagesManager messages,
            FeedbackManager feedback) {
        this.plugin = plugin;
        this.settings = settings;
        this.repository = repository;
        this.audit = audit;
        this.antiAlt = antiAlt;
        this.qualification = qualification;
        this.payout = payout;
        this.identity = identity;
        this.ipHistory = ipHistory;
        this.codeGenerator = new ReferralCodeGenerator(settings.codeCharset(), settings.codeLength());
        this.messages = messages;
        this.feedback = feedback;
        qualification.setOnTick(this::tryQualify);
    }

    public boolean isEnabled() {
        return settings.enabled() && repository.isActive();
    }

    public void onPlayerJoin(Player player) {
        if (!isEnabled()) {
            return;
        }
        String brand = player.getClientBrandName();
        identity.ensureFirstJoin(player.getUniqueId(), brand != null ? brand : "");
        if (player.getAddress() != null) {
            ipHistory.recordIpAsync(player.getUniqueId(), player.getAddress().getAddress());
        }
        ReferralRecord pending = repository.getByInvitee(player.getUniqueId());
        if (pending != null && pending.status() == ReferralStatus.PENDING) {
            qualification.bindPending(player, pending);
        }
        if (pending != null && isPayoutQueue(pending.status())) {
            payout.processPayout(pending, () -> notifyReferrerReward(player, pending));
        }
        for (ReferralRecord row : repository.listByReferrer(player.getUniqueId())) {
            if (isPayoutQueue(row.status())) {
                payout.processPayout(row, () -> notifyReferrerReward(player, row));
            }
        }
        notifyPendingRewardOnJoin(player);
    }

    public void onPlayerQuit(Player player) {
        qualification.unbind(player.getUniqueId());
    }

    public void tickGlobal() {
        if (!isEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        repository.expireStale(now);
        if (!payout.isEconomyAvailable()) {
            return;
        }
        for (ReferralRecord row : repository.listPendingPayoutOrQualification()) {
            if (row.status() == ReferralStatus.QUALIFIED_PENDING_PAYOUT
                    || row.status() == ReferralStatus.PAYOUT_FAILED
                    || row.status() == ReferralStatus.PROCESSING_PAYOUT) {
                payout.processPayout(row, () -> notifyReferrerReward(null, row));
            }
        }
    }

    public void processQualifiedPayouts() {
        if (!isEnabled() || !payout.isEconomyAvailable()) {
            return;
        }
        for (ReferralRecord row : repository.listPendingPayoutOrQualification()) {
            if (row.status() == ReferralStatus.QUALIFIED_PENDING_PAYOUT
                    || row.status() == ReferralStatus.PAYOUT_FAILED
                    || row.status() == ReferralStatus.PROCESSING_PAYOUT) {
                payout.processPayout(row, () -> notifyReferrerReward(null, row));
            }
        }
    }

    public void showOrCreateCode(Player player) {
        try {
            if (!isEnabled()) {
                sendError(player, "referral.error.disabled");
                return;
            }
            if (repository.isPlayerBlocked(player.getUniqueId())) {
                sendError(player, "referral.error.blocked");
                return;
            }
            String code = repository.getOrCreateCode(
                    player.getUniqueId(), codeGenerator, settings.maxCodeGenerationAttempts());
            if (code == null) {
                sendError(player, "referral.error.code-generation");
                return;
            }
            deliver(player, () -> feedback.send(
                    player,
                    messages.component("referral.code.self", "code", code),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS));
        } catch (Throwable error) {
            fail(player, "showOrCreateCode", error);
        }
    }

    public void showStatus(Player player) {
        try {
            showStatusUnsafe(player);
        } catch (Throwable error) {
            fail(player, "showStatus", error);
        }
    }

    private void showStatusUnsafe(Player player) {
        if (!isEnabled()) {
            sendError(player, "referral.error.disabled");
            return;
        }
        qualification.flush(player.getUniqueId());
        String code = repository.getCodeByPlayer(player.getUniqueId());
        ReferralRecord row = repository.getByInvitee(player.getUniqueId());
        deliver(player, () -> feedback.send(player, messages.component("referral.status.header"), FeedbackPriority.NORMAL, true,
                UiSoundMode.SUCCESS));
        deliver(player, () -> feedback.send(player, messages.component("referral.status.code", "code", code != null ? code : "-"),
                FeedbackPriority.NORMAL, false, UiSoundMode.NONE));
        if (row == null) {
            deliver(player, () -> feedback.send(player, messages.component("referral.status.none"), FeedbackPriority.NORMAL, false,
                    UiSoundMode.NONE));
            return;
        }
        long minutes = row.activePlaytimeMs() / 60_000L;
        long needMinutes = settings.minActivePlaytimeMs() / 60_000L;
        deliver(player, () -> feedback.send(player, messages.component(
                "referral.status.progress",
                "status", row.status().name(),
                "minutes", String.valueOf(minutes),
                "needMinutes", String.valueOf(needMinutes),
                "distance", String.valueOf((int) row.distanceTraveled()),
                "needDistance", String.valueOf((int) settings.minDistanceTraveled()),
                "mined", String.valueOf(row.blocksMined()),
                "needMined", String.valueOf(settings.minBlocksMined()),
                "placed", String.valueOf(row.blocksPlaced()),
                "needPlaced", String.valueOf(settings.minBlocksPlaced())),
                FeedbackPriority.NORMAL, false, UiSoundMode.NONE));
    }

    public void redeemCode(Player invitee, String rawCode) {
        try {
            redeemCodeUnsafe(invitee, rawCode);
        } catch (Throwable error) {
            fail(invitee, "redeemCode", error);
        }
    }

    private void redeemCodeUnsafe(Player invitee, String rawCode) {
        if (!isEnabled()) {
            sendError(invitee, "referral.error.disabled");
            return;
        }
        UUID inviteeUuid = invitee.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastRedeemAttempt.get(inviteeUuid);
        if (last != null && now - last < settings.redeemCooldownMs()) {
            reject(invitee, null, ReferralRejectReason.RATE_LIMITED, rawCode);
            return;
        }
        lastRedeemAttempt.put(inviteeUuid, now);

        Object lock = redeemLocks.computeIfAbsent(inviteeUuid, u -> new Object());
        synchronized (lock) {
            if (invitee.getAddress() != null) {
                ipHistory.recordIp(inviteeUuid, invitee.getAddress().getAddress());
            }
            if (repository.hasInviteeEverRedeemed(inviteeUuid)) {
                reject(invitee, null, ReferralRejectReason.ALREADY_REDEEMED, rawCode);
                return;
            }
            if (repository.isPlayerBlocked(inviteeUuid)) {
                reject(invitee, null, ReferralRejectReason.INVITEE_BLOCKED, rawCode);
                return;
            }
            String code = normalizeCode(rawCode);
            if (code == null) {
                reject(invitee, null, ReferralRejectReason.INVALID_CODE, rawCode);
                return;
            }
            if (repository.isCodeBlocked(code)) {
                reject(invitee, null, ReferralRejectReason.CODE_BLOCKED, code);
                return;
            }
            UUID referrerUuid = repository.getReferrerByCode(code);
            if (referrerUuid == null) {
                reject(invitee, null, ReferralRejectReason.CODE_NOT_FOUND, code);
                return;
            }
            if (repository.isPlayerBlocked(referrerUuid)) {
                reject(invitee, null, ReferralRejectReason.REFERRER_BLOCKED, code);
                return;
            }
            Object referrerLock = referrerLocks.computeIfAbsent(referrerUuid, u -> new Object());
            String subnetKey = currentSubnet(invitee);
            Object subnetLock = subnetLocks.computeIfAbsent(subnetKey, key -> new Object());
            synchronized (referrerLock) {
                synchronized (subnetLock) {
                    ReferralRejectReason antiAltReason = antiAlt.validateRedeem(invitee, referrerUuid);
                    if (antiAltReason != null) {
                        reject(invitee, null, antiAltReason, code);
                        return;
                    }
                    long expiresAt = now + settings.qualificationDeadlineMs();
                    Long referralId = repository.createRedemption(inviteeUuid, referrerUuid, code, now, expiresAt);
                    if (referralId == null) {
                        reject(invitee, null, ReferralRejectReason.ALREADY_REDEEMED, code);
                        return;
                    }
                    audit.logAsync(inviteeUuid, referralId, "REDEEM_SUCCESS", null, "code=" + code);
                    ReferralRecord record = repository.getById(referralId);
                    qualification.bindPending(invitee, record);
                    deliver(invitee, () -> feedback.send(
                            invitee,
                            messages.component("referral.redeem.success", "code", code),
                            FeedbackPriority.HIGH,
                            true,
                            UiSoundMode.SUCCESS));
                }
            }
        }
    }

    private String currentSubnet(Player player) {
        if (player.getAddress() == null) {
            return "";
        }
        return NetworkAddressUtils.subnetKey(
                player.getAddress().getAddress(), settings.ipv4Slash24(), settings.ipv6Slash64());
    }

    private void tryQualify(Player invitee) {
        ReferralRecord row = repository.getByInvitee(invitee.getUniqueId());
        if (row == null || row.status() != ReferralStatus.PENDING) {
            return;
        }
        if (System.currentTimeMillis() > row.expiresAt()) {
            repository.transitionStatus(row.id(), ReferralStatus.PENDING, ReferralStatus.EXPIRED);
            audit.logAsync(invitee.getUniqueId(), row.id(), "EXPIRED", ReferralRejectReason.EXPIRED, null);
            qualification.unbind(invitee.getUniqueId());
            return;
        }
        qualification.flush(invitee.getUniqueId());
        if (!qualification.isQualified(invitee.getUniqueId())) {
            return;
        }
        ReferralRejectReason finalCheck = antiAlt.validateQualification(invitee, row.referrerUuid());
        if (finalCheck != null) {
            repository.markBlockedSuspicious(row.id());
            audit.logAsync(invitee.getUniqueId(), row.id(), "BLOCKED_SUSPICIOUS", finalCheck, null);
            qualification.unbind(invitee.getUniqueId());
            String message = finalCheck == ReferralRejectReason.PUNISHED
                    ? "referral.error.punished"
                    : "referral.error.suspicious";
            sendError(invitee, message);
            return;
        }
        if (repository.markQualified(row.id(), System.currentTimeMillis())) {
            audit.logAsync(invitee.getUniqueId(), row.id(), "QUALIFIED", null, null);
            qualification.unbind(invitee.getUniqueId());
            ReferralRecord qualified = repository.getById(row.id());
            payout.processPayout(qualified, () -> notifyReferrerReward(invitee, qualified));
            deliver(invitee, () -> feedback.send(
                    invitee,
                    messages.component("referral.qualify.success"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.SUCCESS));
        }
    }

    private void notifyReferrerReward(Player context, ReferralRecord record) {
        if (record == null || alreadyNotified(record)) {
            return;
        }
        Player referrer = Bukkit.getPlayer(record.referrerUuid());
        if (referrer == null || !referrer.isOnline()) {
            return;
        }
        String inviteeName = context != null ? context.getName() : offlineName(record.inviteeUuid());
        referrer.getScheduler().run(plugin, task -> {
            feedback.send(
                    referrer,
                    messages.component(
                            "referral.reward.referrer",
                            "player",
                            inviteeName,
                            "amount",
                            String.valueOf((int) settings.referrerRewardShards())),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.SUCCESS);
            audit.logAsync(record.referrerUuid(), record.id(), "PAYOUT_NOTIFIED", null, null);
        }, null);
    }

    private void notifyPendingRewardOnJoin(Player player) {
        int sent = 0;
        for (ReferralRecord row : repository.listByReferrer(player.getUniqueId())) {
            if (row.status() != ReferralStatus.REWARDED || alreadyNotified(row)) {
                continue;
            }
            feedback.send(
                    player,
                    messages.component("referral.reward.pending-login", "amount",
                            String.valueOf((int) settings.referrerRewardShards())),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS);
            audit.logAsync(player.getUniqueId(), row.id(), "PAYOUT_NOTIFIED", null, null);
            if (++sent >= 5) {
                break;
            }
        }
    }

    private boolean alreadyNotified(ReferralRecord row) {
        for (ReferralAuditRepository.AuditEntry entry : audit.historyForPlayer(row.referrerUuid(), 100)) {
            if ("PAYOUT_NOTIFIED".equals(entry.eventType())
                    && entry.referralId() != null
                    && entry.referralId() == row.id()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPayoutQueue(ReferralStatus status) {
        return status == ReferralStatus.QUALIFIED_PENDING_PAYOUT
                || status == ReferralStatus.PAYOUT_FAILED
                || status == ReferralStatus.PROCESSING_PAYOUT;
    }

    public void adminReprocess(Player admin, String targetName) {
        if (!admin.hasPermission(ADMIN_PERMISSION)) {
            sendError(admin, "commands.no-permission");
            return;
        }
        UUID uuid = resolvePlayer(targetName);
        if (uuid == null) {
            sendError(admin, "referral.error.player-not-found");
            return;
        }
        ReferralRecord record = repository.getByInvitee(uuid);
        if (record == null) {
            record = repository.listByReferrer(uuid).stream()
                    .filter(r -> r.status() == ReferralStatus.QUALIFIED_PENDING_PAYOUT
                            || r.status() == ReferralStatus.PAYOUT_FAILED)
                    .findFirst()
                    .orElse(null);
        }
        if (record == null) {
            sendError(admin, "referral.error.no-pending-payout");
            return;
        }
        payout.reprocessPayout(record.id());
        feedback.send(admin, messages.component("referral.admin.reprocess"), FeedbackPriority.NORMAL, true,
                UiSoundMode.SUCCESS);
    }

    public void adminInspect(Player admin, String targetName) {
        if (!admin.hasPermission(ADMIN_PERMISSION)) {
            sendError(admin, "commands.no-permission");
            return;
        }
        UUID uuid = resolvePlayer(targetName);
        if (uuid == null) {
            sendError(admin, "referral.error.player-not-found");
            return;
        }
        String code = repository.getCodeByPlayer(uuid);
        ReferralRecord asInvitee = repository.getByInvitee(uuid);
        var list = repository.listByReferrer(uuid);
        var auditList = audit.historyForPlayer(uuid, 10);
        boolean blocked = repository.isPlayerBlocked(uuid);
        admin.sendMessage(messages.component("referral.admin.inspect.header", "player", targetName));
        admin.sendMessage(messages.component("referral.admin.inspect.code", "code", code != null ? code : "-"));
        admin.sendMessage(messages.component("referral.admin.inspect.flags", "blocked", blocked ? "sim" : "não"));
        if (asInvitee != null) {
            admin.sendMessage(messages.component(
                    "referral.admin.inspect.as-invitee",
                    "status",
                    asInvitee.status().name(),
                    "code",
                    asInvitee.code()));
            admin.sendMessage(messages.component(
                    "referral.admin.inspect.progress",
                    "minutes", String.valueOf(asInvitee.activePlaytimeMs() / 60_000L),
                    "distance", String.valueOf((int) asInvitee.distanceTraveled()),
                    "mined", String.valueOf(asInvitee.blocksMined()),
                    "placed", String.valueOf(asInvitee.blocksPlaced())));
        }
        admin.sendMessage(messages.component("referral.admin.inspect.referrals-count", "count", String.valueOf(list.size())));
        for (ReferralAuditRepository.AuditEntry entry : auditList) {
            admin.sendMessage(messages.component(
                    "referral.admin.inspect.audit-line",
                    "event",
                    entry.eventType(),
                    "reason",
                    entry.reason() != null ? entry.reason() : "-"));
        }
    }

    public void adminBlock(Player admin, String targetName, String reason) {
        if (!admin.hasPermission(ADMIN_PERMISSION)) {
            sendError(admin, "commands.no-permission");
            return;
        }
        UUID uuid = resolvePlayer(targetName);
        if (uuid == null) {
            sendError(admin, "referral.error.player-not-found");
            return;
        }
        repository.blockPlayer(uuid, reason != null ? reason : "admin");
        audit.logAsync(uuid, null, "ADMIN_BLOCK", null, reason);
        feedback.send(admin, messages.component("referral.admin.blocked", "player", targetName),
                FeedbackPriority.NORMAL, true, UiSoundMode.SUCCESS);
    }

    public void adminReload(Player admin) {
        if (!admin.hasPermission(ADMIN_PERMISSION)) {
            sendError(admin, "commands.no-permission");
            return;
        }
        reload();
        feedback.send(admin, messages.component("referral.admin.reloaded"), FeedbackPriority.NORMAL, true,
                UiSoundMode.SUCCESS);
    }

    public void reload() {
        settings.reload();
        codeGenerator = new ReferralCodeGenerator(settings.codeCharset(), settings.codeLength());
    }

    private UUID resolvePlayer(String targetName) {
        if (targetName == null || targetName.isBlank()) {
            return null;
        }
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            return online.getUniqueId();
        }
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(targetName);
        if (cached != null) {
            return cached.getUniqueId();
        }
        return null;
    }

    public void shutdown() {
        qualification.flushAll();
    }

    public ReferralRepository repository() {
        return repository;
    }

    public ReferralAuditRepository audit() {
        return audit;
    }

    private void reject(Player player, Long referralId, ReferralRejectReason reason, String code) {
        audit.logAsync(player.getUniqueId(), referralId, "REDEEM_REJECTED", reason, "code=" + code);
        String key = "referral.error." + reason.name().toLowerCase(Locale.ROOT);
        sendError(player, key);
    }

    private void sendError(Player player, String messageKey) {
        deliver(player, () -> feedback.send(
                player, messages.component(messageKey), FeedbackPriority.HIGH, true, UiSoundMode.ERROR));
    }

    private void fail(Player player, String action, Throwable error) {
        plugin.getLogger().log(Level.SEVERE, "[Referral] " + action + " falhou", error);
        deliver(player, () -> feedback.send(
                player,
                messages.component("referral.error.internal"),
                FeedbackPriority.HIGH,
                true,
                UiSoundMode.ERROR));
    }

    private void deliver(Player player, Runnable action) {
        if (player == null || !player.isOnline()) {
            return;
        }
        Runnable safe = () -> {
            try {
                action.run();
            } catch (Throwable error) {
                plugin.getLogger().log(Level.SEVERE, "[Referral] Falha ao enviar mensagem", error);
            }
        };
        if (Bukkit.isOwnedByCurrentRegion(player)) {
            safe.run();
            return;
        }
        player.getScheduler().run(plugin, task -> safe.run(), null);
    }

    private static String normalizeCode(String raw) {
        if (raw == null) {
            return null;
        }
        String code = raw.trim().toUpperCase(Locale.ROOT);
        if (code.length() < 6 || code.length() > 16) {
            return null;
        }
        return code;
    }

    private static String offlineName(UUID uuid) {
        var offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() != null ? offline.getName() : uuid.toString().substring(0, 8);
    }
}