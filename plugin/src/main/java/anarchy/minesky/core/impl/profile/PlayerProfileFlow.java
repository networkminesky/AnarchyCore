package anarchy.minesky.core.impl.profile;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.command.CommandPlayers;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerProfileFlow {

    private final Plugin plugin;
    private final PlayerProfileLoader loader;
    private final PlayerProfileGui gui;
    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final Map<UUID, Runnable> viewerBackSelector = new ConcurrentHashMap<>();

    public PlayerProfileFlow(
            Plugin plugin,
            PlayerProfileLoader loader,
            PlayerProfileGui gui,
            MessagesManager messages,
            FeedbackManager feedback) {
        this.plugin = plugin;
        this.loader = loader;
        this.gui = gui;
        this.messages = messages;
        this.feedback = feedback;
    }

    public void openSelf(Player viewer) {
        openSelf(viewer, null);
    }

    public void openSelf(Player viewer, Runnable backToSelector) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }
        stashBack(viewer, backToSelector);
        loader.loadSelf(viewer, snapshot -> deliver(viewer, snapshot, backToSelector));
    }

    public void openOnlineTarget(Player viewer, Player target, Runnable backToSelector) {
        if (viewer == null || target == null) {
            return;
        }
        stashBack(viewer, backToSelector);
        loader.loadForViewer(viewer, target, snapshot -> deliver(viewer, snapshot, backToSelector));
    }

    public void openOfflineName(Player viewer, String nick, Runnable backToSelector) {
        if (viewer == null || nick == null || nick.isBlank()) {
            return;
        }
        OfflinePlayer offline = CommandPlayers.resolveOffline(nick.trim());
        if (offline == null) {
            viewer.getScheduler().run(plugin, task -> {
                feedback.send(
                        viewer,
                        messages.component("commands.player-not-found", "player", nick),
                        FeedbackPriority.HIGH,
                        true,
                        UiSoundMode.ERROR);
                if (backToSelector != null) {
                    backToSelector.run();
                }
            }, null);
            return;
        }
        openOffline(viewer, offline, backToSelector);
    }

    public void openOffline(Player viewer, OfflinePlayer target, Runnable backToSelector) {
        if (viewer == null || target == null) {
            return;
        }
        stashBack(viewer, backToSelector);
        loader.loadOffline(viewer, target, snapshot -> deliver(viewer, snapshot, backToSelector));
    }

    public void openFromLeaderboard(Player viewer, java.util.UUID targetUuid, String targetName, Runnable backToLeaderboard) {
        if (viewer == null || targetUuid == null) {
            return;
        }
        Player online = Bukkit.getPlayer(targetUuid);
        if (online != null && online.isOnline()) {
            openOnlineTarget(viewer, online, backToLeaderboard);
            return;
        }
        stashBack(viewer, backToLeaderboard);
        loader.loadOffline(viewer, targetUuid, targetName, snapshot -> deliver(viewer, snapshot, backToLeaderboard));
    }

    public void openChest(Player viewer, PlayerProfileSnapshot profile) {
        Runnable back = () -> showSummary(viewer, profile, viewerBackSelector.get(viewer.getUniqueId()));
        gui.open(viewer, profile, back);
    }

    public void onChestBack(Player viewer, PlayerProfileSnapshot profile) {
        Runnable back = () -> showSummary(viewer, profile, viewerBackSelector.get(viewer.getUniqueId()));
        gui.handleBackClick(viewer, back);
    }

    private void deliver(Player viewer, PlayerProfileSnapshot snapshot, Runnable backToSelector) {
        if (snapshot == null) {
            feedback.send(
                    viewer,
                    messages.componentOr("stats.never-joined", "&cEste jogador nunca entrou no servidor."),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            if (backToSelector != null) {
                backToSelector.run();
            }
            return;
        }
        showSummary(viewer, snapshot, backToSelector);
    }

    private void showSummary(Player viewer, PlayerProfileSnapshot profile, Runnable backToSelector) {
        Runnable openGui = () -> viewer.getScheduler().run(plugin, task -> openChest(viewer, profile), null);
        PlayerProfileDialog.openSummary(viewer, profile, openGui, backToSelector);
    }

    private void stashBack(Player viewer, Runnable backToSelector) {
        if (backToSelector == null) {
            viewerBackSelector.remove(viewer.getUniqueId());
        } else {
            viewerBackSelector.put(viewer.getUniqueId(), backToSelector);
        }
    }
}