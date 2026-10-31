package anarchy.minesky.core.impl.guard;

import java.util.List;

public record GuardPunishmentStep(Type type, List<String> consoleCommands) {

    public enum Type {
        KICK,
        CONSOLE
    }

    public static GuardPunishmentStep kick() {
        return new GuardPunishmentStep(Type.KICK, List.of());
    }

    public static GuardPunishmentStep console(List<String> commands) {
        return new GuardPunishmentStep(Type.CONSOLE, List.copyOf(commands));
    }
}
