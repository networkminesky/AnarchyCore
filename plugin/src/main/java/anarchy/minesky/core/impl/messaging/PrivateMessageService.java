package anarchy.minesky.core.impl.messaging;

import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.dialogapi.inputs.TextInput;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.playerprefs.ConfigManager;
import anarchy.minesky.core.impl.playerprefs.PlayerPreferencesService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PrivateMessageService {

    private static final String INPUT_MESSAGE = "message";

    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final PlayerPreferencesService playerPrefs;
    private final ConfigManager configManager;
    private final Map<UUID, UUID> lastPartner = new ConcurrentHashMap<>();

    public PrivateMessageService(
            MessagesManager messages,
            FeedbackManager feedback,
            PlayerPreferencesService playerPrefs,
            ConfigManager configManager) {
        this.messages = messages;
        this.feedback = feedback;
        this.playerPrefs = playerPrefs;
        this.configManager = configManager;
    }

    public void handleQuit(UUID playerId) {
        lastPartner.remove(playerId);
        lastPartner.entrySet().removeIf(entry -> entry.getValue().equals(playerId));
    }

    public void sendTell(Player sender, String targetName, String[] messageArgs) {
        if (messageArgs.length == 0) {
            openTellDialog(sender, targetName);
            return;
        }
        Player target = resolveOnlinePlayer(targetName);
        if (target == null) {
            feedbackPlayer(sender, messages.component("commands.player-not-found", "player", targetName));
            return;
        }
        sendTell(sender, target, String.join(" ", messageArgs));
    }

    public void sendReply(Player sender, String[] messageArgs) {
        UUID partnerId = lastPartner.get(sender.getUniqueId());
        if (partnerId == null) {
            feedbackPlayer(sender, messages.component("commands.reply-no-target"));
            return;
        }
        Player target = Bukkit.getPlayer(partnerId);
        if (target == null || !target.isOnline()) {
            lastPartner.remove(sender.getUniqueId());
            feedbackPlayer(sender, messages.component("commands.player-not-found", "player", "?"));
            return;
        }
        if (messageArgs.length == 0) {
            feedbackPlayer(sender, messages.component("commands.reply-usage"));
            return;
        }
        sendTell(sender, target, String.join(" ", messageArgs));
    }

    public void sendTell(Player sender, Player target, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            feedbackPlayer(sender, messages.component("commands.tell-self"));
            return;
        }
        if (!playerPrefs.canReceiveTellFrom(target, sender)) {
            feedbackPlayer(sender, messages.component("tell.error.target-blocks-strangers", "target", target.getName()));
            return;
        }

        Component outgoing = messages.component(
                "tell.outgoing",
                "target", target.getName(),
                "message", message);
        Component incoming = messages.component(
                "tell.incoming",
                "sender", sender.getName(),
                "message", message);

        sender.sendMessage(outgoing);
        target.sendMessage(incoming);

        feedback.send(sender, outgoing, FeedbackPriority.NORMAL, false);
        feedback.send(target, incoming, FeedbackPriority.NORMAL, false);
        configManager.playTellNotification(target);

        playerPrefs.markKnown(sender.getUniqueId(), target.getUniqueId());
        rememberPartners(sender, target);
    }

    private void openTellDialog(Player sender, String targetName) {
        Player target = resolveOnlinePlayer(targetName);
        if (target == null) {
            feedbackPlayer(sender, messages.component("commands.player-not-found", "player", targetName));
            return;
        }
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            feedbackPlayer(sender, messages.component("commands.tell-self"));
            return;
        }

        Player resolvedTarget = target;
        MineSkyDialog.builder(messages.raw("tell.dialog.title"))
                .body(messages.format("tell.dialog.body", "target", resolvedTarget.getName()))
                .addInput(TextInput.builder(INPUT_MESSAGE, messages.raw("tell.dialog.input-label"))
                        .placeholder(messages.raw("tell.dialog.input-placeholder"))
                        .maxLength(256)
                        .build())
                .button(messages.raw("tell.dialog.send-button"), (player, response) -> {
                    String text = response.getText(INPUT_MESSAGE);
                    if (text == null || text.isBlank()) {
                        return;
                    }
                    sendTell(player, resolvedTarget, text.trim());
                })
                .show(sender);
    }

    private void rememberPartners(Player a, Player b) {
        lastPartner.put(a.getUniqueId(), b.getUniqueId());
        lastPartner.put(b.getUniqueId(), a.getUniqueId());
    }

    private Player resolveOnlinePlayer(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return exact;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(name)) {
                return online;
            }
        }
        return null;
    }

    private void feedbackPlayer(Player player, Component message) {
        feedback.send(player, message, FeedbackPriority.HIGH, true, UiSoundMode.ERROR);
    }
}
