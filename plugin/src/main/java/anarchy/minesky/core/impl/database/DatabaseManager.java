package anarchy.minesky.core.impl.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class DatabaseManager {

    private final Plugin plugin;
    private HikariDataSource dataSource;
    private ExecutorService executor;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start(DatabaseSettings settings) {
        shutdown();
        if (!settings.enabled()) {
            plugin.getLogger().info("Database desativado em config.yml — preferências só em memória.");
            return;
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.jdbcUrl());
        config.setUsername(settings.username());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(settings.poolSize());
        config.setConnectionTimeout(settings.connectionTimeoutMs());
        config.setPoolName("AnarchyCore-Hikari");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        this.dataSource = new HikariDataSource(config);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        runSync(this::migrate);
        plugin.getLogger().info("MariaDB conectado.");
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    public boolean isActive() {
        return dataSource != null && !dataSource.isClosed();
    }

    public void runAsync(Runnable task) {
        if (executor == null) {
            task.run();
            return;
        }
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                plugin.getLogger().severe("Erro async DB: " + e.getMessage());
            }
        });
    }

    public void runSync(Runnable task) {
        task.run();
    }

    public <T> T querySync(SqlFunction<T> function) {
        if (!isActive()) {
            return null;
        }
        try (Connection connection = dataSource.getConnection()) {
            return function.apply(connection);
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro SQL: " + e.getMessage());
            return null;
        }
    }

    public void withConnection(Consumer<Connection> consumer) {
        if (!isActive()) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            consumer.accept(connection);
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro SQL: " + e.getMessage());
        }
    }

    private void migrate() {
        withConnection(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS ac_player_prefs (
                          uuid CHAR(36) PRIMARY KEY,
                          allow_tpa_strangers TINYINT(1) NOT NULL DEFAULT 1,
                          allow_tpahere_strangers TINYINT(1) NOT NULL DEFAULT 1,
                          allow_pay_strangers TINYINT(1) NOT NULL DEFAULT 1,
                          payment_sounds TINYINT(1) NOT NULL DEFAULT 1,
                          allow_tell_strangers TINYINT(1) NOT NULL DEFAULT 1,
                          tell_sounds TINYINT(1) NOT NULL DEFAULT 1,
                          scoreboard_enabled TINYINT(1) NOT NULL DEFAULT 1
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS ac_player_known (
                          player_uuid CHAR(36) NOT NULL,
                          known_uuid CHAR(36) NOT NULL,
                          PRIMARY KEY (player_uuid, known_uuid),
                          INDEX idx_known_reverse (known_uuid, player_uuid)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
            } catch (SQLException e) {
                plugin.getLogger().severe("Falha na migração SQL: " + e.getMessage());
            }
        });
    }

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }
}
