package anarchy.minesky.core.impl.stats;

public final class WeeklyStatMath {

    private WeeklyStatMath() {}

    /** Contadores monotônicos (abates, tempo, blocos): ganho desde o baseline da semana. */
    public static long monotonicWeekly(long current, long baseline) {
        long delta = current - baseline;
        return Math.max(0L, delta);
    }

    /** Saldo que pode subir/descer: nunca negativo no ranking semanal. */
    public static double economyWeekly(double current, double baseline) {
        double delta = current - baseline;
        return Math.max(0.0, delta);
    }

    /** Mantém o maior ganho semanal já observado (picos entre syncs). */
    public static double mergeEconomyScore(double previousScore, double freshDelta) {
        return Math.max(previousScore, freshDelta);
    }
}