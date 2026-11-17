package anarchy.minesky.core.impl.referral;

import org.bukkit.entity.Player;

import java.net.InetAddress;

/**
 * Hook de VPN, proxy ou ASN. A implementação padrão não consulta nenhum provedor.
 * Retorne {@link ReferralRejectReason#VPN_DETECTED} para bloquear o resgate.
 */
public interface ReferralNetworkVerifier {

    ReferralRejectReason checkJoinNetwork(Player player, InetAddress address);

    default ReferralRejectReason checkJoinNetwork(Player player) {
        if (player == null || player.getAddress() == null) {
            return null;
        }
        return checkJoinNetwork(player, player.getAddress().getAddress());
    }
}