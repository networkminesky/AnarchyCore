package anarchy.minesky.core.impl.escmenu;

public enum EscMenuButtonType {
    COMMAND,
    CUSTOM;

    public static EscMenuButtonType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("type de botão ausente");
        }
        String normalized = raw.trim().toUpperCase();
        return switch (normalized) {
            case "COMMAND", "CMD" -> COMMAND;
            case "CUSTOM", "INTERNAL" -> CUSTOM;
            default -> throw new IllegalArgumentException("type de botão inválido: " + raw);
        };
    }
}