package anarchy.minesky.core.referral;

import anarchy.minesky.core.impl.database.DatabaseManager;
import anarchy.minesky.core.impl.referral.IpHistoryRepository;
import anarchy.minesky.core.impl.referral.NoOpReferralNetworkVerifier;
import anarchy.minesky.core.impl.referral.PlayerIdentityRepository;
import anarchy.minesky.core.impl.referral.ReferralAntiAltChecker;
import anarchy.minesky.core.impl.referral.ReferralCodeGenerator;
import anarchy.minesky.core.impl.referral.ReferralQualificationTracker;
import anarchy.minesky.core.impl.referral.ReferralRejectReason;
import anarchy.minesky.core.impl.referral.ReferralRepository;
import anarchy.minesky.core.impl.referral.ReferralSettings;
import anarchy.minesky.core.impl.referral.ReferralStatus;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.sql.PreparedStatement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReferralSecurityTest {

    private DatabaseManager database;
    private ReferralRepository repository;
    private ReferralAntiAltChecker checker;
    private ReferralQualificationTracker tracker;
    private PlayerIdentityRepository identity;

    @BeforeEach
    void setUp() {
        Plugin plugin = TestPlugins.create(ReferralFixtures.config("referral_security"));
        database = new anarchy.minesky.core.impl.database.DatabaseManager(plugin);
        database.start(new anarchy.minesky.core.impl.database.DatabaseSettings(plugin));
        ReferralSettings settings = new ReferralSettings(plugin);
        repository = new ReferralRepository(database);
        identity = new PlayerIdentityRepository(database);
        IpHistoryRepository ipHistory = new IpHistoryRepository(database, settings);
        checker = new ReferralAntiAltChecker(settings, repository, ipHistory, identity, new NoOpReferralNetworkVerifier());
        tracker = new ReferralQualificationTracker(plugin, settings, repository);
        this.ipHistory = ipHistory;
    }

    private IpHistoryRepository ipHistory;

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void rejectsSelfReferral() {
        UUID uuid = UUID.randomUUID();
        assertEquals(ReferralRejectReason.SELF_REFERRAL, checker.validateRedeem(ReferralFixtures.player(uuid, null), uuid));
    }

    @Test
    void rejectsMutualCycle() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        ReferralCodeGenerator generator = new ReferralCodeGenerator(ReferralCodeGenerator.DEFAULT_CHARSET, 8);
        repository.getOrCreateCode(b, generator, 10);
        long now = System.currentTimeMillis();
        repository.createRedemption(a, b, repository.getCodeByPlayer(b), now, now + 86_400_000L);
        assertEquals(
                ReferralRejectReason.CIRCULAR_REFERRAL,
                checker.validateRedeem(ReferralFixtures.player(b, null), a));
    }

    @Test
    void rejectsExpiredFirstJoinAndSecondRedeem() throws Exception {
        UUID referrer = UUID.randomUUID();
        UUID invitee = UUID.randomUUID();
        identity.ensureFirstJoin(invitee, "vanilla");
        database.withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE ac_player_identity SET first_join_at = ? WHERE player_uuid = ?")) {
                ps.setLong(1, System.currentTimeMillis() - 48L * 3_600_000L);
                ps.setString(2, invitee.toString());
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals(
                ReferralRejectReason.ACCOUNT_TOO_OLD,
                checker.validateRedeem(ReferralFixtures.player(invitee, null), referrer));

        ReferralCodeGenerator generator = new ReferralCodeGenerator(ReferralCodeGenerator.DEFAULT_CHARSET, 8);
        String code = repository.getOrCreateCode(referrer, generator, 10);
        long now = System.currentTimeMillis();
        repository.createRedemption(invitee, referrer, code, now, now + 86_400_000L);
        assertEquals(
                ReferralRejectReason.ALREADY_REDEEMED,
                checker.validateRedeem(ReferralFixtures.player(invitee, null), referrer));
    }

    @Test
    void rejectsSameCurrentAndHistoricalIpAndSubnet() throws Exception {
        UUID referrer = UUID.randomUUID();
        UUID invitee = UUID.randomUUID();
        InetAddress shared = InetAddress.getByName("203.0.113.20");
        ipHistory.recordIp(referrer, shared);
        ipHistory.recordIp(invitee, shared);
        assertEquals(
                ReferralRejectReason.SAME_IP,
                checker.validateRedeem(ReferralFixtures.player(invitee, shared), referrer));

        UUID movedA = UUID.randomUUID();
        UUID movedB = UUID.randomUUID();
        ipHistory.recordIp(movedA, InetAddress.getByName("198.51.100.5"));
        ipHistory.recordIp(movedB, InetAddress.getByName("198.51.100.9"));
        ipHistory.recordIp(movedA, InetAddress.getByName("198.51.100.40"));
        ipHistory.recordIp(movedB, InetAddress.getByName("198.51.100.40"));
        assertEquals(
                ReferralRejectReason.SAME_IP,
                checker.validateRedeem(ReferralFixtures.player(movedB, null), movedA));

        UUID netA = UUID.randomUUID();
        UUID netB = UUID.randomUUID();
        ipHistory.recordIp(netA, InetAddress.getByName("10.8.0.2"));
        ipHistory.recordIp(netB, InetAddress.getByName("10.8.0.200"));
        assertEquals(
                ReferralRejectReason.SAME_SUBNET,
                checker.validateRedeem(ReferralFixtures.player(netB, null), netA));
    }

    @Test
    void incompleteQualificationDoesNotReleasePayout() {
        UUID referrer = UUID.randomUUID();
        UUID invitee = UUID.randomUUID();
        ReferralCodeGenerator generator = new ReferralCodeGenerator(ReferralCodeGenerator.DEFAULT_CHARSET, 8);
        String code = repository.getOrCreateCode(referrer, generator, 10);
        long now = System.currentTimeMillis();
        Long id = repository.createRedemption(invitee, referrer, code, now, now + 86_400_000L);
        repository.updateQualificationProgress(id, 60_000L, 10.0, 1, 0);
        assertFalse(tracker.isQualified(invitee));
        assertEquals(ReferralStatus.PENDING, repository.getByInvitee(invitee).status());
        assertNull(repository.getByInvitee(invitee).qualifiedAt());
    }
}
