package anarchy.minesky.core.api.playerdata;

/**
 * Erros retornados pelos callbacks da {@link PlayerDataAPI}.
 */
public enum PlayerDataError {
    DATABASE_DISABLED,
    DATABASE_ERROR,
    UNKNOWN_PLAYER,
    INVALID_IDENTIFIER,
    INVALID_KEY
}
