package anarchy.minesky.core.impl.database;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public final class DatabaseSettings {

    private final boolean enabled;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int poolSize;
    private final long connectionTimeoutMs;

    public DatabaseSettings(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();
        this.enabled = config.getBoolean("database.enabled", true);
        this.jdbcUrl = config.getString("database.jdbc-url", "jdbc:mariadb://127.0.0.1:3306/anarchycore");
        this.username = config.getString("database.username", "root");
        this.password = config.getString("database.password", "");
        this.poolSize = Math.max(2, config.getInt("database.pool-size", 10));
        this.connectionTimeoutMs = Math.max(1000L, config.getLong("database.connection-timeout-ms", 10000L));
    }

    public boolean enabled() {
        return enabled;
    }

    public String jdbcUrl() {
        return jdbcUrl;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public int poolSize() {
        return poolSize;
    }

    public long connectionTimeoutMs() {
        return connectionTimeoutMs;
    }
}
