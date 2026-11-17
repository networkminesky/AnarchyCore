package anarchy.minesky.core.impl.referral;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

public final class NetworkAddressUtils {

    private NetworkAddressUtils() {}

    public static String normalizeIp(InetAddress address) {
        if (address == null) {
            return "";
        }
        return address.getHostAddress();
    }

  /** IPv4 → prefixo /24; IPv6 → prefixo /64 (4 primeiros hextetos). */
    public static String subnetKey(InetAddress address, boolean ipv4Slash24, boolean ipv6Slash64) {
        if (address == null) {
            return "";
        }
        if (address instanceof Inet4Address) {
            if (!ipv4Slash24) {
                return normalizeIp(address);
            }
            byte[] bytes = address.getAddress();
            return (bytes[0] & 0xFF) + "." + (bytes[1] & 0xFF) + "." + (bytes[2] & 0xFF) + ".0/24";
        }
        if (address instanceof Inet6Address inet6) {
            if (!ipv6Slash64) {
                return normalizeIp(inet6);
            }
            byte[] bytes = inet6.getAddress();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i += 2) {
                if (i > 0) {
                    sb.append(':');
                }
                int value = ((bytes[i] & 0xFF) << 8) | (bytes[i + 1] & 0xFF);
                sb.append(Integer.toHexString(value));
            }
            return sb + "::/64";
        }
        return normalizeIp(address);
    }
}