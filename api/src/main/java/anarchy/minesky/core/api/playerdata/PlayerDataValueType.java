package anarchy.minesky.core.api.playerdata;

import java.util.Collections;
import java.util.List;

/**
 * Tipo esperado ao ler uma chave (influencia default quando ausente e conversão).
 */
public enum PlayerDataValueType {
    INTEGER,
    LONG,
    DOUBLE,
    BOOLEAN,
    STRING,
    LIST,
    OBJECT;

    public Object defaultValue() {
        return switch (this) {
            case INTEGER -> 0;
            case LONG -> 0L;
            case DOUBLE -> 0.0d;
            case BOOLEAN -> false;
            case STRING, OBJECT -> null;
            case LIST -> Collections.emptyList();
        };
    }

    @SuppressWarnings("unchecked")
    public Object coerce(Object raw) {
        if (raw == null) {
            return defaultValue();
        }
        return switch (this) {
            case INTEGER -> raw instanceof Number n ? n.intValue() : Integer.parseInt(raw.toString());
            case LONG -> raw instanceof Number n ? n.longValue() : Long.parseLong(raw.toString());
            case DOUBLE -> raw instanceof Number n ? n.doubleValue() : Double.parseDouble(raw.toString());
            case BOOLEAN -> raw instanceof Boolean b ? b : Boolean.parseBoolean(raw.toString());
            case STRING -> raw.toString();
            case LIST -> raw instanceof List<?> list ? list : List.of(raw);
            case OBJECT -> raw;
        };
    }
}
