package anarchy.minesky.core.api.playerdata;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Documento de dados customizados de um jogador (JSON persistido).
 */
public final class PlayerDataSnapshot {

    private final UUID uuid;
    private final @Nullable String latestNickname;
    private final Map<String, Object> data;

    public PlayerDataSnapshot(UUID uuid, @Nullable String latestNickname, Map<String, Object> data) {
        this.uuid = uuid;
        this.latestNickname = latestNickname;
        this.data = data == null ? Map.of() : Map.copyOf(data);
    }

    public UUID uuid() {
        return uuid;
    }

    public @Nullable String latestNickname() {
        return latestNickname;
    }

    public Map<String, Object> data() {
        return data;
    }

    public Map<String, Object> asMap() {
        return data;
    }

    public static PlayerDataSnapshot empty(UUID uuid) {
        return new PlayerDataSnapshot(uuid, null, Collections.emptyMap());
    }
}
