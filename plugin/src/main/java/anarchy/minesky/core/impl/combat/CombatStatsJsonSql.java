package anarchy.minesky.core.impl.combat;

/**
 * Expressões MariaDB para ler chaves pontilhadas em {@code ac_player_data.data}
 * (aninhado {@code {"stats":{"kills":1}}} ou plano {@code {"stats.kills":1}}).
 */
public final class CombatStatsJsonSql {

    private CombatStatsJsonSql() {}

  /** Fragmento SQL: valor numérico de uma chave de combate. */
    public static String statValueExpression(String dottedKey) {
        if (dottedKey == null || dottedKey.isBlank()) {
            return "'0'";
        }
        return "COALESCE(JSON_UNQUOTE(JSON_EXTRACT(data, ?)), JSON_UNQUOTE(JSON_EXTRACT(data, ?)))";
    }

    public static String nestedJsonPath(String dottedKey) {
        return "$." + dottedKey;
    }

    public static String flatJsonPath(String dottedKey) {
        return "$.\"" + dottedKey + "\"";
    }
}