package anarchy.minesky.core.impl.playerdata;

import anarchy.minesky.core.api.playerdata.PlayerDataAPI;
import anarchy.minesky.core.api.playerdata.PlayerDataError;
import anarchy.minesky.core.api.playerdata.PlayerDataFindCallback;
import anarchy.minesky.core.api.playerdata.PlayerDataResult;
import anarchy.minesky.core.api.playerdata.PlayerDataSetCallback;
import anarchy.minesky.core.api.playerdata.PlayerDataSnapshot;
import anarchy.minesky.core.api.playerdata.PlayerDataUpdates;
import anarchy.minesky.core.api.playerdata.PlayerDataValue;
import anarchy.minesky.core.api.playerdata.PlayerDataValueCallback;
import anarchy.minesky.core.api.playerdata.PlayerDataValueType;
import anarchy.minesky.core.impl.database.DatabaseManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.sql.SQLException;
import java.util.UUID;

public final class PlayerDataAPIImpl implements PlayerDataAPI {

    private final Plugin plugin;
    private final PlayerDataStore store;

    public PlayerDataAPIImpl(Plugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.store = new PlayerDataStore(plugin, database);
    }

    public void register() {
        plugin.getServer().getServicesManager().register(PlayerDataAPI.class, this, plugin, ServicePriority.Normal);
    }

    public void unregister() {
        plugin.getServer().getServicesManager().unregister(PlayerDataAPI.class, this);
    }

    @Override
    public boolean isDatabaseActive() {
        return store.isActive();
    }

    @Override
    public void touchPlayerAsync(UUID uuid, String nickname) {
        store.touch(uuid, nickname, (ignored, error) -> {
            if (error != null) {
                plugin.getLogger().warning("touchPlayerAsync falhou: " + error.getMessage());
            }
        });
    }

    @Override
    public void getPlayerDataAsync(UUID uuid, PlayerDataFindCallback callback) {
        if (!store.isActive()) {
            callback.onComplete(PlayerDataResult.failure(PlayerDataError.DATABASE_DISABLED));
            return;
        }
        store.findByUuid(uuid, (row, error) -> dispatchFind(callback, row, error));
    }

    @Override
    public void getPlayerDataByNameAsync(String nickname, PlayerDataFindCallback callback) {
        if (!store.isActive()) {
            callback.onComplete(PlayerDataResult.failure(PlayerDataError.DATABASE_DISABLED));
            return;
        }
        if (!PlayerDataStore.isValidNick(nickname)) {
            callback.onComplete(PlayerDataResult.failure(PlayerDataError.INVALID_IDENTIFIER));
            return;
        }
        store.findByNickname(nickname, (row, error) -> dispatchFind(callback, row, error));
    }

    @Override
    public void getPlayerDataAsync(String nameOrUuid, PlayerDataFindCallback callback) {
        UUID uuid = PlayerDataStore.resolveUuid(nameOrUuid);
        if (uuid != null) {
            getPlayerDataAsync(uuid, callback);
            return;
        }
        if (PlayerDataStore.isValidNick(nameOrUuid)) {
            getPlayerDataByNameAsync(nameOrUuid, callback);
            return;
        }
        callback.onComplete(PlayerDataResult.failure(PlayerDataError.INVALID_IDENTIFIER));
    }

    @Override
    public void getSpecificPlayerDataAsync(
            UUID uuid,
            String key,
            PlayerDataValueType valueType,
            PlayerDataValueCallback callback) {
        if (key == null || key.isBlank()) {
            callback.onComplete(PlayerDataResult.failure(PlayerDataError.INVALID_KEY));
            return;
        }
        getPlayerDataAsync(uuid, result -> {
            if (!(result instanceof PlayerDataResult.Success<PlayerDataSnapshot> success)) {
                callback.onComplete(PlayerDataResult.failure(
                        result.errorOrNull() != null ? result.errorOrNull() : PlayerDataError.DATABASE_ERROR));
                return;
            }
            PlayerDataSnapshot snapshot = success.value();
            Object raw = PlayerDataJson.readNested(snapshot.data(), key);
            if (raw == null) {
                callback.onComplete(PlayerDataResult.success(
                        new PlayerDataValue(valueType.defaultValue(), false)));
                return;
            }
            try {
                callback.onComplete(PlayerDataResult.success(
                        new PlayerDataValue(valueType.coerce(raw), true)));
            } catch (Exception e) {
                callback.onComplete(PlayerDataResult.failure(PlayerDataError.DATABASE_ERROR));
            }
        });
    }

