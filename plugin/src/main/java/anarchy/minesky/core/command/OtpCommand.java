package anarchy.minesky.core.command;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.presence.PlayerPresenceRecord;
import anarchy.minesky.core.impl.presence.PlayerPresenceRepository;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class OtpCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "anarchycore.otp";

    private final Plugin plugin;
    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final PlayerPresenceRepository presence;

    public OtpCommand(
            Plugin plugin, MessagesManager messages, FeedbackManager feedback, PlayerPresenceRepository presence) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
        this.presence = presence;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player staff)) {
            sender.sendMessage(messages.component("commands.player-only"));
            return true;
        }
        if (!staff.hasPermission(PERMISSION)) {
            deny(staff);
            return true;
        }
        if (args.length != 1) {
            feedback.send(
                    staff,
                    messages.component("otp.usage"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return true;
        }

        String query = args[0];
        Player online = CommandPlayers.resolve(query);
        if (online != null) {
            online.getScheduler().run(plugin, task -> {
                if (!online.isOnline() || !staff.isOnline()) {
                    return;
                }
                Location target = online.getLocation().clone();
                staff.teleportAsync(target, PlayerTeleportEvent.TeleportCause.COMMAND)
                        .thenAccept(success -> staff.getScheduler().run(plugin, reply -> {
                            if (!staff.isOnline()) {
                                return;
                            }
                            if (!Boolean.TRUE.equals(success)) {
                                feedback.send(
                                        staff,
                                        messages.component("otp.failed"),
                                        FeedbackPriority.HIGH,
                                        true,
                                        UiSoundMode.ERROR);
                                return;
                            }
                            feedback.send(
                                    staff,
                                    messages.component(
                                            "otp.online",
                                            "target",
                                            online.getName(),
                                            "world",
                                            target.getWorld().getName(),
                                            "x",
                                            format(target.getX()),
                                            "y",
                                            format(target.getY()),
                                            "z",
                                            format(target.getZ())),
                                    FeedbackPriority.NORMAL,
                                    true,
                                    UiSoundMode.SUCCESS);
                        }, null));
            }, null);
            return true;
        }

        var offline = CommandPlayers.resolveOffline(query);
        if (offline == null || !offline.hasPlayedBefore()) {
            feedback.send(
                    staff,
                    messages.component("commands.player-not-found", "player", query),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return true;
        }

        UUID uuid = offline.getUniqueId();
        String displayName = offline.getName() != null ? offline.getName() : query;
        presence.findByUuid(uuid).whenComplete((row, error) -> {
            if (error != null) {
                plugin.getLogger().warning("[OTP] Falha ao consultar " + displayName + ": " + error.getMessage());
            }
            staff.getScheduler().run(plugin, task -> {
                if (!staff.isOnline()) {
                    return;
                }
                if (error != null) {
                    feedback.send(staff, messages.component("otp.failed"), FeedbackPriority.HIGH, true, UiSoundMode.ERROR);
                    return;
                }
                PlayerPresenceRecord record = row.orElse(null);
                if (record == null || !record.hasLogoutLocation()) {
                    feedback.send(
                            staff,
                            messages.component("otp.no-location", "target", displayName),
                            FeedbackPriority.HIGH,
                            true,
                            UiSoundMode.ERROR);
                    return;
                }
                if (Bukkit.getWorld(record.world()) == null) {
                    feedback.send(
                            staff,
                            messages.component("otp.world-missing", "world", record.world()),
                            FeedbackPriority.HIGH,
                            true,
                            UiSoundMode.ERROR);
                    return;
                }
                Location destination = record.toLogoutLocation();
                if (destination == null) {
                    feedback.send(staff, messages.component("otp.failed"), FeedbackPriority.HIGH, true, UiSoundMode.ERROR);
                    return;
                }
                staff.teleportAsync(destination, PlayerTeleportEvent.TeleportCause.COMMAND)
                        .thenAccept(success -> staff.getScheduler().run(plugin, reply -> {
                            if (!staff.isOnline()) {
                                return;
                            }
                            if (!Boolean.TRUE.equals(success)) {
                                feedback.send(
                                        staff,
                                        messages.component("otp.failed"),
                                        FeedbackPriority.HIGH,
                                        true,
                                        UiSoundMode.ERROR);
                                return;
                            }
                            feedback.send(
                                    staff,
                                    messages.component(
                                            "otp.offline",
                                            "target",
                                            displayName,
                                            "world",
                                            record.world(),
                                            "x",
                                            format(record.x()),
                                            "y",
                                            format(record.y()),
                                            "z",
                                            format(record.z())),
                                    FeedbackPriority.NORMAL,
                                    true,
                                    UiSoundMode.SUCCESS);
                        }, null));
            }, null);
        }).exceptionally(ex -> {
            plugin.getLogger().warning("[OTP] " + ex.getMessage());
            staff.getScheduler().run(plugin, task -> feedback.send(
                    staff,
                    messages.component("otp.failed"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR), null);
            return null;
        });
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length != 1 || !(sender instanceof Player staff)) {
            return List.of();
        }
        if (!staff.hasPermission(PERMISSION)) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
    }

    private void deny(Player player) {
        feedback.send(
                player,
                messages.component("commands.no-permission"),
                FeedbackPriority.HIGH,
                true,
                UiSoundMode.ERROR);
    }

    private static String format(double value) {
        if (Math.rint(value) == value && Math.abs(value) < 1.0E7) {
            return Long.toString((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}