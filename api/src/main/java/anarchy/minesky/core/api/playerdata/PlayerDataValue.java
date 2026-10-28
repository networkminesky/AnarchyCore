package anarchy.minesky.core.api.playerdata;

/**
 * Valor lido de uma chave.
 *
 * @param value valor convertido ({@link PlayerDataValueType#coerce(Object)})
 * @param present {@code true} se a chave existia no documento
 */
public record PlayerDataValue(Object value, boolean present) {
}
