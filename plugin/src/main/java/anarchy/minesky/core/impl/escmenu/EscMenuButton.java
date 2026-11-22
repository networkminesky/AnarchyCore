package anarchy.minesky.core.impl.escmenu;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public record EscMenuButton(
        String id,
        Component label,
        @Nullable String sprite,
        EscMenuButtonType type,
        @Nullable String command,
        String handler,
        @Nullable String permission,
        int width) {}