    @Override
    public void getSpecificPlayerDataAsync(
            String nameOrUuid,
            String key,
            PlayerDataValueType valueType,
            PlayerDataValueCallback callback) {
        UUID uuid = PlayerDataStore.resolveUuid(nameOrUuid);
        if (uuid != null) {
            getSpecificPlayerDataAsync(uuid, key, valueType, callback);
            return;
        }
        if (PlayerDataStore.isValidNick(nameOrUuid)) {
            getPlayerDataByNameAsync(nameOrUuid, findResult -> {
                if (!(findResult instanceof PlayerDataResult.Success<PlayerDataSnapshot> success)) {
                    callback.onComplete(PlayerDataResult.failure(
                            findResult.errorOrNull() != null ? findResult.errorOrNull() : PlayerDataError.DATABASE_ERROR));
                    return;
                }
                getSpecificPlayerDataAsync(success.value().uuid(), key, valueType, callback);
            });
            return;
        }
        callback.onComplete(PlayerDataResult.failure(PlayerDataError.INVALID_IDENTIFIER));
    }

    @Override
    public void setPlayerDataAsync(UUID uuid, PlayerDataUpdates updates, PlayerDataSetCallback callback) {
        if (!store.isActive()) {
            callback.onComplete(PlayerDataResult.failure(PlayerDataError.DATABASE_DISABLED));
            return;
        }
        if (updates == null || updates.isEmpty()) {
            callback.onComplete(PlayerDataResult.success(null));
            return;
        }
        store.saveMerged(uuid, updates.values(), (knownBefore, error) -> {
            if (error != null) {
                callback.onComplete(PlayerDataResult.failure(PlayerDataError.DATABASE_ERROR));
                return;
            }
            callback.onComplete(PlayerDataResult.success(null));
        });
    }

    @Override
    public void setPlayerDataAsync(String nameOrUuid, PlayerDataUpdates updates, PlayerDataSetCallback callback) {
        UUID uuid = PlayerDataStore.resolveUuid(nameOrUuid);
        if (uuid != null) {
            setPlayerDataAsync(uuid, updates, callback);
            return;
        }
        if (PlayerDataStore.isValidNick(nameOrUuid)) {
            getPlayerDataByNameAsync(nameOrUuid, findResult -> {
                if (!(findResult instanceof PlayerDataResult.Success<PlayerDataSnapshot> success)) {
                    callback.onComplete(PlayerDataResult.failure(
                            findResult.errorOrNull() != null ? findResult.errorOrNull() : PlayerDataError.UNKNOWN_PLAYER));
                    return;
                }
                setPlayerDataAsync(success.value().uuid(), updates, callback);
            });
            return;
        }
        callback.onComplete(PlayerDataResult.failure(PlayerDataError.INVALID_IDENTIFIER));
    }

    private void dispatchFind(PlayerDataFindCallback callback, PlayerDataStore.Row row, SQLException error) {
        if (error != null) {
            callback.onComplete(PlayerDataResult.failure(PlayerDataError.DATABASE_ERROR));
            return;
        }
        if (row == null) {
            callback.onComplete(PlayerDataResult.failure(PlayerDataError.UNKNOWN_PLAYER));
            return;
        }
        callback.onComplete(PlayerDataResult.success(
                new PlayerDataSnapshot(row.uuid(), row.latestNickname(), row.data())));
    }
}
