package anarchy.minesky.core.impl.referral;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Validação pura de código personalizado: formato, palavras reservadas e limite de troca.
 * A unicidade fica no banco ({@code UNIQUE} + comparação case-insensitive).
 */
public final class ReferralCustomCodeRules {

    public static final String DEFAULT_PATTERN = "^[A-Z0-9_-]{3,16}$";

    private static final Set<String> BUILTIN_RESERVED = Set.of(
            "USAR", "USE", "RESGATAR", "STATUS", "INFO", "DEFINIR", "CUSTOM", "SET",
            "CODIGO", "CODE", "ADMIN");

    public enum Problem {
        INVALID,
        RESERVED,
        BLACKLISTED
    }

    public enum ChangeProblem {
        COOLDOWN,
        LIMIT
    }

    private final Pattern pattern;
    private final int minLength;
    private final int maxLength;
    private final long cooldownMs;
    private final int maxChanges;
    private final Set<String> reserved;
    private final Set<String> blacklist;

    public ReferralCustomCodeRules(
            String pattern,
            int minLength,
            int maxLength,
            long cooldownMs,
            int maxChanges,
            Collection<String> reserved,
            Collection<String> blacklist) {
        int max = Math.min(16, Math.max(1, maxLength));
        int min = Math.min(max, Math.max(1, minLength));
        this.minLength = min;
        this.maxLength = max;
        this.cooldownMs = Math.max(0L, cooldownMs);
        this.maxChanges = Math.max(0, maxChanges);
        this.pattern = compile(pattern);
        this.reserved = new HashSet<>(BUILTIN_RESERVED);
        addTokens(this.reserved, reserved);
        this.blacklist = new HashSet<>();
        addTokens(this.blacklist, blacklist);
    }

    private static Pattern compile(String pattern) {
        String source = pattern == null || pattern.isBlank() ? DEFAULT_PATTERN : pattern;
        try {
            return Pattern.compile(source);
        } catch (PatternSyntaxException ignored) {
            return Pattern.compile(DEFAULT_PATTERN);
        }
    }

    private static void addTokens(Set<String> target, Collection<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            target.add(value.trim().toUpperCase(Locale.ROOT));
        }
    }

    public int minLength() {
        return minLength;
    }

    public int maxLength() {
        return maxLength;
    }

    public long cooldownMs() {
        return cooldownMs;
    }

    public int maxChanges() {
        return maxChanges;
    }

    /** Trim + maiúsculas. Espaço, controle, {@code &} e tags são rejeitados. */
    public String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String code = raw.trim().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            return null;
        }
        for (int i = 0; i < code.length(); i++) {
            char symbol = code.charAt(i);
            if (Character.isWhitespace(symbol) || Character.isISOControl(symbol)
                    || symbol == '<' || symbol == '>' || symbol == '&') {
                return null;
            }
        }
        return code;
    }

    public Problem validate(String normalized) {
        if (normalized == null
                || normalized.length() < minLength
                || normalized.length() > maxLength
                || !pattern.matcher(normalized).matches()) {
            return Problem.INVALID;
        }
        if (reserved.contains(normalized)) {
            return Problem.RESERVED;
        }
        if (blacklisted(normalized)) {
            return Problem.BLACKLISTED;
        }
        return null;
    }

    /**
     * {@code changeCount == 0} é a primeira definição e não entra em cooldown.
     * {@code maxChanges == 0} desliga o limite.
     */
    public ChangeProblem checkChange(int changeCount, long changedAt, long now) {
        if (maxChanges > 0 && changeCount >= maxChanges) {
            return ChangeProblem.LIMIT;
        }
        if (changeCount > 0 && cooldownMs > 0 && changedAt > 0 && now - changedAt < cooldownMs) {
            return ChangeProblem.COOLDOWN;
        }
        return null;
    }

    private boolean blacklisted(String code) {
        for (String entry : blacklist) {
            if (entry.equals(code)) {
                return true;
            }
            if (entry.length() >= 4 && code.contains(entry)) {
                return true;
            }
        }
        return false;
    }
}
