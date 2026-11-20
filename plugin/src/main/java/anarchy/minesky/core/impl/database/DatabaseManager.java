package anarchy.minesky.core.impl.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class DatabaseManager {

    private final Plugin plugin;
    private HikariDataSource dataSource;
    private ExecutorService executor;
    private volatile boolean acceptingAsync = true;

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
        this.acceptingAsync = true;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        runSync(this::migrate);
        plugin.getLogger().info("MariaDB conectado.");
    }

    public void shutdown() {
        acceptingAsync = false;
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    public Plugin plugin() {
        return plugin;
    }

    public boolean isActive() {
        return dataSource != null && !dataSource.isClosed();
    }

    public void runAsync(Runnable task) {
        if (!acceptingAsync || executor == null) {
            return;
        }
        executor.execute(() -> {
            if (!acceptingAsync) {
                return;
            }
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
        try {
            return querySyncThrowing(function);
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro SQL: " + e.getMessage());
            return null;
        }
    }

    public <T> T querySyncThrowing(SqlFunction<T> function) throws SQLException {
        if (!isActive()) {
            throw new SQLException("Database inactive");
        }
        try (Connection connection = dataSource.getConnection()) {
            return function.apply(connection);
        }
    }

    public <T> void queryAsync(SqlFunction<T> function, AsyncCallback<T> callback) {
        submit(function).whenComplete((value, error) -> {
            if (error == null) {
                callback.complete(value, null);
                return;
            }
            Throwable cause = error instanceof java.util.concurrent.CompletionException && error.getCause() != null
                    ? error.getCause()
                    : error;
            SQLException sql = cause instanceof SQLException sqlException
                    ? sqlException
                    : new SQLException(cause.getMessage(), cause);
            callback.complete(null, sql);
        });
    }

    public <T> java.util.concurrent.CompletableFuture<T> submit(SqlFunction<T> function) {
        java.util.concurrent.CompletableFuture<T> future = new java.util.concurrent.CompletableFuture<>();
        if (!isActive() || !acceptingAsync || executor == null) {
            future.completeExceptionally(new SQLException("Database inactive"));
            return future;
        }
        try {
            executor.execute(() -> {
                if (!acceptingAsync || !isActive()) {
                    future.completeExceptionally(new SQLException("Database inactive"));
                    return;
                }
                try {
                    future.complete(querySyncThrowing(function));
                } catch (Exception e) {
                    plugin.getLogger().severe("Erro SQL async: " + e.getMessage());
                    future.completeExceptionally(e);
                }
            });
        } catch (RuntimeException e) {
            future.completeExceptionally(e);
        }
        return future;
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
                          tpa_auto TINYINT(1) NOT NULL DEFAULT 0,
                          config_options JSON NOT NULL DEFAULT ('{}')
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
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS ac_player_data (
                          uuid CHAR(36) PRIMARY KEY,
                          latest_nickname VARCHAR(16) NULL,
                          data JSON NOT NULL,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          INDEX idx_latest_nickname (latest_nickname)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS ac_player_homes (
                          owner_uuid CHAR(36) NOT NULL,
                          home_id VARCHAR(32) NOT NULL,
                          display_name VARCHAR(64) NOT NULL,
                          icon_material VARCHAR(64) NOT NULL DEFAULT 'BLUE_BED',
                          world VARCHAR(64) NOT NULL,
                          x DOUBLE NOT NULL,
                          y DOUBLE NOT NULL,
                          z DOUBLE NOT NULL,
                          yaw FLOAT NOT NULL,
                          pitch FLOAT NOT NULL,
                          PRIMARY KEY (owner_uuid, home_id),
                          INDEX idx_owner (owner_uuid)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
                try {
                    statement.execute("""
                            ALTER TABLE ac_player_homes
                            ADD COLUMN icon_material VARCHAR(64) NOT NULL DEFAULT 'BLUE_BED'
                            """);
                } catch (SQLException ignored) {
                }
                try {
                    statement.execute("""
                            ALTER TABLE ac_player_prefs
                            ADD COLUMN tpa_auto TINYINT(1) NOT NULL DEFAULT 0
                            """);
                } catch (SQLException ignored) {
                }
                try {
                    statement.execute("""
                            ALTER TABLE ac_player_prefs
                            ADD COLUMN config_options JSON NOT NULL DEFAULT ('{}')
                            """);
                } catch (SQLException ignored) {
                }
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS ac_guard_strikes (
                          player_uuid CHAR(36) PRIMARY KEY,
                          strikes INT NOT NULL DEFAULT 0
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS ac_guard_history (
                          id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                          player_uuid CHAR(36) NOT NULL,
                          recorded_at BIGINT NOT NULL,
                          trigger_source VARCHAR(64) NOT NULL,
                          passed TINYINT(1) NOT NULL,
                          detected_mods JSON NOT NULL,
                          client_brand VARCHAR(128) NOT NULL DEFAULT '',
                          ping INT NOT NULL DEFAULT 0,
                          detail TEXT NULL,
                          INDEX idx_guard_history_player_time (player_uuid, recorded_at DESC)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS ac_player_friends (
                          player_uuid CHAR(36) NOT NULL,
                          friend_uuid CHAR(36) NOT NULL,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (player_uuid, friend_uuid),
                          INDEX idx_friends_friend (friend_uuid)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS ac_player_friend_requests (
                          from_uuid CHAR(36) NOT NULL,
                          to_uuid CHAR(36) NOT NULL,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (from_uuid, to_uuid),
                          INDEX idx_friend_req_to (to_uuid)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS ac_referral_codes (
                          player_uuid CHAR(36) NOT NULL PRIMARY KEY,
                          code VARCHAR(16) NOT NULL,
                          created_at BIGINT NOT NULL,
                          blocked TINYINT(1) NOT NULL DEFAULT 0,
                          code_changed_at BIGINT NOT NULL DEFAULT 0,
                          code_change_count INT NOT NULL DEFAULT 0,
                          UNIQUE KEY uk_referral_code (code)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
                try {
                    statement.execute("""
                            ALTER TABLE ac_referral_codes
                            ADD COLUMN code_changed_at BIGINT NOT NULL DEFAULT 0
                            """);
                } catch (SQLException ignored) {
                }
                try {
                    statement.execute("""
                            ALTER TABLE ac_referral_codes
                            ADD COLUMN code_change_count INT NOT NULL DEFAULT 0
                            """);
                } catch (SQLException ignored) {
                }
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS ac_referral_redemptions (
                          id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                          invitee_uuid CHAR(36) NOT NULL,
                          referrer_uuid CHAR(36) NOT NULL,
                          code VARCHAR(16) NOT NULL,
                          status VARCHAR(32) NOT NULL,
                          created_at BIGINT NOT NULL,
                          expires_at BIGINT NOT NULL,
                          qualified_at BIGINT NULL,
                          rewarded_at BIGINT NULL,
                          payout_idempotency_key VARCHAR(64) NULL,
                          active_playtime_ms BIGINT NOT NULL DEFAULT 0,
                          distance_traveled DOUBLE NOT NULL DEFAULT 0,
                          blocks_mined INT NOT NULL DEFAULT 0,
                          blocks_placed INT NOT NULL DEFAULT 0,
                          UNIQUE KEY uk_referral_invitee (invitee_uuid),
                          INDEX idx_referral_referrer_status (referrer_uuid, status),
                          INDEX idx_referral_status_expires (status, expires_at),
                          INDEX idx_referral_payout_pending (status, qualified_at)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS ac_referral_audit (
                          id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                          referral_id BIGINT NULL,
                          player_uuid CHAR(36) NOT NULL,
                          event_type VARCHAR(64) NOT NULL,
                          reason VARCHAR(64) NULL,
                          detail TEXT NULL,
                          created_at BIGINT NOT NULL,
                          INDEX idx_referral_audit_player_time (player_uuid, created_at DESC),
                          INDEX idx_referral_audit_referral (referral_id, created_at DESC)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS ac_referral_blocks (
                          player_uuid CHAR(36) NOT NULL PRIMARY KEY,
                          blocked_at BIGINT NOT NULL,
                          reason VARCHAR(128) NULL
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS ac_player_ip_history (
                          id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                          player_uuid CHAR(36) NOT NULL,
                          ip_address VARCHAR(45) NOT NULL,
                          subnet_key VARCHAR(64) NOT NULL,
                          first_seen BIGINT NOT NULL,
                          last_seen BIGINT NOT NULL,
                          UNIQUE KEY uk_player_ip (player_uuid, ip_address),
                          INDEX idx_ip_subnet (subnet_key, player_uuid),
                          INDEX idx_ip_address (ip_address)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS ac_player_identity (
                          player_uuid CHAR(36) NOT NULL PRIMARY KEY,
                          first_join_at BIGINT NOT NULL,
                          client_brand VARCHAR(128) NOT NULL DEFAULT '',
                          referral_dialog_seen TINYINT(1) NOT NULL DEFAULT 0
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                        """);
                try {
                    statement.execute("""
                            ALTER TABLE ac_player_identity
                            ADD COLUMN referral_dialog_seen TINYINT(1) NOT NULL DEFAULT 0
                            """);
                } catch (SQLException ignored) {
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Falha na migração SQL: " + e.getMessage());
            }
        });
    }

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface AsyncCallback<T> {
        void complete(T value, SQLException error);
    }
}
