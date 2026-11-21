package anarchy.minesky.core.presence;

import anarchy.minesky.core.impl.database.DatabaseManager;
import anarchy.minesky.core.impl.presence.PlayerPresenceRecord;
import anarchy.minesky.core.impl.presence.PlayerPresenceRepository;
import anarchy.minesky.core.referral.ReferralFixtures;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresenceRepositoryTest {

    private DatabaseManager database;
    private PlayerPresenceRepository repository;

    @BeforeEach
    void setUp() {
        database = ReferralFixtures.database("presence");
        repository = new PlayerPresenceRepository(database);
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void joinQuitRoundTripAndNameLookup() throws Exception {
        UUID uuid = UUID.randomUUID();
        long join = 1_700_000_000_000L;
        long quit = join + TimeUnit.HOURS.toMillis(2);
        repository.upsertJoinSync(uuid, "Tester", join);
        Location location = fakeLocation("world", 10.5, 64.0, -3.25, 90f, 0f);
        repository.saveQuitSync(uuid, "Tester", quit, location);

        PlayerPresenceRecord byUuid =
                repository.findByUuid(uuid).get(5, TimeUnit.SECONDS).orElseThrow();
        assertEquals("Tester", byUuid.playerName());
        assertEquals(join, byUuid.lastJoinAtMillis());
        assertEquals(quit, byUuid.lastQuitAtMillis());
        assertEquals("world", byUuid.world());
        assertEquals(10.5, byUuid.x(), 0.001);
        assertTrue(byUuid.hasLogoutLocation());

        PlayerPresenceRecord byName = repository.findByNameIgnoreCase("tester")
                .get(5, TimeUnit.SECONDS)
                .orElseThrow();
        assertEquals(uuid, byName.uuid());
    }

    private static Location fakeLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        World world = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[] {World.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) {
                        return worldName;
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    return null;
                });
        return new Location(world, x, y, z, yaw, pitch);
    }
}