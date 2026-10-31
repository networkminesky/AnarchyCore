package anarchy.minesky.core.command;

import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.guard.GuardHistoryEntry;
import anarchy.minesky.core.impl.guard.GuardManager;
import anarchy.minesky.core.impl.guard.GuardPlayerSnapshot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class GuardCommandHandler {

    public static final String PERMISSION = "anarchycore.guard.admin";

    private final GuardManager guardManager;
    private final MessagesManager messages;
    private final FeedbackManager feedback;

    public GuardCommandHandler(GuardManager guardManager, MessagesManager messages, FeedbackManager feedback) {
        this.guardManager = guardManager;
        this.messages = messages;
        this.feedback = feedback;
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            send(sender, messages.component("commands.no-permission"));
            return true;
        }
        if (args.length < 2) {
            send(sender, messages.component("guard.usage"));
            return true;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "check" -> handleCheck(sender, args);
            case "info" -> handleInfo(sender, args);
            case "history" -> handleHistory(sender, args);
            default -> {
                send(sender, messages.component("guard.usage"));
                yield true;
            }
        };
    }

    public List<String> tabComplete(String[] args) {
        if (args.length == 2) {
            return filterPrefix(args[1], List.of("check", "info", "history"));
        }
        if (args.length == 3 && ("check".equalsIgnoreCase(args[1]) || "info".equalsIgnoreCase(args[1]) || "history".equalsIgnoreCase(args[1]))) {
            return PlayerNameTabComplete.onlineNames(args, 2);
        }
        return List.of();
    }

    private boolean handleCheck(CommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, messages.component("guard.check-usage"));
            return true;
        }
        Player target = CommandPlayers.resolve(args[2]);
        if (target == null) {
            send(sender, messages.component("commands.player-not-found", "player", args[2]));
            return true;
        }
        if (guardManager.isChecking(target.getUniqueId())) {
            send(sender, messages.component("guard.already-checking", "player", target.getName()));
            return true;
        }
        if (guardManager.hasGuardBypass(target)) {
            send(sender, messages.component("guard.check-bypass", "player", target.getName()));
            return true;
        }
        boolean started = guardManager.startCheck(target, "manual:" + sender.getName());
        if (!started) {
            send(sender, messages.component("guard.skipped", "player", target.getName()));
            return true;
        }
        send(sender, messages.component("guard.check-started", "player", target.getName()));
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, messages.component("guard.info-usage"));
            return true;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(args[2]);
        UUID uuid = offline.getUniqueId();
        GuardPlayerSnapshot snapshots = guardManager.snapshots();
        Player online = offline.getPlayer();

        if (online != null) {
            snapshots.refreshFromPlayer(online);
        }

        int strikes = guardManager.repository().getStrikes(uuid);
        boolean checking = online != null && guardManager.isChecking(uuid);

        sender.sendMessage(messages.component("guard.info-header", "player", offline.getName() != null ? offline.getName() : args[2]));
        sender.sendMessage(line("UUID", uuid.toString()));
        sender.sendMessage(line("Brand", snapshots.clientBrand(uuid)));
        sender.sendMessage(line("Strikes", Integer.toString(strikes)));
        sender.sendMessage(line("Verificando agora", checking ? "sim" : "não"));
        sender.sendMessage(line("Última verificação", guardManager.formatTimestamp(snapshots.lastCheckAt(uuid))));
        sender.sendMessage(line("Resultado", snapshots.lastCheckResult(uuid)));
        if (online != null) {
            sender.sendMessage(line("Ping", online.getPing() + "ms"));
            sender.sendMessage(line("Online", "sim"));
        } else {
            sender.sendMessage(line("Online", "não"));
        }
        return true;
    }

    private boolean handleHistory(CommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, messages.component("guard.history-usage"));
            return true;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(args[2]);
        UUID uuid = offline.getUniqueId();
        List<GuardHistoryEntry> history = guardManager.repository().getHistory(uuid);
        String name = offline.getName() != null ? offline.getName() : args[2];

        sender.sendMessage(messages.component("guard.history-header", "player", name, "count", Integer.toString(history.size())));

        if (history.isEmpty()) {
            send(sender, messages.component("guard.history-empty"));
            return true;
        }

        int from = Math.max(0, history.size() - 15);
        for (int i = history.size() - 1; i >= from; i--) {
            GuardHistoryEntry entry = history.get(i);
            String mods = entry.detectedMods().isEmpty() ? "—" : String.join(", ", entry.detectedMods());
            sender.sendMessage(Component.text(
                            guardManager.formatTimestamp(entry.timestampMillis())
                                    + " | "
                                    + entry.trigger()
                                    + " | "
                                    + (entry.passed() ? "OK" : "DETECTADO")
                                    + " | "
                                    + mods
                                    + " | brand="
                                    + entry.clientBrand()
                                    + " | ping="
                                    + entry.ping()
                                    + "ms",
                    entry.passed() ? NamedTextColor.GREEN : NamedTextColor.RED));
            if (entry.detail() != null && !entry.detail().isBlank()) {
                sender.sendMessage(Component.text("  ↳ " + entry.detail(), NamedTextColor.GRAY));
            }
        }
        return true;
    }

    private static Component line(String label, String value) {
        return Component.text(label + ": ", NamedTextColor.GRAY).append(Component.text(value, NamedTextColor.WHITE));
    }

    private static List<String> filterPrefix(String prefix, List<String> values) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.startsWith(lower)).toList();
    }

    private void send(CommandSender sender, Component message) {
        if (sender instanceof Player player) {
            feedback.send(player, message);
        } else {
            sender.sendMessage(message);
        }
    }
}
