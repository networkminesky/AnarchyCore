package anarchy.minesky.core.impl.referral;

import java.security.SecureRandom;

public final class ReferralCodeGenerator {

    /** Crockford-like charset sem 0/O, 1/I/L. */
    public static final String DEFAULT_CHARSET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

    private final SecureRandom random = new SecureRandom();
    private final String charset;
    private final int length;

    public ReferralCodeGenerator(String charset, int length) {
        this.charset = sanitize(charset);
        this.length = Math.max(6, length);
    }

    private static String sanitize(String charset) {
        String source = charset == null || charset.isBlank() ? DEFAULT_CHARSET : charset;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < source.length(); i++) {
            char symbol = Character.toUpperCase(source.charAt(i));
            if (symbol == '0' || symbol == 'O' || symbol == '1' || symbol == 'I' || symbol == 'L') {
                continue;
            }
            if (builder.indexOf(String.valueOf(symbol)) >= 0) {
                continue;
            }
            builder.append(symbol);
        }
        return builder.isEmpty() ? DEFAULT_CHARSET : builder.toString();
    }

    public String generate() {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(charset.charAt(random.nextInt(charset.length())));
        }
        return builder.toString();
    }
}