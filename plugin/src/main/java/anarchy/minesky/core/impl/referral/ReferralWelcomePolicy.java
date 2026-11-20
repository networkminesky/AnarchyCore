package anarchy.minesky.core.impl.referral;

public final class ReferralWelcomePolicy {

    private ReferralWelcomePolicy() {}

    /**
     * Primeira sessão ({@code !hasPlayedBefore}), com permissão base, sem resgate,
     * sem bloqueio e sem o diálogo já visto ou pulado.
     */
    public static boolean shouldOpen(
            boolean systemEnabled,
            boolean dialogEnabled,
            boolean firstJoin,
            boolean hasBasePermission,
            boolean seenDialog,
            boolean alreadyRedeemed,
            boolean blocked) {
        return systemEnabled
                && dialogEnabled
                && firstJoin
                && hasBasePermission
                && !seenDialog
                && !alreadyRedeemed
                && !blocked;
    }
}
