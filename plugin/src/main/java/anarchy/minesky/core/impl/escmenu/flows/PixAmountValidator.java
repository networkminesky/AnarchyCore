package anarchy.minesky.core.impl.escmenu.flows;

public final class PixAmountValidator {

    public record Result(boolean valid, double amount, String errorKey) {
        public static Result ok(double amount) {
            return new Result(true, amount, null);
        }

        public static Result fail(String errorKey) {
            return new Result(false, 0, errorKey);
        }
    }

    private PixAmountValidator() {}

    public static Result validate(String raw, double min, double max) {
        if (raw == null || raw.isBlank()) {
            return Result.fail("empty");
        }
        String normalized = raw.trim().replace(" ", "").replace("_", "");
        if (normalized.startsWith("$") || normalized.startsWith("R$")) {
            normalized = normalized.replaceFirst("^R?\\$", "");
        }
        normalized = normalized.replace(".", "").replace(",", ".");
        double value;
        try {
            value = Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return Result.fail("invalid");
        }
        return validate(value, min, max);
    }

    public static Result validate(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return Result.fail("invalid");
        }
        if (value <= 0) {
            return Result.fail("non_positive");
        }
        if (value < min) {
            return Result.fail("below_min");
        }
        if (value > max) {
            return Result.fail("above_max");
        }
        return Result.ok(value);
    }
}