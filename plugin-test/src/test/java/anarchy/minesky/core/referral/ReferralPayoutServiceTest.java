package anarchy.minesky.core.referral;

import anarchy.minesky.core.impl.database.DatabaseManager;
import anarchy.minesky.core.impl.database.DatabaseSettings;
import anarchy.minesky.core.impl.referral.ReferralAuditRepository;
import anarchy.minesky.core.impl.referral.ReferralCodeGenerator;
import anarchy.minesky.core.impl.referral.ReferralPayoutService;
import anarchy.minesky.core.impl.referral.ReferralRepository;
import anarchy.minesky.core.impl.referral.ReferralSettings;
import anarchy.minesky.core.impl.referral.ReferralStatus;
import anarchy.minesky.core.impl.referral.economy.AnarchyEconomyFragmentProvider;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferralPayoutServiceTest {

    private DatabaseManager database;
    private ReferralRepository repository;
    private ReferralPayoutService payout;
    private ReferralFixtures.FakeEconomy economy;
    private Plugin plugin;

    @BeforeEach
    void setUp() {
        plugin = TestPlugins.create(ReferralFixtures.config("referral_payout"));
        database = new DatabaseManager(plugin);
        database.start(new DatabaseSettings(plugin));
        repository = new ReferralRepository(database);
        economy = new ReferralFixtures.FakeEconomy();
        payout = new ReferralPayoutService(
                plugin,
                new ReferralSettings(plugin),
                repository,
                new ReferralAuditRepository(database),
                economy);
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void concurrentPayoutCallsEconomyOnce() throws Exception {
        long id = qualified();
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    payout.processPayout(repository.getById(id), null);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();
        assertEquals(1, economy.deposits());
        assertEquals(ReferralStatus.REWARDED, repository.getById(id).status());
    }

    @Test
    void economyFailureStaysRetryableWithoutDuplicateCredit() {
        long id = qualified();
        economy.setFail(true);
        payout.processPayout(repository.getById(id), null);
        assertEquals(ReferralStatus.PAYOUT_FAILED, repository.getById(id).status());
        assertEquals(1, economy.deposits());

        payout.processPayout(repository.getById(id), null);
        assertEquals(1, economy.deposits());
        assertEquals(ReferralStatus.PAYOUT_FAILED, repository.getById(id).status());

        economy.setFail(false);
        payout.reprocessPayout(id);
        assertEquals(ReferralStatus.REWARDED, repository.getById(id).status());
        assertEquals(2, economy.deposits());

        payout.processPayout(repository.getById(id), null);
        payout.reprocessPayout(id);
        assertEquals(2, economy.deposits());
        assertEquals(ReferralStatus.REWARDED, repository.getById(id).status());
    }

    @Test
    void unavailableAdapterKeepsQualifiedWithoutLoadingEconomyApi() {
        AnarchyEconomyFragmentProvider adapter = new AnarchyEconomyFragmentProvider(plugin);
        assertFalse(adapter.isAvailable());
        ReferralPayoutService offline = new ReferralPayoutService(
                plugin,
                new ReferralSettings(plugin),
                repository,
                new ReferralAuditRepository(database),
                adapter);
        long id = qualified();
        offline.processPayout(repository.getById(id), null);
        assertEquals(ReferralStatus.QUALIFIED_PENDING_PAYOUT, repository.getById(id).status());
        assertTrue(adapter.depositShards(UUID.randomUUID(), "offline", 1.0, "referral:" + id).isCompletedExceptionally());
    }

    private long qualified() {
        UUID referrer = UUID.randomUUID();
        UUID invitee = UUID.randomUUID();
        ReferralCodeGenerator generator = new ReferralCodeGenerator(ReferralCodeGenerator.DEFAULT_CHARSET, 8);
        String code = repository.getOrCreateCode(referrer, generator, 10);
        long now = System.currentTimeMillis();
        Long id = repository.createRedemption(invitee, referrer, code, now, now + 86_400_000L);
        assertNotNull(id);
        assertTrue(repository.markQualified(id, now));
        return id;
    }
}
