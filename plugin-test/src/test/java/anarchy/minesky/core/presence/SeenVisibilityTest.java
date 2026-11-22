package anarchy.minesky.core.presence;

import anarchy.minesky.core.impl.presence.SeenOfflineResolver;
import anarchy.minesky.core.impl.presence.SeenVisibility;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeenVisibilityTest {

    @Test
    void vanishHiddenFromViewerWithoutPermission() {
        Player target = player(true, true, true);
        Player viewer = player(true, false, true);
        assertFalse(SeenVisibility.showAsOnline(target, viewer, false));
    }

    @Test
    void vanishVisibleWithVanishPermission() {
        Player target = player(true, true, true);
        Player viewer = player(true, false, true);
        assertTrue(SeenVisibility.showAsOnline(target, viewer, true));
    }

    @Test
    void offlineResolverPrefersPresenceQuitThenBukkitLastPlayed() {
        var record = new anarchy.minesky.core.impl.presence.PlayerPresenceRecord(
                UUID.randomUUID(),
                "a",
                1L,
                5_000L,
                "w",
                0.0,
                64.0,
                0.0,
                0f,
                0f);
        OfflinePlayer offline = (OfflinePlayer) Proxy.newProxyInstance(
                OfflinePlayer.class.getClassLoader(),
                new Class<?>[] {OfflinePlayer.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getLastPlayed" -> 100L;
                    default -> method.getReturnType() == boolean.class ? false : null;
                });
        assertTrue(SeenOfflineResolver.offlineSinceMillis(record, offline).orElse(-1) == 5_000L);
        assertTrue(SeenOfflineResolver.offlineSinceMillis(null, offline).orElse(-1) == 100L);
    }

    private static Player player(boolean online, boolean vanished, boolean canSee) {
        AtomicBoolean vanishedFlag = new AtomicBoolean(vanished);
        AtomicBoolean canSeeFlag = new AtomicBoolean(canSee);
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isOnline" -> online;
                    case "hasMetadata" -> vanishedFlag.get() && args != null && args.length == 1 && "vanished".equals(args[0]);
                    case "canSee" -> canSeeFlag.get();
                    case "getName" -> "spy";
                    case "getUniqueId" -> UUID.randomUUID();
                    default -> method.getReturnType() == boolean.class ? false : null;
                });
    }
}