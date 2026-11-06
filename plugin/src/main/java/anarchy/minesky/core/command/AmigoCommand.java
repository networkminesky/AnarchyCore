package anarchy.minesky.core.command;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.friends.FriendsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AmigoCommand implements CommandExecutor, TabCompleter {

    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final FriendsManager friends;

    public AmigoCommand(MessagesManager messages, FeedbackManager feedback, FriendsManager friends) {
        this.messages = messages;
        this.feedback = feedback;
        this.friends = friends;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.component("commands.player-only"));
            return true;
        }
        if (!player.hasPermission("anarchycore.friends")) {
            feedback.send(
                    player,
                    messages.component("commands.no-permission"),
                    FeedbackPriority.HIGH,
                    true,
                    UiSoundMode.ERROR);
            return true;
        }
        if (args.length == 0) {
            friends.openMainDialog(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "adicionar", "add" -> {
                if (args.length < 2) {
                    feedback.send(
                            player,
                            messages.component("friends.usage.adicionar"),
                            FeedbackPriority.HIGH,
                            true,
                            UiSoundMode.ERROR);
                    yield true;
                }
                friends.sendFriendRequestByNick(player, args[1]);
                yield true;
            }
            case "aceitar", "accept" -> {
                if (args.length < 2) {
                    feedback.send(
                            player,
                            messages.component("friends.usage.aceitar"),
                            FeedbackPriority.HIGH,
                            true,
                            UiSoundMode.ERROR);
                    yield true;
                }
                friends.acceptFriendRequestByNick(player, args[1]);
                yield true;
            }
            case "recusar", "negar", "deny", "reject" -> {
                if (args.length < 2) {
                    feedback.send(
                            player,
                            messages.component("friends.usage.recusar"),
                            FeedbackPriority.HIGH,
                            true,
                            UiSoundMode.ERROR);
                    yield true;
                }
                friends.denyFriendRequestByNick(player, args[1]);
                yield true;
            }
            case "remover", "remove", "del" -> {
                if (args.length < 2) {
                    feedback.send(
                            player,
                            messages.component("friends.usage.remover"),
                            FeedbackPriority.HIGH,
                            true,
                            UiSoundMode.ERROR);
                    yield true;
                }
                friends.removeFriend(player, args[1]);
                yield true;
            }
            case "listar", "list", "lista" -> {
                friends.listFriendsChat(player);
                yield true;
            }
            default -> {
                feedback.send(
                        player,
                        messages.component("friends.usage.generic"),
                        FeedbackPriority.HIGH,
                        true,
                        UiSoundMode.ERROR);
                yield true;
            }
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        if (args.length == 1) {
            String p = args[0].toLowerCase(Locale.ROOT);
            List<String> subs = List.of("adicionar", "aceitar", "recusar", "remover", "listar");
            List<String> out = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(p)) {
                    out.add(s);
                }
            }
            return out;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("adicionar") || sub.equals("add") || sub.equals("remover") || sub.equals("remove") || sub.equals("del")) {
                String prefix = args[1].toLowerCase(Locale.ROOT);
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .toList();
            }
            if (sub.equals("aceitar") || sub.equals("accept") || sub.equals("recusar") || sub.equals("negar")
                    || sub.equals("deny") || sub.equals("reject")) {
                return friends.tabCompleteIncomingSenderNicks(player, args[1]);
            }
        }
        return List.of();
    }
}
