package anarchy.minesky.core.impl.referral;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class ReferralMenuUi {

    private ReferralMenuUi() {}

    public static boolean showRedeemAction(boolean hasInviteeEverRedeemed) {
        return !hasInviteeEverRedeemed;
    }

    public static boolean showCustomCodeAction(boolean customCodeEnabled, boolean hasPermission) {
        return customCodeEnabled && hasPermission;
    }

    public static int countValidReferrals(Collection<ReferralRecord> referrals) {
        if (referrals == null || referrals.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (ReferralRecord row : referrals) {
            if (row == null || row.status() == null) {
                continue;
            }
            if (isValidReferralStatus(row.status())) {
                count++;
            }
        }
        return count;
    }

    public static boolean isValidReferralStatus(ReferralStatus status) {
        return switch (status) {
            case QUALIFIED_PENDING_PAYOUT, PROCESSING_PAYOUT, REWARDED, PAYOUT_FAILED -> true;
            default -> false;
        };
    }

    public static String statusDisplayName(ReferralStatus status) {
        if (status == null) {
            return "Desconhecido";
        }
        return switch (status) {
            case PENDING -> "Pendente";
            case QUALIFIED_PENDING_PAYOUT, PROCESSING_PAYOUT, PAYOUT_FAILED -> "Qualificado";
            case REWARDED -> "Recompensado";
            case BLOCKED_SUSPICIOUS -> "Bloqueado";
            case EXPIRED -> "Expirado";
        };
    }

    public static String statusDisplayNameDb(String raw) {
        return statusDisplayName(ReferralStatus.fromDb(raw));
    }

    public static List<String> visibleActionIds(ReferralMenuSnapshot snapshot) {
        java.util.ArrayList<String> actions = new java.util.ArrayList<>();
        actions.add("copy");
        if (showRedeemAction(snapshot.hasInviteeEverRedeemed())) {
            actions.add("redeem");
        }
        if (showCustomCodeAction(snapshot.customCodeEnabled(), snapshot.customCodePermission())) {
            actions.add("custom");
        }
        return List.copyOf(actions);
    }

    public record ReferralMenuSnapshot(
            String ownCode,
            boolean hasInviteeEverRedeemed,
            ReferralRecord asInvitee,
            int validReferralCount,
            boolean customCodeEnabled,
            boolean customCodePermission) {

        public String usedCodeOrDash() {
            if (asInvitee == null || asInvitee.code() == null || asInvitee.code().isBlank()) {
                return "-";
            }
            return asInvitee.code().toUpperCase(Locale.ROOT);
        }

        public String usedStatusLabel() {
            if (asInvitee == null) {
                return "-";
            }
            return ReferralMenuUi.statusDisplayName(asInvitee.status());
        }
    }
}