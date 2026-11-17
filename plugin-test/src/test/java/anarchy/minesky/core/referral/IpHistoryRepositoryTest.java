package anarchy.minesky.core.referral;

import anarchy.minesky.core.impl.database.DatabaseManager;
import anarchy.minesky.core.impl.referral.IpHistoryRepository;
import anarchy.minesky.core.impl.referral.ReferralSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpHistoryRepositoryTest {

    private DatabaseManager database;
    private IpHistoryRepository ipHistory;

    @BeforeEach
    void setUp() {
        database = ReferralFixtures.database("ip_hist");
        ReferralSettings settings = ReferralFixtures.settings("ip_hist_settings");
        ipHistory = new IpHistoryRepository(database, settings);
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void detectsCurrentSharedIp() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        InetAddress shared = InetAddress.getByName("198.51.100.10");
        ipHistory.recordIp(a, shared);
        ipHistory.recordIp(b, shared);
        assertTrue(ipHistory.shareAnyIp(a, b));
    }

    @Test
    void detectsHistoricalSharedIpAfterChange() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        ipHistory.recordIp(a, InetAddress.getByName("198.51.100.10"));
        ipHistory.recordIp(b, InetAddress.getByName("198.51.100.20"));
        assertFalse(ipHistory.shareAnyIp(a, b));

        ipHistory.recordIp(a, InetAddress.getByName("203.0.113.50"));
        ipHistory.recordIp(b, InetAddress.getByName("203.0.113.50"));
        assertTrue(ipHistory.shareAnyIp(a, b));
    }

    @Test
    void detectsSameIpv4SubnetWithoutExactIp() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        ipHistory.recordIp(a, InetAddress.getByName("192.168.1.10"));
        ipHistory.recordIp(b, InetAddress.getByName("192.168.1.200"));
        assertFalse(ipHistory.shareAnyIp(a, b));
        assertTrue(ipHistory.shareAnySubnet(a, b));
    }

    @Test
    void detectsSameIpv6Subnet64() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        ipHistory.recordIp(a, InetAddress.getByName("2001:db8:1:2::10"));
        ipHistory.recordIp(b, InetAddress.getByName("2001:db8:1:2::abcd"));
        ipHistory.recordIp(c, InetAddress.getByName("2001:db8:1:3::10"));
        assertTrue(ipHistory.shareAnySubnet(a, b));
        assertFalse(ipHistory.shareAnySubnet(a, c));
    }
}
