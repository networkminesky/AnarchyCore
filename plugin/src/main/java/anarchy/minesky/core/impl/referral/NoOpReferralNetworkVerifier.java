package anarchy.minesky.core.impl.referral;

import org.bukkit.entity.Player;

import java.net.InetAddress;

public final class NoOpReferralNetworkVerifier implements ReferralNetworkVerifier {

    @Override
    public ReferralRejectReason checkJoinNetwork(Player player, InetAddress address) {
        return null;
    }
}