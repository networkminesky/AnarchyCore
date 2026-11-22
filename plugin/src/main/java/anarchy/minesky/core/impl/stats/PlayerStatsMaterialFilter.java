package anarchy.minesky.core.impl.stats;

/** Lógica pura do filtro de materiais (testável sem servidor). */
public final class PlayerStatsMaterialFilter {

    private PlayerStatsMaterialFilter() {}

    public static boolean shouldSkipLegacyFirst(boolean legacy, boolean ignored) {
        return legacy;
    }

    public static boolean shouldIncludeMine(boolean legacy, boolean block) {
        return !legacy && block;
    }

    public static boolean shouldIncludeUse(boolean legacy, boolean block, boolean item) {
        return !legacy && block && item;
    }
}