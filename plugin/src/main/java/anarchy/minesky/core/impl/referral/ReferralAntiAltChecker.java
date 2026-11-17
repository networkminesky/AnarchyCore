package anarchy.minesky.core.impl.referral;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.UUID;

public final class ReferralAntiAltChecker {

    private final ReferralSettings settings;
    private final ReferralRepository referrals;
    private final IpHistoryRepository ipHistory;
    private final PlayerIdentityRepository identity;
    private final ReferralNetworkVerifier networkVerifier;

    public ReferralAntiAltChecker(
            ReferralSettings settings,
            ReferralRepository referrals,
            IpHistoryRepository ipHistory,
            PlayerIdentityRepository identity,
            ReferralNetworkVerifier networkVerifier) {
        this.settings = settings;
        this.referrals = referrals;
        this.ipHistory = ipHistory;
        this.identity = identity;
        this.networkVerifier = networkVerifier;
    }

    public ReferralRejectReason validateRedeem(Player invitee, UUID referrerUuid) {
        if (!ipHistory.isActive() || !referrals.isActive()) {
            return ReferralRejectReason.DATABASE_INACTIVE;
        }
        UUID inviteeUuid = invitee.getUniqueId();
        if (inviteeUuid.equals(referrerUuid)) {
            return ReferralRejectReason.SELF_REFERRAL;
        }
        if (referrals.hasInviteeEverRedeemed(inviteeUuid)) {
            return ReferralRejectReason.ALREADY_REDEEMED;
        }
        if (referrals.wouldCreateCycle(referrerUuid, inviteeUuid, settings.cycleMaxDepth())) {
            return ReferralRejectReason.CIRCULAR_REFERRAL;
        }
        ReferralRejectReason vpn = networkVerifier.checkJoinNetwork(invitee);
        if (vpn != null) {
            return vpn;
        }
        Long firstJoin = identity.getFirstJoinAt(inviteeUuid);
        long now = System.currentTimeMillis();
        if (firstJoin != null && now - firstJoin > settings.newAccountMaxAgeMs()) {
            return ReferralRejectReason.ACCOUNT_TOO_OLD;
        }
        ReferralRejectReason network = sharedNetwork(invitee, referrerUuid);
        if (network != null) {
            return network;
        }
        ReferralRejectReason subnetLimit = checkSubnetRedemptionLimit(invitee);
        if (subnetLimit != null) {
            return subnetLimit;
        }
        return checkReferrerCaps(referrerUuid, now);
    }

    public ReferralRejectReason validateQualification(Player invitee, UUID referrerUuid) {
        if (!ipHistory.isActive() || !referrals.isActive()) {
            return ReferralRejectReason.DATABASE_INACTIVE;
        }
        UUID inviteeUuid = invitee.getUniqueId();
        if (inviteeUuid.equals(referrerUuid)) {
            return ReferralRejectReason.SELF_REFERRAL;
        }
        if (referrals.isPlayerBlocked(inviteeUuid)) {
            return ReferralRejectReason.INVITEE_BLOCKED;
        }
        if (referrals.isPlayerBlocked(referrerUuid)) {
            return ReferralRejectReason.REFERRER_BLOCKED;
        }
        ReferralRejectReason punishment = checkPunishment(invitee, referrerUuid);
        if (punishment != null) {
            return punishment;
        }
        if (referrals.wouldCreateCycle(referrerUuid, inviteeUuid, settings.cycleMaxDepth())) {
            return ReferralRejectReason.CIRCULAR_REFERRAL;
        }
        ReferralRejectReason vpn = networkVerifier.checkJoinNetwork(invitee);
        if (vpn != null) {
            return vpn;
        }
        ReferralRejectReason network = sharedNetwork(invitee, referrerUuid);
        if (network != null) {
            return network;
        }
        return checkSubnetRedemptionLimit(invitee);
    }

    private ReferralRejectReason checkPunishment(Player invitee, UUID referrerUuid) {
        if (invitee.isBanned() || isProfileBanned(invitee.getUniqueId()) || isProfileBanned(referrerUuid)) {
            return ReferralRejectReason.PUNISHED;
        }
        return null;
    }

    private static boolean isProfileBanned(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        try {
            BanList bans = Bukkit.getBanList(BanList.Type.PROFILE);
            return bans.isBanned(uuid.toString());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private ReferralRejectReason sharedNetwork(Player invitee, UUID referrerUuid) {
        UUID inviteeUuid = invitee.getUniqueId();
        if (settings.checkSameIp() && ipHistory.shareAnyIp(referrerUuid, inviteeUuid)) {
            return ReferralRejectReason.SAME_IP;
        }
        if (settings.checkSameSubnet() && ipHistory.shareAnySubnet(referrerUuid, inviteeUuid)) {
            return ReferralRejectReason.SAME_SUBNET;
        }
        return null;
    }

    private ReferralRejectReason checkSubnetRedemptionLimit(Player invitee) {
        if (settings.maxRedemptionsPerSubnet() <= 0) {
            return null;
        }
        if (invitee.getAddress() == null) {
            return null;
        }
        InetAddress address = invitee.getAddress().getAddress();
        String subnet = NetworkAddressUtils.subnetKey(address, settings.ipv4Slash24(), settings.ipv6Slash64());
        int count = referrals.countActiveRedemptionsOnSubnet(subnet, invitee.getUniqueId());
        if (count >= settings.maxRedemptionsPerSubnet()) {
            return ReferralRejectReason.SUBNET_LIMIT;
        }
        return null;
    }

    private ReferralRejectReason checkReferrerCaps(UUID referrerUuid, long now) {
        if (settings.maxRewardsPerDay() > 0) {
            int day = referrals.countRewardsInPeriod(referrerUuid, now - 86_400_000L);
            if (day >= settings.maxRewardsPerDay()) {
                return ReferralRejectReason.REFERRER_CAP_DAILY;
            }
        }
        if (settings.maxRewardsPerWeek() > 0) {
            int week = referrals.countRewardsInPeriod(referrerUuid, now - 7L * 86_400_000L);
            if (week >= settings.maxRewardsPerWeek()) {
                return ReferralRejectReason.REFERRER_CAP_WEEKLY;
            }
        }
        if (settings.maxRewardsTotal() > 0) {
            int total = referrals.countTotalRewards(referrerUuid);
            if (total >= settings.maxRewardsTotal()) {
                return ReferralRejectReason.REFERRER_CAP_TOTAL;
            }
        }
        return null;
    }
}
