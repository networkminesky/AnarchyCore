package anarchy.minesky.core.listener;

import anarchy.minesky.core.impl.tab.TabSettings;
import org.bukkit.entity.Player;
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
            "?",
            "about",
            "ver",
            "version",
            "help",
            "plugin",
            "icanhasbukkit"
    );

    private final TabSettings tabSettings;

    public PluginCommandBlockListener(TabSettings tabSettings) {
        this.tabSettings = tabSettings;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (bypassesTabFilter(player)) {
            if (!player.hasPermission(ADMIN_PERMISSION) && isBlockedCommand(event.getMessage())) {
                event.setCancelled(true);
            }
            return;
        }
        if (isBlockedCommand(event.getMessage())) {
            event.setCancelled(true);
            return;
        }
        Set<String> allowed = tabSettings.resolveAllowedRoots(player);
        if (allowed != null && !isAllowedRoot(event.getMessage(), allowed)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player player)) {
            return;
        }
        String buffer = event.getBuffer();
        if (buffer == null || !buffer.startsWith("/")) {
            return;
        }
        if (isBlockedCommand(buffer) || isCompletingBlockedRoot(buffer)) {
            event.getCompletions().clear();
            event.setCancelled(true);
            return;
        }
        if (!isRootTabComplete(buffer) || bypassesTabFilter(player)) {
            return;
        }
        Set<String> allowed = tabSettings.resolveAllowedRoots(player);
        if (allowed == null) {
            return;
        }
        event.getCompletions().removeIf(completion -> !allowed.contains(TabSettings.normalizeCommandRoot(completion)));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        boolean bypass = bypassesTabFilter(player);
        Set<String> allowed = bypass ? null : tabSettings.resolveAllowedRoots(player);

        event.getCommands().removeIf(label -> {
            if (isBlockedRootLabel(label)) {
                return true;
            }
            if (allowed == null) {
                return false;
            }
            return !allowed.contains(TabSettings.normalizeCommandRoot(label));
        });
    }

    private boolean bypassesTabFilter(Player player) {
        return tabSettings.resolveAllowedRoots(player) == null;
    }

    private static boolean isRootTabComplete(String buffer) {
        return !buffer.substring(1).contains(" ");
    }

    private static boolean isAllowedRoot(String rawMessage, Set<String> allowed) {
        String message = rawMessage.trim();
        if (!message.startsWith("/")) {
            return false;
        }
        String label = message.substring(1).split("\\s+", 2)[0];
        return allowed.contains(TabSettings.normalizeCommandRoot(label));
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
        return TabSettings.normalizeCommandRoot(label);
    }
}
