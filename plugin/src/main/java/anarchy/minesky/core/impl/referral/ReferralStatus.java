package anarchy.minesky.core.impl.referral;

public enum ReferralStatus {
    PENDING,
    QUALIFIED_PENDING_PAYOUT,
    PROCESSING_PAYOUT,
    REWARDED,
    BLOCKED_SUSPICIOUS,
    EXPIRED,
    PAYOUT_FAILED;

    public String dbValue() {
        return name();
    }

    public static ReferralStatus fromDb(String raw) {
        if (raw == null || raw.isBlank()) {
            return PENDING;
        }
        try {
            return valueOf(raw);
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }
}