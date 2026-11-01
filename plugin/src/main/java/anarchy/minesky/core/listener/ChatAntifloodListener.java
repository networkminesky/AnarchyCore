package anarchy.minesky.core.listener;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.api.playerconfig.PlayerConfigAPI;
import anarchy.minesky.core.api.playerconfig.PlayerConfigKeys;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import anarchy.minesky.core.utils.RichMessageParser;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.messaging.PrivateMessageService;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
    public static final String COLOR_PERMISSION = "anarchycore.chat.color";
    public static final String URL_PERMISSION = "anarchycore.chat.url";

    private final Plugin plugin;
    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final PrivateMessageService privateMessages;
    private final PlayerConfigAPI playerConfig;
    private final Map<UUID, Deque<Long>> recentMessages = new ConcurrentHashMap<>();

    private int maxMessages = 2;
    private long windowMillis = 2000L;

    public ChatAntifloodListener(
            Plugin plugin,
            MessagesManager messages,
            FeedbackManager feedback,
            PrivateMessageService privateMessages,
            PlayerConfigAPI playerConfig) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
        this.privateMessages = privateMessages;
        this.playerConfig = playerConfig;
        reloadConfig();
    }

    public void reloadConfig() {
        maxMessages = Math.max(1, plugin.getConfig().getInt("chat.antiflood.max-messages", 2));
        windowMillis = Math.max(500L, plugin.getConfig().getLong("chat.antiflood.window-ms", 2000L));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChatAntiflood(AsyncChatEvent event) {
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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChatFormat(AsyncChatEvent event) {
        Player sender = event.getPlayer();

        if (!playerConfig.getBoolean(PlayerConfigKeys.CHAT_GLOBAL_ENABLED, sender)) {
            event.setCancelled(true);
            feedback.send(
                    sender,
                    messages.component("chat.global-disabled"),
                    FeedbackPriority.NORMAL,
                    false,
                    UiSoundMode.ERROR);
            return;
        }

        String prefix = "";
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            prefix = PlaceholderAPI.setPlaceholders(sender, "%luckperms_prefix%");
        }
        final Component name = DialogComponentUtils.parse(prefix).append(sender.displayName());

        String rawMessage = event.signedMessage().message();
        Component messageBody = RichMessageParser.format(sender, rawMessage);

        event.setCancelled(true);

        Component consoleChat = buildChatLine(sender, name, messageBody, false);
        Bukkit.getConsoleSender().sendMessage(consoleChat);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!playerConfig.getBoolean(PlayerConfigKeys.CHAT_GLOBAL_ENABLED, viewer)) {
                continue;
            }
            boolean showHead = playerConfig.getBoolean(PlayerConfigKeys.CHAT_HEADS_ENABLED, viewer);
            viewer.sendMessage(buildChatLine(sender, name, messageBody, showHead));
        }
    }

    private Component buildChatLine(Player sender, Component name, Component messageBody, boolean showHead) {
        Component open = Component.text("<");
        Component close = Component.text(">").append(Component.space()).append(messageBody);

        if (showHead) {
            Component head = DialogComponentUtils.parse("<head:" + sender.getUniqueId() + ">");
            return head.append(Component.space()).append(open).append(name).append(close);
        }
        return open.append(name).append(close);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        recentMessages.remove(event.getPlayer().getUniqueId());
        privateMessages.handleQuit(event.getPlayer().getUniqueId());
    }
}
