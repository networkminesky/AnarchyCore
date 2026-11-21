package anarchy.minesky.core.referral;

import anarchy.minesky.core.impl.referral.ReferralMenuUi;
import anarchy.minesky.core.impl.referral.ReferralRecord;
import anarchy.minesky.core.impl.referral.ReferralStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferralConviteMenuUiTest {

    @Test
    void redeemActionOnlyWhenNeverRedeemed() {
        assertTrue(ReferralMenuUi.showRedeemAction(false));
        assertFalse(ReferralMenuUi.showRedeemAction(true));
    }

    @Test
    void customActionRequiresPermissionAndSetting() {
        assertTrue(ReferralMenuUi.showCustomCodeAction(true, true));
        assertFalse(ReferralMenuUi.showCustomCodeAction(true, false));
        assertFalse(ReferralMenuUi.showCustomCodeAction(false, true));
    }

    @Test
    void validReferralCountIgnoresPendingAndExpired() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        List<ReferralRecord> rows = List.of(
                record(a, b, ReferralStatus.PENDING),
                record(a, b, ReferralStatus.QUALIFIED_PENDING_PAYOUT),
                record(a, b, ReferralStatus.REWARDED),
                record(a, b, ReferralStatus.EXPIRED));
        assertEquals(2, ReferralMenuUi.countValidReferrals(rows));
    }

    @Test
    void statusDisplayNamesForMenuBody() {
        assertEquals("Pendente", ReferralMenuUi.statusDisplayName(ReferralStatus.PENDING));
        assertEquals("Qualificado", ReferralMenuUi.statusDisplayName(ReferralStatus.QUALIFIED_PENDING_PAYOUT));
        assertEquals("Recompensado", ReferralMenuUi.statusDisplayName(ReferralStatus.REWARDED));
        assertEquals("Bloqueado", ReferralMenuUi.statusDisplayName(ReferralStatus.BLOCKED_SUSPICIOUS));
    }

    @Test
    void visibleActionsFollowEligibility() {
        var fresh = new ReferralMenuUi.ReferralMenuSnapshot(
                "CODE1", false, null, 0, true, true);
        assertEquals(List.of("copy", "redeem", "custom"), ReferralMenuUi.visibleActionIds(fresh));

        var redeemed = new ReferralMenuUi.ReferralMenuSnapshot(
                "CODE1", true, record(UUID.randomUUID(), UUID.randomUUID(), ReferralStatus.PENDING),
                1, true, false);
        assertEquals(List.of("copy"), ReferralMenuUi.visibleActionIds(redeemed));
    }

    private static ReferralRecord record(UUID invitee, UUID referrer, ReferralStatus status) {
        return new ReferralRecord(
                1L,
                invitee,
                referrer,
                "ABC",
                status,
                0L,
                0L,
                null,
                null,
                null,
                0L,
                0.0,
                0,
                0);
    }
}