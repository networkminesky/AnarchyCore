package anarchy.minesky.core.impl.blocks;

import java.util.Locale;

public enum BlockType {
    POSITION,
    CLICK;

    public static BlockType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}