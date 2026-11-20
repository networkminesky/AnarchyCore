package anarchy.minesky.core.referral;

import anarchy.minesky.core.impl.database.DatabaseManager;
import anarchy.minesky.core.impl.referral.ReferralCustomCodeRules;
import anarchy.minesky.core.impl.referral.ReferralRepository;
import anarchy.minesky.core.impl.referral.ReferralSettings;
import anarchy.minesky.core.impl.referral.ReferralStatus;
import anarchy.minesky.core.impl.referral.ReferralWelcomeGate;
import anarchy.minesky.core.impl.referral.ReferralWelcomePolicy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferralCustomCodeAndWelcomeTest {

    private DatabaseManager database;
    private ReferralRepository repository;

    @BeforeEach
    void setUp() {
        database = ReferralFixtures.database("referral_custom");
        repository = new ReferralRepository(database);
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void customCodeRejectsFormatReservedWordsAndBlacklist() {
        ReferralCustomCodeRules rules = new ReferralCustomCodeRules(
                ReferralCustomCodeRules.DEFAULT_PATTERN,
                3,
                16,
                3_600_000L,
                5,
                List.of("USAR", "STATUS", "DEFINIR", "ADMIN"),
                List.of("STAFF", "PORNO", "MOD"));

        assertNull(rules.validate(rules.normalize("mina_1")));
        assertEquals("MINA-2", rules.normalize("  mina-2  "));
        assertNull(rules.normalize("mi na"));
        assertNull(rules.normalize("<red>ABC"));
        assertNull(rules.normalize("&cABC"));
        assertEquals(ReferralCustomCodeRules.Problem.INVALID, rules.validate(rules.normalize("AB")));
        assertEquals(ReferralCustomCodeRules.Problem.INVALID, rules.validate(rules.normalize("ABCDEFGHIJKLMNOPQ")));
        assertEquals(ReferralCustomCodeRules.Problem.INVALID, rules.validate(rules.normalize("AAA BBB")));
        assertEquals(ReferralCustomCodeRules.Problem.RESERVED, rules.validate(rules.normalize("usar")));
        assertEquals(ReferralCustomCodeRules.Problem.RESERVED, rules.validate(rules.normalize("definir")));
        assertEquals(ReferralCustomCodeRules.Problem.RESERVED, rules.validate(rules.normalize("admin")));
        assertEquals(ReferralCustomCodeRules.Problem.BLACKLISTED, rules.validate(rules.normalize("staff")));
        assertEquals(ReferralCustomCodeRules.Problem.BLACKLISTED, rules.validate(rules.normalize("xporno")));
        assertEquals(ReferralCustomCodeRules.Problem.BLACKLISTED, rules.validate(rules.normalize("mod")));
        assertNull(rules.validate(rules.normalize("moda")));

        long now = 10_000L;
        assertNull(rules.checkChange(0, 0L, now));
        assertEquals(ReferralCustomCodeRules.ChangeProblem.COOLDOWN, rules.checkChange(1, now - 1_000L, now));
        assertNull(rules.checkChange(1, now - 3_600_000L, now));
        assertEquals(ReferralCustomCodeRules.ChangeProblem.LIMIT, rules.checkChange(5, 0L, now));
    }

    @Test
    void settingsLoadCustomRulesFromConfig() {
        YamlConfiguration config = ReferralFixtures.config("referral_custom_settings");
        config.set("referral.custom-code.min-length", 4);
        config.set("referral.custom-code.max-length", 32);
        config.set("referral.custom-code.pattern", "^[A-Z0-9_-]+$");
        config.set("referral.custom-code.blacklist", List.of("HACK"));
        config.set("referral.custom-code.change-cooldown-seconds", 10);
        config.set("referral.custom-code.max-changes", 2);
        config.set("referral.first-join-dialog.delay-ticks", 30);
        ReferralSettings settings = new ReferralSettings(TestPlugins.create(config));
        ReferralCustomCodeRules rules = settings.customCodeRules();
        assertEquals(4, rules.minLength());
        assertEquals(16, rules.maxLength());
        assertEquals(ReferralCustomCodeRules.Problem.BLACKLISTED, rules.validate(rules.normalize("hack")));
        assertEquals(ReferralCustomCodeRules.Problem.RESERVED, rules.validate(rules.normalize("status")));
        assertEquals(30L, settings.welcomeDialogDelayTicks());
        assertTrue(settings.welcomeDialogEnabled());
    }

    @Test
    void customCodeIsUniqueIgnoringCaseAndKeepsOldReferrals() throws Exception {
        UUID referrer = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID invitee = UUID.randomUUID();
        long now = System.currentTimeMillis();

        database.withConnection(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO ac_referral_codes
                      (player_uuid, code, created_at, blocked, code_changed_at, code_change_count)
                    VALUES (?, 'abc1', ?, 0, 0, 0)
                    """)) {
                ps.setString(1, referrer.toString());
                ps.setLong(2, now);
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertEquals(ReferralRepository.AssignResult.TAKEN, repository.assignCustomCode(other, "ABC1", now));
        assertEquals(referrer, repository.getReferrerByCode("abc1"));
        assertEquals(ReferralRepository.AssignResult.UNCHANGED, repository.assignCustomCode(referrer, "abc1", now));
        assertEquals(0, repository.codeState(referrer).changeCount());

        assertEquals(ReferralRepository.AssignResult.APPLIED, repository.assignCustomCode(referrer, "mina_nova", now));
        assertEquals("MINA_NOVA", repository.getCodeByPlayer(referrer));
        assertEquals(1, repository.codeState(referrer).changeCount());

        Long id = repository.createRedemption(invitee, referrer, "MINA_NOVA", now, now + 86_400_000L);
        assertNotNull(id);
        assertEquals(ReferralRepository.AssignResult.APPLIED, repository.assignCustomCode(referrer, "outro_codigo", now + 5));
        var pending = repository.getByInvitee(invitee);
        assertNotNull(pending);
        assertEquals(referrer, pending.referrerUuid());
        assertEquals(ReferralStatus.PENDING, pending.status());
        assertEquals("MINA_NOVA", pending.code());

        assertTrue(repository.markQualified(id, now));
        assertTrue(repository.reservePayout(id, "referral:" + id));
        assertTrue(repository.markPayoutSent(id));
        assertTrue(repository.markRewarded(id, now));
        assertEquals(ReferralRepository.AssignResult.APPLIED, repository.assignCustomCode(referrer, "final_code", now + 9));
        var rewarded = repository.getById(id);
        assertNotNull(rewarded);
        assertEquals(ReferralStatus.REWARDED, rewarded.status());
        assertEquals(referrer, rewarded.referrerUuid());
        assertEquals(referrer, repository.getReferrerByCode("final_code"));
        assertNull(repository.getReferrerByCode("mina_nova"));
    }

    @Test
    void welcomeDialogOpensOnlyForEligibleFirstJoin() {
        assertTrue(ReferralWelcomePolicy.shouldOpen(true, true, true, true, false, false, false));
        assertFalse(ReferralWelcomePolicy.shouldOpen(false, true, true, true, false, false, false));
        assertFalse(ReferralWelcomePolicy.shouldOpen(true, false, true, true, false, false, false));
        assertFalse(ReferralWelcomePolicy.shouldOpen(true, true, false, true, false, false, false));
        assertFalse(ReferralWelcomePolicy.shouldOpen(true, true, true, false, false, false, false));
        assertFalse(ReferralWelcomePolicy.shouldOpen(true, true, true, true, true, false, false));
        assertFalse(ReferralWelcomePolicy.shouldOpen(true, true, true, true, false, true, false));
        assertFalse(ReferralWelcomePolicy.shouldOpen(true, true, true, true, false, false, true));

        UUID fresh = UUID.randomUUID();
        ReferralWelcomeGate freshGate = repository.loadWelcomeGate(fresh);
        assertNotNull(freshGate);
        assertTrue(ReferralWelcomePolicy.shouldOpen(
                true, true, true, true, freshGate.seen(), freshGate.redeemed(), freshGate.blocked()));

        repository.markWelcomeSeen(fresh);
        ReferralWelcomeGate seen = repository.loadWelcomeGate(fresh);
        assertTrue(seen.seen());
        assertFalse(ReferralWelcomePolicy.shouldOpen(
                true, true, true, true, seen.seen(), seen.redeemed(), seen.blocked()));

        UUID referrer = UUID.randomUUID();
        UUID invitee = UUID.randomUUID();
        long now = System.currentTimeMillis();
        assertEquals(ReferralRepository.AssignResult.APPLIED, repository.assignCustomCode(referrer, "HOST", now));
        assertNotNull(repository.createRedemption(invitee, referrer, "HOST", now, now + 86_400_000L));
        ReferralWelcomeGate redeemed = repository.loadWelcomeGate(invitee);
        assertTrue(redeemed.redeemed());
        assertFalse(redeemed.seen());
        assertFalse(ReferralWelcomePolicy.shouldOpen(
                true, true, true, true, redeemed.seen(), redeemed.redeemed(), redeemed.blocked()));

        repository.blockPlayer(fresh, "teste");
        ReferralWelcomeGate blocked = repository.loadWelcomeGate(fresh);
        assertTrue(blocked.blocked());
        assertFalse(ReferralWelcomePolicy.shouldOpen(
                true, true, true, true, blocked.seen(), blocked.redeemed(), blocked.blocked()));
    }
}
