package anarchy.minesky.core.impl.playerdata;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

final class PlayerDataJson {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    private PlayerDataJson() {
    }

    static Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> map = GSON.fromJson(json, MAP_TYPE);
        return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
    }

    static String toJson(Map<String, Object> map) {
        return GSON.toJson(map == null ? Map.of() : map);
    }

    static Object readNested(Map<String, Object> root, String dottedKey) {
        if (root == null || dottedKey == null || dottedKey.isBlank()) {
            return null;
        }
        String[] parts = dottedKey.split("\\.");
        Object value = root;
        for (String part : parts) {
            if (value instanceof Map<?, ?> map) {
                value = map.get(part);
            } else {
                return null;
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    static void putNested(Map<String, Object> root, String dottedKey, Object value) {
        String[] parts = dottedKey.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            current = (Map<String, Object>) current.computeIfAbsent(parts[i], k -> new LinkedHashMap<>());
        }
        current.put(parts[parts.length - 1], value);
    }
}
