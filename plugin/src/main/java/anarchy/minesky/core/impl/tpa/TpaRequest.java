package anarchy.minesky.core.impl.tpa;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record TpaRequest(
        @NotNull UUID requesterId,
        @NotNull UUID targetId,
        @NotNull TpaRequestType type,
        long createdAtMillis) {
}
