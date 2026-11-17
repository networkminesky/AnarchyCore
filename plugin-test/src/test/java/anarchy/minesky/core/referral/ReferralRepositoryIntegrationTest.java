package anarchy.minesky.core.referral;

import anarchy.minesky.core.impl.database.DatabaseManager;
import anarchy.minesky.core.impl.referral.ReferralCodeGenerator;
import anarchy.minesky.core.impl.referral.ReferralRepository;
import anarchy.minesky.core.impl.referral.ReferralStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferralRepositoryIntegrationTest {

    private DatabaseManager database;
    private ReferralRepository repository;

    @BeforeEach
    void setUp() {
        database = ReferralFixtures.database("referral_repo");
        repository = new ReferralRepository(database);
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void cycleDetectionBlocksMutualReferral() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        ReferralCodeGenerator generator = new ReferralCodeGenerator(ReferralCodeGenerator.DEFAULT_CHARSET, 8);
        repository.getOrCreateCode(a, generator, 10);
        repository.getOrCreateCode(b, generator, 10);
        long now = System.currentTimeMillis();
        repository.createRedemption(a, b, repository.getCodeByPlayer(b), now, now + 86_400_000L);
        assertTrue(repository.wouldCreateCycle(a, b));
    }

    @Test
    void inviteeUniqueConstraintPreventsDoubleRedeem() {
        UUID referrer = UUID.randomUUID();
        UUID invitee = UUID.randomUUID();
        ReferralCodeGenerator generator = new ReferralCodeGenerator(ReferralCodeGenerator.DEFAULT_CHARSET, 8);
        String code = repository.getOrCreateCode(referrer, generator, 10);
        long now = System.currentTimeMillis();
        assertNotNull(repository.createRedemption(invitee, referrer, code, now, now + 86_400_000L));
        assertNull(repository.createRedemption(invitee, referrer, code, now, now + 86_400_000L));
        assertTrue(repository.hasInviteeEverRedeemed(invitee));
    }

    @Test
    void payoutTransitionIsIdempotentUnderConcurrency() throws Exception {
        UUID referrer = UUID.randomUUID();
        UUID invitee = UUID.randomUUID();
        ReferralCodeGenerator generator = new ReferralCodeGenerator(ReferralCodeGenerator.DEFAULT_CHARSET, 8);
        String code = repository.getOrCreateCode(referrer, generator, 10);
        long now = System.currentTimeMillis();
        Long id = repository.createRedemption(invitee, referrer, code, now, now + 86_400_000L);
        assertNotNull(id);
        repository.markQualified(id, now);

        AtomicInteger reserved = new AtomicInteger();
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    if (repository.reservePayout(id, "referral:" + id)) {
                        reserved.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();
        assertEquals(1, reserved.get());
    }

    @Test
    void failedPayoutCanReturnToRetryState() {
        UUID referrer = UUID.randomUUID();
        UUID invitee = UUID.randomUUID();
        ReferralCodeGenerator generator = new ReferralCodeGenerator(ReferralCodeGenerator.DEFAULT_CHARSET, 8);
        String code = repository.getOrCreateCode(referrer, generator, 10);
        long now = System.currentTimeMillis();
        Long id = repository.createRedemption(invitee, referrer, code, now, now + 86_400_000L);
        repository.markQualified(id, now);
        assertTrue(repository.reservePayout(id, "referral:" + id));
        assertTrue(repository.markPayoutFailed(id));
        assertTrue(repository.releasePayoutToRetry(id));
        var row = repository.getById(id);
        assertNotNull(row);
        assertEquals(ReferralStatus.QUALIFIED_PENDING_PAYOUT, row.status());
    }
}
