package anarchy.minesky.core.impl.escmenu;

import net.kyori.adventure.key.Key;

public final class EscMenuKeys {

    public static final String NAMESPACE = "anarchycore";
    public static final String CUSTOM_PREFIX = "esc_";

    private EscMenuKeys() {}

    public static Key customClick(String buttonId) {
        return Key.key(NAMESPACE, CUSTOM_PREFIX + normalizeId(buttonId));
    }

    public static boolean isEscCustomClick(Key key) {
        return key != null
                && NAMESPACE.equals(key.namespace())
                && key.value().startsWith(CUSTOM_PREFIX);
    }

    public static String buttonIdFromKey(Key key) {
        if (!isEscCustomClick(key)) {
            return null;
        }
        return key.value().substring(CUSTOM_PREFIX.length());
    }

    static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id de botão ausente");
        }
        String normalized = id.trim().toLowerCase();
        if (!normalized.matches("[a-z0-9_\\-]+")) {
            throw new IllegalArgumentException("id de botão inválido: " + id);
        }
        return normalized;
    }
}