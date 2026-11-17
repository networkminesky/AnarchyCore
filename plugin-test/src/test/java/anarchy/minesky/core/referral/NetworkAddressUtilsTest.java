package anarchy.minesky.core.referral;

import anarchy.minesky.core.impl.referral.NetworkAddressUtils;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class NetworkAddressUtilsTest {

    @Test
    void ipv4SubnetUsesSlash24() throws Exception {
        InetAddress address = InetAddress.getByName("203.0.113.42");
        assertEquals("203.0.113.0/24", NetworkAddressUtils.subnetKey(address, true, true));
    }

    @Test
    void sameSubnetDifferentHostOctet() throws Exception {
        InetAddress a = InetAddress.getByName("10.0.0.1");
        InetAddress b = InetAddress.getByName("10.0.0.99");
        assertEquals(
                NetworkAddressUtils.subnetKey(a, true, true),
                NetworkAddressUtils.subnetKey(b, true, true));
    }

    @Test
    void differentSubnet() throws Exception {
        InetAddress a = InetAddress.getByName("10.0.0.1");
        InetAddress b = InetAddress.getByName("10.0.1.1");
        assertNotEquals(
                NetworkAddressUtils.subnetKey(a, true, true),
                NetworkAddressUtils.subnetKey(b, true, true));
    }

    @Test
    void ipv6SubnetUsesSlash64() throws Exception {
        InetAddress a = InetAddress.getByName("2001:db8:1:2::1");
        InetAddress b = InetAddress.getByName("2001:db8:1:2::ffff");
        InetAddress c = InetAddress.getByName("2001:db8:1:3::1");
        assertEquals(NetworkAddressUtils.subnetKey(a, true, true), NetworkAddressUtils.subnetKey(b, true, true));
        assertFalse(NetworkAddressUtils.subnetKey(a, true, true).equals(NetworkAddressUtils.subnetKey(c, true, true)));
        assertTrueEndsWith64(NetworkAddressUtils.subnetKey(a, true, true));
    }

    private static void assertTrueEndsWith64(String subnet) {
        assertEquals(true, subnet.endsWith("::/64"));
    }
}
