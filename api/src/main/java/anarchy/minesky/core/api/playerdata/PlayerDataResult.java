package anarchy.minesky.core.api.playerdata;

import org.jetbrains.annotations.Nullable;

/**
 * Resultado discriminated: sucesso ou erro (callbacks sempre async).
 */
public sealed interface PlayerDataResult<T> permits PlayerDataResult.Success, PlayerDataResult.Failure {

    static <T> Success<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Failure<T> failure(PlayerDataError error) {
        return new Failure<>(error);
    }

    default boolean isSuccess() {
        return this instanceof Success;
    }

    default @Nullable T valueOrNull() {
        return this instanceof Success<T> s ? s.value() : null;
    }

    default @Nullable PlayerDataError errorOrNull() {
        return this instanceof Failure<T> f ? f.error() : null;
    }

    record Success<T>(T value) implements PlayerDataResult<T> {
    }

    record Failure<T>(PlayerDataError error) implements PlayerDataResult<T> {
    }
}
