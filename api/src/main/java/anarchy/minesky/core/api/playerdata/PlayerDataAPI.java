package anarchy.minesky.core.api.playerdata;

import anarchy.minesky.core.api.ServiceProviders;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Dados customizados por jogador (JSON em MariaDB, pool Hikari + virtual threads).
 * <p>
 * Callbacks rodam na thread async do pool — use {@code player.getScheduler().run(...)} antes de tocar Bukkit API.
 */
public interface PlayerDataAPI {

    static @Nullable PlayerDataAPI get() {
        return ServiceProviders.get(PlayerDataAPI.class, "anarchy.minesky.core.api.playerdata.PlayerDataAPI");
    }

    boolean isDatabaseActive();

    /**
     * Garante linha do jogador e atualiza {@code latest_nickname} (chamado pelo core no join).
     */
    void touchPlayerAsync(UUID uuid, String nickname);

    void getPlayerDataAsync(UUID uuid, PlayerDataFindCallback callback);

    void getPlayerDataByNameAsync(String nickname, PlayerDataFindCallback callback);

    /**
     * Nick (3–16) ou UUID (36 caracteres), case-insensitive para nick.
     */
    void getPlayerDataAsync(String nameOrUuid, PlayerDataFindCallback callback);

    void getSpecificPlayerDataAsync(
            UUID uuid,
            String key,
            PlayerDataValueType valueType,
            PlayerDataValueCallback callback);

    void getSpecificPlayerDataAsync(
            String nameOrUuid,
            String key,
            PlayerDataValueType valueType,
            PlayerDataValueCallback callback);

    void setPlayerDataAsync(UUID uuid, PlayerDataUpdates updates, PlayerDataSetCallback callback);

    void setPlayerDataAsync(String nameOrUuid, PlayerDataUpdates updates, PlayerDataSetCallback callback);
}
