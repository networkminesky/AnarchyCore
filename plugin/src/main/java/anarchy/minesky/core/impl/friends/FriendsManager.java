package anarchy.minesky.core.impl.friends;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.dialogapi.PlayerPickerDialogs;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.api.playerconfig.PlayerConfigAPI;
import anarchy.minesky.core.api.playerconfig.PlayerConfigKeys;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.messaging.PrivateMessageService;
import anarchy.minesky.core.impl.playerdata.PlayerDataAPIImpl;
import anarchy.minesky.core.impl.playerprefs.ConfigManager;
import anarchy.minesky.core.impl.tpa.TpaManager;
import anarchy.minesky.core.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.function.Predicate;

/**
 * Lista de amigos, pedidos de amizade, diálogos e notificações de entrada.
 */
public final class FriendsManager {

    private static final int MAX_DIALOG_BUTTONS = 40;
    private static final int ACTION_BTN_WIDTH = 280;
    /** Hub: só botões de ação. */
    private static final int RESERVED_HUB = 4;
    /** Lista de amigos: amigos + voltar + fechar + até 2 de paginação. */
    private static final int RESERVED_FRIEND_LIST = 4;
    /** Diálogo de pedidos: pares aceitar/recusar + voltar + fechar + até 2 pag. */
    private static final int RESERVED_REQUESTS = 4;

    private final Plugin plugin;
    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final FriendsRepository friends;
    private final PlayerDataAPIImpl playerDataApi;
    private final PrivateMessageService privateMessages;
    private final TpaManager tpaManager;
    private final PlayerConfigAPI playerConfig;
    private final ConfigManager configManager;

