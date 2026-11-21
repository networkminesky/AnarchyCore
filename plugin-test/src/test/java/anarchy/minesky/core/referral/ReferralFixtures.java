package anarchy.minesky.core.referral;

import anarchy.minesky.core.impl.database.DatabaseManager;
import anarchy.minesky.core.impl.database.DatabaseSettings;
import anarchy.minesky.core.impl.referral.ReferralSettings;
import anarchy.minesky.core.impl.referral.economy.FragmentEconomyProvider;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class ReferralFixtures {

    private ReferralFixtures() {}

    public static YamlConfiguration config(String databaseName) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("database.enabled", true);
        config.set("database.jdbc-url",
                "jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        config.set("database.username", "sa");
        config.set("database.password", "");
        config.set("database.pool-size", 8);
        config.set("database.connection-timeout-ms", 5000);
        config.set("referral.enabled", true);
        config.set("referral.security.new-account-max-age-hours", 24);
        config.set("referral.security.check-same-ip", true);
        config.set("referral.security.check-same-subnet", true);
        config.set("referral.security.ipv4-slash24", true);
        config.set("referral.security.ipv6-slash64", true);
        config.set("referral.security.max-redemptions-per-subnet", 1);
        config.set("referral.security.cycle-depth", 16);
        config.set("referral.rewards.referrer-shards", 10.0);
        config.set("referral.rewards.invitee-bonus-shards", 0.0);
        config.set("referral.qualification.min-active-playtime-minutes", 120);
        config.set("referral.qualification.min-distance", 5000.0);
        config.set("referral.qualification.min-blocks-mined", 100);
        config.set("referral.qualification.min-blocks-placed", 25);
        return config;
    }

    public static DatabaseManager database(String name) {
        Plugin plugin = TestPlugins.create(config(name));
        DatabaseManager database = new DatabaseManager(plugin);
        database.start(new DatabaseSettings(plugin));
        return database;
    }

    static ReferralSettings settings(String name) {
        return new ReferralSettings(TestPlugins.create(config(name)));
    }

    static Player player(UUID uuid, InetAddress address) {
        InetSocketAddress socket = address == null ? null : new InetSocketAddress(address, 25565);
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> uuid;
                    case "getAddress" -> socket;
                    case "getName" -> "tester";
                    case "isOnline" -> true;
                    case "isBanned" -> false;
                    case "toString" -> uuid.toString();
                    case "hashCode" -> uuid.hashCode();
                    case "equals" -> args != null && args.length == 1 && proxy == args[0];
                    default -> {
                        Class<?> type = method.getReturnType();
                        if (type == boolean.class) {
                            yield false;
                        }
                        if (type == int.class) {
                            yield 0;
                        }
                        if (type == long.class) {
                            yield 0L;
                        }
                        if (type == double.class) {
                            yield 0.0;
                        }
                        yield null;
                    }
                });
    }

    static final class FakeEconomy implements FragmentEconomyProvider {
        private final AtomicInteger deposits = new AtomicInteger();
        private volatile boolean available = true;
        private volatile boolean fail;

        int deposits() {
            return deposits.get();
        }

        void setAvailable(boolean available) {
            this.available = available;
        }

        void setFail(boolean fail) {
            this.fail = fail;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public CompletableFuture<Double> depositShards(UUID targetUuid, String targetName, double amount, String auditKey) {
            deposits.incrementAndGet();
            if (fail) {
                return CompletableFuture.failedFuture(new IllegalStateException("simulated failure"));
            }
            return CompletableFuture.completedFuture(amount);
        }

        @Override
        public CompletableFuture<Void> ensureAccount(UUID uuid, String name) {
            if (!available) {
                return CompletableFuture.failedFuture(new IllegalStateException("AnarchyEconomy indisponível"));
            }
            return CompletableFuture.completedFuture(null);
        }
    }
}
