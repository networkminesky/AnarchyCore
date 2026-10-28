package anarchy.minesky.core.api.playerdata;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lote de campos para {@code $set} (mescla com o JSON existente).
 * Chaves podem usar ponto ({@code stats.kills}).
 */
public final class PlayerDataUpdates {

    private final Map<String, Object> values;

    private PlayerDataUpdates(Map<String, Object> values) {
        this.values = Map.copyOf(values);
    }

    public static PlayerDataUpdates of(Map<String, Object> values) {
        return new PlayerDataUpdates(values == null ? Map.of() : values);
    }

    public static PlayerDataUpdates single(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        return new PlayerDataUpdates(map);
    }

    public Map<String, Object> values() {
        return values;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
