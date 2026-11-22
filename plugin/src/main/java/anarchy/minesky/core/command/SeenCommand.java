package anarchy.minesky.core.command;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.presence.PlayerPresenceRepository;
import anarchy.minesky.core.impl.presence.PlayerPresenceTracker;
import anarchy.minesky.core.impl.presence.SeenOfflineResolver;
import anarchy.minesky.core.impl.presence.SeenVisibility;
import anarchy.minesky.core.utils.PlaytimeFormat;
import anarchy.minesky.core.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.UUID;

public final class SeenCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "anarchycore.seen";
    public static final String VANISH_PERMISSION = "anarchycore.seen.vanish";

    private final Plugin plugin;
    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final PlayerPresenceTracker tracker;
    private final PlayerPresenceRepository presence;

    public SeenCommand(
            Plugin plugin,
            MessagesManager messages,
            FeedbackManager feedback,
            PlayerPresenceTracker tracker,
            PlayerPresenceRepository presence) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
        this.tracker = tracker;
        this.presence = presence;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage(messages.component("commands.player-only"));
            return true;
        }
        if (!viewer.hasPermission(PERMISSION)) {
            deny(viewer);
            return true;
        }
        if (args.length != 1) {
            feedback.send(
                    viewer,
                    messages.component("seen.usage"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return true;
        }

        String queryName = args[0];
        boolean vanishPerm = viewer.hasPermission(VANISH_PERMISSION);
        Player online = CommandPlayers.resolve(queryName);
        if (online != null && SeenVisibility.showAsOnline(online, viewer, vanishPerm)) {
            long started = tracker.sessionStartMillis(online.getUniqueId());
            if (started <= 0) {
                started = System.currentTimeMillis();
            }
            long duration = Math.max(0L, System.currentTimeMillis() - started);
            Component message = messages.component(
                    "seen.online",
                    "target",
                    online.getName(),
                    "duration",
                    PlaytimeFormat.fromDurationMillis(duration));
            feedback.send(viewer, message, FeedbackPriority.NORMAL, true, UiSoundMode.SUCCESS);
            return true;
        }

        OfflinePlayer offline = online != null ? online : CommandPlayers.resolveOffline(queryName);
        if (offline == null || !offline.hasPlayedBefore()) {
            feedback.send(
                    viewer,
                    messages.component("seen.never-joined", "target", queryName),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return true;
        }

        UUID uuid = offline.getUniqueId();
        String displayName = offline.getName() != null ? offline.getName() : queryName;
        presence.findByUuid(uuid).whenComplete((row, error) -> {
            if (error != null) {
                plugin.getLogger().warning("[Seen] Falha ao consultar presença de " + displayName + ": " + error.getMessage());
            }
            viewer.getScheduler().run(plugin, task -> {
                if (!viewer.isOnline()) {
                    return;
                }
                if (error != null) {
                    feedback.send(
                            viewer,
                            messages.component("seen.error"),
                            FeedbackPriority.HIGH,
                            true,
                            UiSoundMode.ERROR);
                    return;
                }
                var record = row.orElse(null);
                OptionalLong since = SeenOfflineResolver.offlineSinceMillis(record, offline);
                if (since.isEmpty()) {
                    feedback.send(
                            viewer,
                            messages.component("seen.never-joined", "target", displayName),
                            FeedbackPriority.HIGH,
                            true,
                            UiSoundMode.ERROR);
                    return;
                }
                long agoMillis = Math.max(0L, System.currentTimeMillis() - since.getAsLong());
                Component message = messages.component(
                        "seen.offline",
                        "target",
                        displayName,
                        "duration",
                        PlaytimeFormat.fromDurationMillis(agoMillis),
                        "when",
                        SeenOfflineResolver.formatTimestamp(since.getAsLong()));
                feedback.send(viewer, message, FeedbackPriority.NORMAL, true, UiSoundMode.SUCCESS);
            }, null);
        }).exceptionally(ex -> {
            plugin.getLogger().warning("[Seen] " + ex.getMessage());
            viewer.getScheduler().run(plugin, task -> feedback.send(
                    viewer,
                    messages.component("seen.error"),
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
        if (args.length != 1 || !(sender instanceof Player viewer)) {
            return List.of();
        }
        if (!viewer.hasPermission(PERMISSION)) {
            return List.of();
        }
        boolean vanishPerm = viewer.hasPermission(VANISH_PERMISSION);
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> vanishPerm || (!Utils.isVanished(player) && viewer.canSee(player)))
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
}