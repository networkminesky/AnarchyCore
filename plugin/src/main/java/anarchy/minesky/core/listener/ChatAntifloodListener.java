package anarchy.minesky.core.listener;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.messaging.PrivateMessageService;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatAntifloodListener implements Listener {

    public static final String BYPASS_PERMISSION = "anarchycore.chat.bypass";

    private final Plugin plugin;
    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final PrivateMessageService privateMessages;
    private final Map<UUID, Deque<Long>> recentMessages = new ConcurrentHashMap<>();

    private int maxMessages = 2;
    private long windowMillis = 2000L;

    public ChatAntifloodListener(
            Plugin plugin,
            MessagesManager messages,
            FeedbackManager feedback,
            PrivateMessageService privateMessages) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
        this.privateMessages = privateMessages;
        reloadConfig();
    }

    public void reloadConfig() {
        maxMessages = Math.max(1, plugin.getConfig().getInt("chat.antiflood.max-messages", 2));
        windowMillis = Math.max(500L, plugin.getConfig().getLong("chat.antiflood.window-ms", 2000L));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat.antiflood.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return;
        }

        long now = System.currentTimeMillis();
        Deque<Long> timestamps = recentMessages.computeIfAbsent(player.getUniqueId(), id -> new ArrayDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowMillis) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxMessages) {
                event.setCancelled(true);
                feedback.send(
                        player,
                        messages.component("chat.antiflood.wait"),
                        FeedbackPriority.HIGH,
                        false,
                        UiSoundMode.ERROR);
                return;
            }
            timestamps.addLast(now);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        recentMessages.remove(event.getPlayer().getUniqueId());
        privateMessages.handleQuit(event.getPlayer().getUniqueId());
    }
}
