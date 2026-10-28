package anarchy.minesky.core.impl.feedback;

import anarchy.minesky.core.api.feedback.FeedbackAPI;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FeedbackManager implements FeedbackAPI {

    private final Plugin plugin;
    private final Map<UUID, FeedbackPlayerState> states = new ConcurrentHashMap<>();

    private int maxCharsPerPart = 64;
    private long ticksPerPart = 40L;
    private long ticksBetweenMessages = 5L;
    private boolean mirrorToChat = true;

    public FeedbackManager(Plugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void start() {
        Bukkit.getServicesManager().register(FeedbackAPI.class, this, plugin, ServicePriority.Normal);
    }

    public void stop() {
        Bukkit.getServicesManager().unregister(FeedbackAPI.class, this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(Component.empty());
        }
        states.clear();
    }

    public void reloadConfig() {
        FileConfiguration config = plugin.getConfig();
        maxCharsPerPart = Math.max(16, config.getInt("feedback.max-chars-per-part", 64));
        ticksPerPart = Math.max(5L, config.getLong("feedback.ticks-per-part", 40L));
        ticksBetweenMessages = Math.max(0L, config.getLong("feedback.ticks-between-messages", 5L));
        mirrorToChat = config.getBoolean("feedback.mirror-to-chat", true);
    }

    public void handleQuit(UUID playerId) {
        states.remove(playerId);
    }

    @Override
    public void send(Player player, String message) {
        send(player, message, FeedbackPriority.NORMAL);
    }

    @Override
    public void send(Player player, String message, FeedbackPriority priority) {
        send(player, message, priority, mirrorToChat);
    }

    public void send(Player player, String message, FeedbackPriority priority, boolean mirrorToChat) {
        if (player == null || message == null || message.isBlank() || !plugin.isEnabled()) {
            return;
        }
        String normalized = DialogComponentUtils.normalizeLegacyColors(message.trim());
        Component full = DialogComponentUtils.parse(normalized);
        Component chatMirror = mirrorToChat ? full : null;
        enqueue(player, FeedbackTextSplitter.split(normalized, maxCharsPerPart), priority, chatMirror);
    }

    @Override
    public void send(Player player, Component message) {
        send(player, message, FeedbackPriority.NORMAL);
    }

    @Override
    public void send(Player player, Component message, FeedbackPriority priority) {
        send(player, message, priority, mirrorToChat);
    }

    public void send(Player player, Component message, FeedbackPriority priority, boolean mirrorToChat) {
        if (player == null || !player.isOnline() || message == null || !plugin.isEnabled()) {
            return;
        }
        if (priority == null) {
            priority = FeedbackPriority.NORMAL;
        }
        Component chatMirror = mirrorToChat ? message : null;
        enqueue(player, FeedbackTextSplitter.split(message, maxCharsPerPart), priority, chatMirror);
    }

    @Override
    public void clear(Player player) {
        if (player == null) {
            return;
        }
        FeedbackPlayerState state = states.get(player.getUniqueId());
        player.getScheduler().run(plugin, task -> {
            if (state != null) {
                synchronized (state) {
                    state.clearAll();
                }
            }
            if (player.isOnline()) {
                player.sendActionBar(Component.empty());
            }
        }, null);
    }

    @Override
    public boolean isBusy(Player player) {
        if (player == null) {
            return false;
        }
        FeedbackPlayerState state = states.get(player.getUniqueId());
        return state != null && state.isBusy();
    }

    private void enqueue(Player player, List<Component> parts, FeedbackPriority priority, Component chatMirror) {
        FeedbackPlayerState state = states.computeIfAbsent(player.getUniqueId(), id -> new FeedbackPlayerState());
        FeedbackPlayerState.QueuedMessage queued = new FeedbackPlayerState.QueuedMessage(parts, priority, chatMirror);

        player.getScheduler().run(plugin, task -> {
            if (!player.isOnline() || !plugin.isEnabled()) {
                return;
            }
            synchronized (state) {
                state.enqueue(queued, priority);
            }
            pump(player, state);
        }, null);
    }

    private void pump(Player player, FeedbackPlayerState state) {
        if (!player.isOnline() || !plugin.isEnabled()) {
            return;
        }

        int generation;
        synchronized (state) {
            generation = state.generation();

            if (state.hasActiveParts()) {
                Component part = state.currentPart();
                state.advancePart();
                boolean moreParts = state.hasActiveParts();
                if (!moreParts) {
                    state.finishActive();
                }
                player.sendActionBar(part);
                long delay = moreParts ? ticksPerPart : ticksBetweenMessages;
                schedulePump(player, state, generation, delay);
                return;
            }

            FeedbackPlayerState.QueuedMessage next = state.pollNext();
            if (next == null) {
                return;
            }
            if (next.chatMirror() != null) {
                player.sendMessage(next.chatMirror());
            }
            state.beginActive(next.parts());
        }
        pump(player, state);
    }

    private void schedulePump(Player player, FeedbackPlayerState state, int generation, long delayTicks) {
        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline() || !plugin.isEnabled()) {
                return;
            }
            synchronized (state) {
                if (generation != state.generation()) {
                    return;
                }
            }
            pump(player, state);
        }, null, delayTicks);
    }
}