    public FriendsManager(
            Plugin plugin,
            MessagesManager messages,
            FeedbackManager feedback,
            FriendsRepository friends,
            PlayerDataAPIImpl playerDataApi,
            PrivateMessageService privateMessages,
            TpaManager tpaManager,
            PlayerConfigAPI playerConfig,
            ConfigManager configManager) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
        this.friends = friends;
        this.playerDataApi = playerDataApi;
        this.privateMessages = privateMessages;
        this.tpaManager = tpaManager;
        this.playerConfig = playerConfig;
        this.configManager = configManager;
    }

    public Predicate<Player> visibleOnline(Player viewer) {
        return target -> target != null
                && target.isOnline()
                && !target.getUniqueId().equals(viewer.getUniqueId())
                && viewer.canSee(target)
                && !Utils.isVanished(target);
    }

    public void openMainDialog(Player player) {
        if (!friends.isActive()) {
            feedback.send(
                    player,
                    messages.component("friends.error.database"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        player.getScheduler().run(plugin, task -> showFriendsHubDialog(player), null);
    }

    public void openAddPicker(Player player) {
        if (!friends.isActive()) {
            feedback.send(
                    player,
                    messages.component("friends.error.database"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        Component title = messages.component("friends.dialog.pick-player-title");
        PlayerPickerDialogs.open(player, title, visibleOnline(player), nick -> trySendRequestByNickFromDialog(player, nick));
    }

    public void openRequestsDialog(Player player) {
        if (!friends.isActive()) {
            feedback.send(
                    player,
                    messages.component("friends.error.database"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        player.getScheduler().run(plugin, task -> showRequestsDialog(player, 0), null);
    }

    private void trySendRequestByNickFromDialog(Player player, String nick) {
        player.getScheduler().run(plugin, task -> {
            Player target = Bukkit.getPlayerExact(nick);
            if (target == null) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getName().equalsIgnoreCase(nick)) {
                        target = online;
                        break;
                    }
                }
            }
            if (target == null || !target.isOnline()) {
                feedback.send(
                        player,
                        messages.component("friends.error.not-online", "player", nick),
                        FeedbackPriority.HIGH,
                        true,
                        UiSoundMode.ERROR);
                return;
            }
            if (!visibleOnline(player).test(target)) {
                feedback.send(
                        player,
                        messages.component("friends.error.cannot-add"),
                        FeedbackPriority.HIGH,
                        true,
                        UiSoundMode.ERROR);
                return;
            }
            sendFriendRequest(player, target, true);
        }, null);
    }

    private void showFriendsHubDialog(Player player) {
        if (!player.isOnline()) {
            return;
        }
        List<UUID> friendIds = friends.listFriendUuids(player.getUniqueId());
        int friendCount = friendIds.size();
        int pendingCount = friends.listIncomingRequestFromUuids(player.getUniqueId()).size();

        MineSkyDialog.Builder builder = MineSkyDialog.builder(messages.component("friends.dialog.hub-title"))
                .body(messages.component(
                        "friends.dialog.hub-body",
                        "count", String.valueOf(friendCount),
                        "pending", String.valueOf(pendingCount)))
                .multiActionColumns(1);

        builder.button(DialogButton.builder()
                .label(messages.component("friends.dialog.open-friends-list", "count", String.valueOf(friendCount)))
                .width(ACTION_BTN_WIDTH)
                .onClick(p -> showFriendButtonListDialog(p, 0))
                .build());

        builder.button(DialogButton.builder()
                .label(messages.component("friends.dialog.requests-button", "count", String.valueOf(pendingCount)))
                .width(ACTION_BTN_WIDTH)
                .onClick(p -> showRequestsDialog(p, 0))
                .build());

        builder.button(DialogButton.builder()
                .label(messages.component("friends.dialog.add-button"))
                .width(ACTION_BTN_WIDTH)
                .onClick(p -> openAddPicker(p))
                .build());

        builder.button(DialogButton.builder()
                .label(messages.component("friends.dialog.close-button"))
                .width(ACTION_BTN_WIDTH)
                .onClick(p -> {
                })
                .build());

        player.getScheduler().run(plugin, task -> {
            if (player.isOnline()) {
                builder.show(player);
            }
        }, null);
    }

    /**
     * Lista cada amigo como botão; ao clicar abre ações (TPA, tell, remover).
     */
    private void showFriendButtonListDialog(Player player, int page) {
        if (!player.isOnline()) {
            return;
        }
        List<UUID> friendIds = friends.listFriendUuids(player.getUniqueId());
        List<FriendRow> rows = buildFriendRows(friendIds);

        int pageSize = MAX_DIALOG_BUTTONS - RESERVED_FRIEND_LIST;
        int totalPages = Math.max(1, (rows.size() + pageSize - 1) / pageSize);
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));
        int from = currentPage * pageSize;
        int to = Math.min(from + pageSize, rows.size());
        List<FriendRow> slice = rows.subList(from, to);

        MineSkyDialog.Builder builder = MineSkyDialog.builder(messages.component("friends.dialog.friends-list-title"))
                .body(messages.component(
                        "friends.dialog.friends-list-body",
                        "count", String.valueOf(rows.size()),
                        "page", String.valueOf(currentPage + 1),
                        "total_pages", String.valueOf(totalPages)))
                .multiActionColumns(1);

        if (slice.isEmpty()) {
            builder.body(messages.component("friends.dialog.friends-list-empty"));
        }

        for (FriendRow row : slice) {
            builder.button(DialogButton.builder()
                    .label(friendListButtonLabel(row))
                    .width(ACTION_BTN_WIDTH)
                    .onClick(p -> showFriendDetailDialog(p, row))
                    .build());
        }

        builder.button(DialogButton.builder()
                .label(messages.component("friends.dialog.back-to-hub"))
                .width(ACTION_BTN_WIDTH)
                .onClick(p -> showFriendsHubDialog(p))
                .build());

        if (totalPages > 1) {
            if (currentPage > 0) {
                int prev = currentPage - 1;
                builder.button(DialogButton.builder()
                        .label(Component.text("« " + messages.raw("friends.dialog.page-prev")))
                        .width(ACTION_BTN_WIDTH)
                        .onClick(p -> showFriendButtonListDialog(p, prev))
                        .build());
            }
            if (currentPage < totalPages - 1) {
                int next = currentPage + 1;
                builder.button(DialogButton.builder()
                        .label(Component.text(messages.raw("friends.dialog.page-next") + " »"))
                        .width(ACTION_BTN_WIDTH)
                        .onClick(p -> showFriendButtonListDialog(p, next))
                        .build());
            }
        }

        builder.button(DialogButton.builder()
                .label(messages.component("friends.dialog.close-button"))
                .width(ACTION_BTN_WIDTH)
                .onClick(p -> {
                })
                .build());

        player.getScheduler().run(plugin, task -> {
            if (player.isOnline()) {
                builder.show(player);
            }
        }, null);
    }

    private Component friendListButtonLabel(FriendRow row) {
        String statusMini = messages.raw(row.online ? "friends.status.online" : "friends.status.offline");
        String filled = messages.format(
                messages.raw("friends.dialog.friend-list-button"),
                "player", row.displayName,
                "status", statusMini);
        return DialogComponentUtils.parse(filled);
    }

    private void showFriendDetailDialog(Player viewer, FriendRow row) {
        if (!viewer.isOnline()) {
            return;
        }
        MineSkyDialog.Builder builder = MineSkyDialog.builder(messages.component(
                        "friends.dialog.friend-detail-title",
                        "player", row.displayName))
                .body(messages.component(
                        "friends.dialog.friend-detail-body",
                        "player", row.displayName,
                        "status", messages.raw(row.online ? "friends.status.online" : "friends.status.offline")))
                .multiActionColumns(1);

        if (row.online) {
            Player target = Bukkit.getPlayer(row.uuid);
            if (target != null && target.isOnline() && visibleOnline(viewer).test(target)) {
                builder.button(DialogButton.builder()
                        .label(messages.component("friends.dialog.friend-tpa-button", "player", row.displayName))
                        .width(ACTION_BTN_WIDTH)
                        .onClick(p -> {
                            Player t = Bukkit.getPlayer(row.uuid);
                            if (t != null && t.isOnline() && visibleOnline(p).test(t)) {
                                tpaManager.requestTpa(p, t);
                            } else {
                                feedback.send(
                                        p,
                                        messages.component("friends.error.friend-offline-action", "player", row.displayName),
                                        FeedbackPriority.HIGH,
                                        true,
                                        UiSoundMode.ERROR);
                            }
                        })
                        .build());
            }
        }

        builder.button(DialogButton.builder()
                .label(messages.component("friends.dialog.friend-tell-button", "player", row.displayName))
                .width(ACTION_BTN_WIDTH)
                .onClick(p -> privateMessages.openTellDialogForFriend(p, row.uuid, row.displayName))
                .build());

        builder.button(DialogButton.builder()
                .label(messages.component("friends.dialog.friend-remove-button", "player", row.displayName))
                .width(ACTION_BTN_WIDTH)
                .onClick(p -> {
                    removeFriend(p, row.displayName);
                    showFriendButtonListDialog(p, 0);
                })
                .build());

        builder.button(DialogButton.builder()
                .label(messages.component("friends.dialog.back-to-friend-list"))
                .width(ACTION_BTN_WIDTH)
                .onClick(p -> showFriendButtonListDialog(p, 0))
                .build());

        builder.button(DialogButton.builder()
                .label(messages.component("friends.dialog.close-button"))
                .width(ACTION_BTN_WIDTH)
                .onClick(p -> {
                })
                .build());

        viewer.getScheduler().run(plugin, task -> {
            if (viewer.isOnline()) {
                builder.show(viewer);
            }
        }, null);
    }

    private void showRequestsDialog(Player viewer, int page) {
        if (!viewer.isOnline()) {
            return;
        }
        List<UUID> fromIds = friends.listIncomingRequestFromUuids(viewer.getUniqueId());
        List<RequestRow> rows = new ArrayList<>();
        for (UUID fromId : fromIds) {
            rows.add(new RequestRow(fromId, resolveDisplayName(fromId)));
        }
        rows.sort(Comparator.comparing(r -> r.displayName.toLowerCase(Locale.ROOT)));

        int pageSize = (MAX_DIALOG_BUTTONS - RESERVED_REQUESTS) / 2;
        if (pageSize < 1) {
            pageSize = 1;
        }
        int totalPages = Math.max(1, (rows.size() + pageSize - 1) / pageSize);
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));
        int from = currentPage * pageSize;
        int to = Math.min(from + pageSize, rows.size());
        List<RequestRow> slice = rows.subList(from, to);

        MineSkyDialog.Builder builder = MineSkyDialog.builder(messages.component("friends.dialog.requests-title"))
                .body(messages.component("friends.dialog.requests-body", "count", String.valueOf(rows.size())))
                .multiActionColumns(2);

        for (RequestRow row : slice) {
            UUID senderId = row.uuid();
            builder.button(DialogButton.builder()
                    .label(messages.component("friends.dialog.request-accept-label", "player", row.displayName))
                    .width(ACTION_BTN_WIDTH)
                    .onClick(p -> acceptFriendRequestByUuid(p, senderId))
                    .build());
            builder.button(DialogButton.builder()
                    .label(messages.component("friends.dialog.request-deny-label", "player", row.displayName))
                    .width(ACTION_BTN_WIDTH)
                    .onClick(p -> denyFriendRequestByUuid(p, senderId, true))
                    .build());
        }

        builder.button(DialogButton.builder()
                .label(messages.component("friends.dialog.back-to-hub"))
                .width(ACTION_BTN_WIDTH)
                .onClick(p -> showFriendsHubDialog(p))
                .build());

        if (totalPages > 1) {
            if (currentPage > 0) {
                int prev = currentPage - 1;
                builder.button(DialogButton.builder()
                        .label(Component.text("« " + messages.raw("friends.dialog.page-prev")))
                        .width(ACTION_BTN_WIDTH)
                        .onClick(p -> showRequestsDialog(p, prev))
                        .build());
            }
            if (currentPage < totalPages - 1) {
                int next = currentPage + 1;
                builder.button(DialogButton.builder()
                        .label(Component.text(messages.raw("friends.dialog.page-next") + " »"))
                        .width(ACTION_BTN_WIDTH)
                        .onClick(p -> showRequestsDialog(p, next))
                        .build());
            }
        }

        builder.button(DialogButton.builder()
                .label(messages.component("friends.dialog.close-button"))
                .width(ACTION_BTN_WIDTH)
                .onClick(p -> {
                })
                .build());

        viewer.getScheduler().run(plugin, task -> {
            if (viewer.isOnline()) {
                builder.show(viewer);
            }
        }, null);
    }

    private void acceptFriendRequestByUuid(Player player, UUID fromUuid) {
        OfflinePlayer fromOff = Bukkit.getOfflinePlayer(fromUuid);
        String name = fromOff.getName();
        if (name == null || name.isBlank()) {
            name = fromUuid.toString();
        }
        acceptFriendRequest(player, fromUuid, name, true);
    }

    public void denyFriendRequestByNick(Player player, String nick) {
        OfflinePlayer fromOff = resolveOffline(nick);
        if (fromOff == null) {
            feedback.send(
                    player,
                    messages.component("commands.player-not-found", "player", nick),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        UUID fromUuid = fromOff.getUniqueId();
        if (!friends.hasIncomingRequest(player.getUniqueId(), fromUuid)) {
            feedback.send(
                    player,
                    messages.component("friends.error.no-request-from", "player", nick),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        denyFriendRequestByUuid(player, fromUuid, false);
    }

    private void denyFriendRequestByUuid(Player player, UUID fromUuid, boolean reopenRequestsDialog) {
        if (!friends.isActive()) {
            return;
        }
        if (!friends.deleteRequest(fromUuid, player.getUniqueId())) {
            return;
        }
        feedback.send(
                player,
                messages.component("friends.request-denied-you", "player", resolveDisplayName(fromUuid)),
                FeedbackPriority.NORMAL,
                true,
                UiSoundMode.SUCCESS);
        if (reopenRequestsDialog) {
            player.getScheduler().runDelayed(plugin, task -> {
                if (player.isOnline()) {
                    showRequestsDialog(player, 0);
                }
            }, null, 2L);
        }
    }

    public List<String> tabCompleteIncomingSenderNicks(Player player, String prefix) {
        if (!friends.isActive() || prefix == null) {
            return List.of();
        }
        String p = prefix.toLowerCase(Locale.ROOT);
        return friends.listIncomingRequestFromUuids(player.getUniqueId()).stream()
                .map(this::resolveDisplayName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(p))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    private List<FriendRow> buildFriendRows(List<UUID> friendIds) {
        List<FriendRow> rows = new ArrayList<>();
        for (UUID id : friendIds) {
            rows.add(buildFriendRow(id));
        }
        rows.sort(Comparator
                .comparing((FriendRow r) -> !r.online)
                .thenComparing(r -> r.displayName.toLowerCase(Locale.ROOT)));
        return rows;
    }

    private FriendRow buildFriendRow(UUID id) {
        Player online = Bukkit.getPlayer(id);
        String name;
        boolean isOnline = online != null && online.isOnline();
        if (isOnline) {
            name = online.getName();
        } else {
            OfflinePlayer off = Bukkit.getOfflinePlayer(id);
            name = off.getName();
            if (name == null || name.isBlank()) {
                name = id.toString();
            }
        }
        return new FriendRow(id, name, isOnline);
    }

    private String resolveDisplayName(UUID id) {
        return buildFriendRow(id).displayName;
    }

    private record FriendRow(UUID uuid, String displayName, boolean online) {}

    private record RequestRow(UUID uuid, String displayName) {}

    /**
     * Envia pedido de amizade (ou aceita automaticamente se o alvo já tiver pedido a você).
     */
    public void sendFriendRequest(Player player, Player target, boolean reopenMainDialog) {
        if (!trySendFriendRequestCore(player, target)) {
            return;
        }
        if (reopenMainDialog) {
            player.getScheduler().runDelayed(plugin, task -> {
                if (player.isOnline()) {
                    showFriendsHubDialog(player);
                }
            }, null, 2L);
        }
    }

    private boolean trySendFriendRequestCore(Player player, Player target) {
        if (!friends.isActive()) {
            feedback.send(
                    player,
                    messages.component("friends.error.database"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return false;
        }
        if (target == null || target.equals(player)) {
            feedback.send(
                    player,
                    messages.component("friends.error.self"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return false;
        }
        playerDataApi.touchPlayerAsync(player.getUniqueId(), player.getName());
        playerDataApi.touchPlayerAsync(target.getUniqueId(), target.getName());

        if (!playerConfig.getBoolean(PlayerConfigKeys.FRIENDS_ALLOW_INCOMING_REQUESTS, target)) {
            feedback.send(
                    player,
                    messages.component("friends.error.target-blocks-requests", "player", target.getName()),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.ERROR);
            return false;
        }

        if (friends.areFriends(player.getUniqueId(), target.getUniqueId())) {
            feedback.send(
                    player,
                    messages.component("friends.error.already", "player", target.getName()),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.ERROR);
            return false;
        }

        // O outro já te enviou pedido: amizade confirmada; notifica quem tinha pedido (target).
        if (friends.hasIncomingRequest(player.getUniqueId(), target.getUniqueId())) {
            return finalizeMutualFriendship(
                    player,
                    target.getUniqueId(),
                    target.getName(),
                    NotifyMode.AUTO_FROM_OUTGOING);
        }

        if (friends.hasOutgoingRequest(player.getUniqueId(), target.getUniqueId())) {
            feedback.send(
                    player,
                    messages.component("friends.error.request-pending"),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.ERROR);
            return false;
        }

        if (!friends.createRequest(player.getUniqueId(), target.getUniqueId())) {
            feedback.send(
                    player,
                    messages.component("friends.error.request-pending"),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.ERROR);
            return false;
        }

        feedback.send(
                player,
                messages.component("friends.request-sent", "player", target.getName()),
                FeedbackPriority.NORMAL,
                true,
                UiSoundMode.SUCCESS);

        feedback.send(
                target,
                messages.component("friends.request-received", "player", player.getName()),
                FeedbackPriority.NORMAL,
                true,
                UiSoundMode.NONE);
        configManager.playFriendRequestNotification(target);
        return true;
    }

    public void sendFriendRequestByNick(Player player, String nick) {
        if (!friends.isActive()) {
            feedback.send(
                    player,
                    messages.component("friends.error.database"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        OfflinePlayer target = resolveOffline(nick);
        if (target == null) {
            feedback.send(
                    player,
                    messages.component("commands.player-not-found", "player", nick),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            feedback.send(
                    player,
                    messages.component("friends.error.self"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }

        Player online = target instanceof Player tp ? tp : Bukkit.getPlayer(target.getUniqueId());
        if (online != null && online.isOnline()) {
            if (!visibleOnline(player).test(online)) {
                feedback.send(
                        player,
                        messages.component("friends.error.cannot-add"),
                        FeedbackPriority.HIGH,
                        true,
                        UiSoundMode.ERROR);
                return;
            }
            sendFriendRequest(player, online, false);
            return;
        }

        feedback.send(
                player,
                messages.component("friends.error.target-offline-request"),
                FeedbackPriority.HIGH,
                true,
                UiSoundMode.ERROR);
    }

    public void acceptFriendRequestByNick(Player player, String nick) {
        OfflinePlayer fromOff = resolveOffline(nick);
        if (fromOff == null) {
            feedback.send(
                    player,
                    messages.component("commands.player-not-found", "player", nick),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        String name = fromOff.getName() != null ? fromOff.getName() : nick;
        acceptFriendRequest(player, fromOff.getUniqueId(), name, false);
    }

    private void acceptFriendRequest(Player player, UUID fromUuid, String fromNameGuess, boolean fromDialog) {
        if (!friends.isActive()) {
            feedback.send(
                    player,
                    messages.component("friends.error.database"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        if (fromUuid.equals(player.getUniqueId())) {
            feedback.send(
                    player,
                    messages.component("friends.error.self"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        if (!friends.hasIncomingRequest(player.getUniqueId(), fromUuid)) {
            feedback.send(
                    player,
                    messages.component("friends.error.no-request-from", "player", fromNameGuess),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        playerDataApi.touchPlayerAsync(fromUuid, fromNameGuess);
        playerDataApi.touchPlayerAsync(player.getUniqueId(), player.getName());

        String otherName = resolveDisplayName(fromUuid);
        if (finalizeMutualFriendship(
                player,
                fromUuid,
                otherName,
                fromDialog ? NotifyMode.ACCEPT_FROM_DIALOG : NotifyMode.ACCEPT_FROM_COMMAND)
                && fromDialog) {
            player.getScheduler().runDelayed(plugin, task -> {
                if (player.isOnline()) {
                    showRequestsDialog(player, 0);
                }
            }, null, 2L);
        }
    }

    private enum NotifyMode {
        /** Aceitou um pedido recebido (notifica quem enviou). */
        ACCEPT_FROM_DIALOG,
        /** /amigo aceitar (mesmo que dialog). */
        ACCEPT_FROM_COMMAND,
        /** Enviou pedido e o outro já tinha pedido antes (notifica o outro jogador). */
        AUTO_FROM_OUTGOING
    }

    private boolean finalizeMutualFriendship(Player accepter, UUID otherUuid, String otherName, NotifyMode notifyMode) {
        friends.deleteRequest(otherUuid, accepter.getUniqueId());
        friends.deleteRequest(accepter.getUniqueId(), otherUuid);

        if (friends.areFriends(accepter.getUniqueId(), otherUuid)) {
            feedback.send(
                    accepter,
                    messages.component("friends.error.already", "player", otherName),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS);
            return false;
        }

        friends.addFriendshipMutual(accepter.getUniqueId(), otherUuid);

        switch (notifyMode) {
            case AUTO_FROM_OUTGOING -> {
                feedback.send(
                        accepter,
                        messages.component("friends.request-accepted-you", "player", otherName),
                        FeedbackPriority.NORMAL,
                        true,
                        UiSoundMode.SUCCESS);
                Player other = Bukkit.getPlayer(otherUuid);
                if (other != null && other.isOnline()) {
                    feedback.send(
                            other,
                            messages.component("friends.request-accepted-other", "player", accepter.getName()),
                            FeedbackPriority.NORMAL,
                            true,
                            UiSoundMode.TELL);
                }
            }
            case ACCEPT_FROM_DIALOG, ACCEPT_FROM_COMMAND -> {
                feedback.send(
                        accepter,
                        messages.component("friends.request-accepted-you", "player", otherName),
                        FeedbackPriority.NORMAL,
                        true,
                        UiSoundMode.SUCCESS);
                Player sender = Bukkit.getPlayer(otherUuid);
                if (sender != null && sender.isOnline()) {
                    feedback.send(
                            sender,
                            messages.component("friends.request-accepted-other", "player", accepter.getName()),
                            FeedbackPriority.NORMAL,
                            true,
                            UiSoundMode.TELL);
                }
            }
        }
        return true;
    }

    public void removeFriend(Player player, String targetNick) {
        if (!friends.isActive()) {
            feedback.send(
                    player,
                    messages.component("friends.error.database"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        OfflinePlayer target = resolveOffline(targetNick);
        if (target == null || target.getUniqueId().equals(player.getUniqueId())) {
            feedback.send(
                    player,
                    messages.component("commands.player-not-found", "player", targetNick),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        if (friends.removeFriendshipMutual(player.getUniqueId(), target.getUniqueId())) {
            feedback.send(
                    player,
                    messages.component("friends.removed", "player", target.getName() != null ? target.getName() : targetNick),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS);
        } else {
            feedback.send(
                    player,
                    messages.component("friends.error.not-friend", "player", targetNick),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
        }
    }

    public void listFriendsChat(Player player) {
        if (!friends.isActive()) {
            feedback.send(
                    player,
                    messages.component("friends.error.database"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        List<UUID> ids = friends.listFriendUuids(player.getUniqueId());
        if (ids.isEmpty()) {
            feedback.send(
                    player,
                    messages.component("friends.list-empty"),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS);
            return;
        }
        List<FriendRow> rows = buildFriendRows(ids);
        StringBuilder sb = new StringBuilder();
        sb.append(messages.raw("friends.list-header")).append("\n");
        for (FriendRow row : rows) {
            String line = row.online
                    ? messages.format("friends.list-line-online", "player", row.displayName)
                    : messages.format("friends.list-line-offline", "player", row.displayName);
            sb.append(line).append("\n");
        }
        feedback.send(
                player,
                DialogComponentUtils.parse(sb.toString().trim()),
                FeedbackPriority.NORMAL,
                true,
                UiSoundMode.SUCCESS);
    }

    public void notifyFriendsOfJoin(Player joined) {
        if (!friends.isActive()) {
            return;
        }
        if (Utils.isVanished(joined)) {
            return;
        }
        playerDataApi.touchPlayerAsync(joined.getUniqueId(), joined.getName());
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            Set<UUID> who = friends.whoListsAsFriend(joined.getUniqueId());
            plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
                if (!joined.isOnline()) {
                    return;
                }
                if (Utils.isVanished(joined)) {
                    return;
                }
                for (UUID viewerId : who) {
                    Player viewer = Bukkit.getPlayer(viewerId);
                    if (viewer == null || !viewer.isOnline()) {
                        continue;
                    }
                    if (!viewer.canSee(joined)) {
                        continue;
                    }
                    if (!playerConfig.getBoolean(PlayerConfigKeys.FRIENDS_NOTIFY_ON_FRIEND_JOIN, viewer)) {
                        continue;
                    }
                    feedback.send(
                            viewer,
                            messages.component("friends.notify-join", "player", joined.getName()),
                            FeedbackPriority.NORMAL,
                            true,
                            UiSoundMode.NONE);
                    configManager.playFriendJoinNotification(viewer);
                }
            });
        });
    }

    private static OfflinePlayer resolveOffline(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached != null && cached.hasPlayedBefore()) {
            return cached;
        }
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            String offlineName = offline.getName();
            if (offlineName != null
                    && offlineName.equalsIgnoreCase(name)
                    && offline.hasPlayedBefore()) {
                return offline;
            }
        }
        return null;
    }
}
