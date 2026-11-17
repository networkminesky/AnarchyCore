package anarchy.minesky.core.impl.referral;

import java.util.UUID;

public record ReferralRecord(
        long id,
        UUID inviteeUuid,
        UUID referrerUuid,
        String code,
        ReferralStatus status,
        long createdAt,
        long expiresAt,
        Long qualifiedAt,
        Long rewardedAt,
        String payoutIdempotencyKey,
        long activePlaytimeMs,
        double distanceTraveled,
        int blocksMined,
        int blocksPlaced) {
}