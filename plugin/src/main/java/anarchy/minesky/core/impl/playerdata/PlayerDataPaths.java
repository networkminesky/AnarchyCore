package anarchy.minesky.core.impl.playerdata;

import java.util.Map;

/** Leitura de chaves pontilhadas no JSON de {@code ac_player_data}. */
public final class PlayerDataPaths {

    private PlayerDataPaths() {}

    public static Map<String, Object> parseMap(String json) {
        return PlayerDataJson.parseMap(json);
    }

    public static Object readNested(Map<String, Object> root, String dottedKey) {
        return PlayerDataJson.readNested(root, dottedKey);
    }

    /** Chave aninhada ({@code stats.kills}) ou plana no JSON ({@code "stats.kills"}). */
    public static Object readDotted(Map<String, Object> root, String dottedKey) {
        if (root == null || dottedKey == null || dottedKey.isBlank()) {
            return null;
        }
        if (root.containsKey(dottedKey)) {
            return root.get(dottedKey);
        }
        return readNested(root, dottedKey);
    }
}