package anarchy.minesky.core.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.Locale;
import java.util.Set;

public final class PluginCommandBlockListener implements Listener {

    public static final String ADMIN_PERMISSION = "anarchycore.admin";

    private static final Set<String> BLOCKED_ROOTS = Set.of(
            "plugins",
            "pl",
            "about",
            "ver",
            "version",
            "help",
            "plugin",
            "icanhasbukkit"
    );

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.getPlayer().hasPermission(ADMIN_PERMISSION)) {
            return;
        }
        if (isBlockedCommand(event.getMessage())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof org.bukkit.entity.Player player)) {
            return;
        }
        if (player.hasPermission(ADMIN_PERMISSION)) {
            return;
        }
        String buffer = event.getBuffer();
        if (buffer == null || !buffer.startsWith("/")) {
            return;
        }
        if (isBlockedCommand(buffer) || isCompletingBlockedRoot(buffer)) {
            event.getCompletions().clear();
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandSend(PlayerCommandSendEvent event) {
        if (event.getPlayer().hasPermission(ADMIN_PERMISSION)) {
            return;
        }
        event.getCommands().removeIf(PluginCommandBlockListener::isBlockedRootLabel);
    }

    private static boolean isBlockedCommand(String rawMessage) {
        String message = rawMessage.trim();
        if (!message.startsWith("/")) {
            return false;
        }
        String label = message.substring(1).split("\\s+", 2)[0];
        return isBlockedRootLabel(label);
    }

    private static boolean isCompletingBlockedRoot(String buffer) {
        String withoutSlash = buffer.substring(1);
        if (withoutSlash.contains(" ")) {
            return false;
        }
        String partial = commandRoot(withoutSlash).toLowerCase(Locale.ROOT);
        for (String blocked : BLOCKED_ROOTS) {
            if (blocked.startsWith(partial)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlockedRootLabel(String label) {
        return BLOCKED_ROOTS.contains(commandRoot(label).toLowerCase(Locale.ROOT));
    }

    private static String commandRoot(String label) {
        int colon = label.indexOf(':');
        if (colon >= 0 && colon < label.length() - 1) {
            return label.substring(colon + 1);
        }
        return label;
    }
}
