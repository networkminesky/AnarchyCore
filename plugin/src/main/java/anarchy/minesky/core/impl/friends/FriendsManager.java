package anarchy.minesky.core.impl.friends;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.dialogapi.PlayerPickerDialogs;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.api.text.DialogComponentUtils;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.playerdata.PlayerDataAPIImpl;
import anarchy.minesky.core.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import java.util.function.Predicate;

/**
 * Lista de amigos, diálogos e notificações de entrada.
 */
public final class FriendsManager {

    /** Limite de botões no diálogo; reserva para Adicionar, fechar e até 2 de paginação. */
    private static final int MAX_DIALOG_BUTTONS = 40;
    private static final int RESERVED_LIST_BUTTONS = 4;

    private final Plugin plugin;
    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final FriendsRepository friends;
    private final PlayerDataAPIImpl playerDataApi;

    public FriendsManager(
            Plugin plugin,
            MessagesManager messages,
            FeedbackManager feedback,
            FriendsRepository friends,
            PlayerDataAPIImpl playerDataApi) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
        this.friends = friends;
        this.playerDataApi = playerDataApi;
    }

    /** Jogadores online visíveis no seletor (não vanish para o viewer). */
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
        player.getScheduler().run(plugin, task -> showFriendsListDialog(player, 0), null);
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
        PlayerPickerDialogs.open(player, title, visibleOnline(player), nick -> tryAddByNickFromDialog(player, nick));
    }

    private void tryAddByNickFromDialog(Player player, String nick) {
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
            addFriend(player, target, true);
        }, null);
    }

    private void showFriendsListDialog(Player player, int page) {
        if (!player.isOnline()) {
            return;
        }
        List<UUID> friendIds = friends.listFriendUuids(player.getUniqueId());
        List<FriendRow> rows = buildFriendRows(friendIds);

        int pageSize = MAX_DIALOG_BUTTONS - RESERVED_LIST_BUTTONS;
        int totalPages = Math.max(1, (rows.size() + pageSize - 1) / pageSize);
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));
        int from = currentPage * pageSize;
        int to = Math.min(from + pageSize, rows.size());
        List<FriendRow> slice = rows.subList(from, to);

        MineSkyDialog.Builder builder = MineSkyDialog.builder(messages.component("friends.dialog.list-title"))
                .body(messages.component("friends.dialog.list-body", "count", String.valueOf(rows.size())))
                .multiActionColumns(1);

        for (FriendRow row : slice) {
            Component status = row.online
                    ? messages.component("friends.status.online")
                    : messages.component("friends.status.offline");
            Component label = Component.text(row.displayName, NamedTextColor.WHITE)
                    .append(Component.space())
                    .append(status);
            builder.button(DialogButton.builder()
                    .label(label)
                    .width(280)
                    .tooltip(Component.text(row.displayName, NamedTextColor.GRAY))
                    .onClick(p -> { })
                    .build());
        }

        builder.button(DialogButton.builder()
                .label(messages.component("friends.dialog.add-button"))
                .width(220)
                .onClick(p -> openAddPicker(p))
                .build());

        if (totalPages > 1) {
            if (currentPage > 0) {
                int prev = currentPage - 1;
                builder.button(DialogButton.builder()
                        .label(Component.text("« " + messages.raw("friends.dialog.page-prev")))
                        .width(160)
                        .onClick(p -> showFriendsListDialog(p, prev))
                        .build());
            }
            if (currentPage < totalPages - 1) {
                int next = currentPage + 1;
                builder.button(DialogButton.builder()
                        .label(Component.text(messages.raw("friends.dialog.page-next") + " »"))
                        .width(160)
                        .onClick(p -> showFriendsListDialog(p, next))
                        .build());
            }
        }

        builder.button(DialogButton.builder()
                .label(messages.component("friends.dialog.close-button"))
                .width(120)
                .onClick(p -> { })
                .build());

        player.getScheduler().run(plugin, task -> {
            if (player.isOnline()) {
                builder.show(player);
            }
        }, null);
    }

    private List<FriendRow> buildFriendRows(List<UUID> friendIds) {
        List<FriendRow> rows = new ArrayList<>();
        for (UUID id : friendIds) {
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
            rows.add(new FriendRow(id, name, isOnline));
        }
        rows.sort(Comparator
                .comparing((FriendRow r) -> !r.online)
                .thenComparing(r -> r.displayName.toLowerCase(Locale.ROOT)));
        return rows;
    }

    private record FriendRow(UUID uuid, String displayName, boolean online) {}

    public void addFriend(Player player, Player target, boolean fromDialog) {
        if (!friends.isActive()) {
            feedback.send(
                    player,
                    messages.component("friends.error.database"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        if (target == null || target.equals(player)) {
            feedback.send(
                    player,
                    messages.component("friends.error.self"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        if (friends.areFriends(player.getUniqueId(), target.getUniqueId())) {
            feedback.send(
                    player,
                    messages.component("friends.error.already", "player", target.getName()),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        playerDataApi.touchPlayerAsync(target.getUniqueId(), target.getName());
        boolean added = friends.addFriend(player.getUniqueId(), target.getUniqueId());
        if (!added) {
            feedback.send(
                    player,
                    messages.component("friends.error.already", "player", target.getName()),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        feedback.send(
                player,
                messages.component("friends.added", "player", target.getName()),
                FeedbackPriority.NORMAL,
                true,
                UiSoundMode.SUCCESS);
        if (fromDialog) {
            player.getScheduler().runDelayed(plugin, task -> {
                if (player.isOnline()) {
                    showFriendsListDialog(player, 0);
                }
            }, null, 2L);
        }
    }

    /** Adiciona pelo nick (online ou UUID conhecido no cache / já jogou). */
    public void addFriendByNick(Player player, String nick) {
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
            addFriend(player, online, false);
            return;
        }
        if (!target.hasPlayedBefore()) {
            feedback.send(
                    player,
                    messages.component("commands.player-not-found", "player", nick),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        String name = target.getName() != null ? target.getName() : nick;
        playerDataApi.touchPlayerAsync(target.getUniqueId(), name);
        if (friends.areFriends(player.getUniqueId(), target.getUniqueId())) {
            feedback.send(
                    player,
                    messages.component("friends.error.already", "player", name),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        boolean added = friends.addFriend(player.getUniqueId(), target.getUniqueId());
        if (!added) {
            feedback.send(
                    player,
                    messages.component("friends.error.already", "player", name),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        feedback.send(
                player,
                messages.component("friends.added", "player", name),
                FeedbackPriority.NORMAL,
                true,
                UiSoundMode.SUCCESS);
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
        boolean removed = friends.removeFriend(player.getUniqueId(), target.getUniqueId());
        if (!removed) {
            feedback.send(
                    player,
                    messages.component("friends.error.not-friend", "player", targetNick),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return;
        }
        feedback.send(
                player,
                messages.component("friends.removed", "player", target.getName() != null ? target.getName() : targetNick),
                FeedbackPriority.NORMAL,
                true,
                UiSoundMode.SUCCESS);
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
                    feedback.send(
                            viewer,
                            messages.component("friends.notify-join", "player", joined.getName()),
                            FeedbackPriority.NORMAL,
                            true,
                            UiSoundMode.TELL);
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
