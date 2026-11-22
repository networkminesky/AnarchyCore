package anarchy.minesky.core.impl.escmenu;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record EscMenuDefinition(
        Component pauseLabel,
        Component hubTitle,
        int columns,
        int buttonWidth,
        boolean exitEnabled,
        @Nullable Component exitLabel,
        int exitWidth,
        boolean commandEnabled,
        String commandName,
        List<String> commandAliases,
        List<EscMenuButton> buttons,
        EscMenuFlows flows) {

    public EscMenuButton findButton(String id) {
        if (id == null) {
            return null;
        }
        for (EscMenuButton button : buttons) {
            if (button.id().equals(id)) {
                return button;
            }
        }
        return null;
    }
}