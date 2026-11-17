package anarchy.minesky.core.command;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
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

/**
 * {@code /gm <0|1|2|3|modo> [jogador]} e atalhos {@code /gmc}, {@code /gms}, {@code /gma}, {@code /gmsp}.
 * O atalho vem do label registrado, não de um alias — Paper entrega o label do bind.
 */
public final class GamemodeCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "anarchycore.gamemode";
    public static final String OTHERS_PERMISSION = "anarchycore.gamemode.others";

    private static final List<String> MODES = List.of(
            "0", "1", "2", "3", "survival", "creative", "adventure", "spectator");

    private final Plugin plugin;
    private final MessagesManager messages;
    private final FeedbackManager feedback;

    public GamemodeCommand(Plugin plugin, MessagesManager messages, FeedbackManager feedback) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        GameMode shortcut = modeFromLabel(label);
        if (shortcut == null) {
            return gm(sender, args);
        }
        return applyShortcut(sender, shortcut, args);
    }

    private boolean gm(CommandSender sender, String[] args) {
        if (args.length == 0 || args.length > 2) {
            reply(sender, "gamemode.usage", FeedbackPriority.HIGH, UiSoundMode.ERROR);
            return true;
        }
        GameMode mode = parseMode(args[0]);
        if (mode == null) {
            reply(sender, "gamemode.invalid", FeedbackPriority.HIGH, UiSoundMode.ERROR, "mode", args[0]);
            return true;
        }
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                reply(sender, "gamemode.console-needs-player", FeedbackPriority.HIGH, UiSoundMode.ERROR);
                return true;
            }
            if (!player.hasPermission(PERMISSION)) {
                deny(player);
                return true;
            }
            apply(sender, player, mode);
            return true;
        }
        if (!sender.hasPermission(OTHERS_PERMISSION)) {
            deny(sender);
            return true;
        }
        Player target = CommandPlayers.resolve(args[1]);
        if (target == null) {
            missing(sender, args[1]);
            return true;
        }
        apply(sender, target, mode);
        return true;
    }

    private boolean applyShortcut(CommandSender sender, GameMode mode, String[] args) {
        if (args.length > 1) {
            reply(sender, "gamemode.shortcut-usage", FeedbackPriority.HIGH, UiSoundMode.ERROR);
            return true;
        }
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                reply(sender, "gamemode.console-needs-player", FeedbackPriority.HIGH, UiSoundMode.ERROR);
                return true;
            }
            if (!player.hasPermission(PERMISSION)) {
                deny(player);
                return true;
            }
            apply(sender, player, mode);
            return true;
        }
        if (!sender.hasPermission(OTHERS_PERMISSION)) {
            deny(sender);
            return true;
        }
        Player target = CommandPlayers.resolve(args[0]);
        if (target == null) {
            missing(sender, args[0]);
            return true;
        }
        apply(sender, target, mode);
        return true;
    }

    private void apply(CommandSender sender, Player target, GameMode mode) {
        target.getScheduler().run(plugin, task -> {
            if (!target.isOnline()) {
                missing(sender, target.getName());
                return;
            }
            target.setGameMode(mode);
            String label = label(mode);
            if (sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId())) {
                reply(sender, "gamemode.self", FeedbackPriority.NORMAL, UiSoundMode.SUCCESS, "mode", label);
                return;
            }
            reply(sender, "gamemode.other", FeedbackPriority.NORMAL, UiSoundMode.SUCCESS,
                    "target", target.getName(), "mode", label);
            feedback.send(
                    target,
                    messages.component("gamemode.target", "player", sender.getName(), "mode", label),
                    FeedbackPriority.NORMAL,
                    true,
                    UiSoundMode.SUCCESS);
        }, null);
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION) && !sender.hasPermission(OTHERS_PERMISSION)) {
            return List.of();
        }
        GameMode shortcut = modeFromLabel(alias);
        if (shortcut == null) {
            if (args.length == 1) {
                return filter(args[0], MODES);
            }
            if (args.length == 2 && sender.hasPermission(OTHERS_PERMISSION)) {
                return PlayerNameTabComplete.onlineNames(args, 1);
            }
            return List.of();
        }
        if (args.length == 1 && sender.hasPermission(OTHERS_PERMISSION)) {
            return PlayerNameTabComplete.onlineNames(args, 0);
        }
        return List.of();
    }

    static @Nullable GameMode modeFromLabel(String label) {
        return switch (label.toLowerCase(Locale.ROOT)) {
            case "gmc" -> GameMode.CREATIVE;
            case "gms" -> GameMode.SURVIVAL;
            case "gma" -> GameMode.ADVENTURE;
            case "gmsp" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    static @Nullable GameMode parseMode(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "0", "s", "survival", "sobrevivencia" -> GameMode.SURVIVAL;
            case "1", "c", "creative", "criativo" -> GameMode.CREATIVE;
            case "2", "a", "adventure", "aventura" -> GameMode.ADVENTURE;
            case "3", "sp", "spectator", "espectador", "gmsp" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    static String label(GameMode mode) {
        return switch (mode) {
            case SURVIVAL -> "Sobrevivência";
            case CREATIVE -> "Criativo";
            case ADVENTURE -> "Aventura";
            case SPECTATOR -> "Espectador";
        };
    }

    private static List<String> filter(String prefix, List<String> values) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.startsWith(lower)).toList();
    }

    private void missing(CommandSender sender, String name) {
        reply(sender, "commands.player-not-found", FeedbackPriority.HIGH, UiSoundMode.ERROR, "player", name);
    }

    private void deny(CommandSender sender) {
        reply(sender, "commands.no-permission", FeedbackPriority.HIGH, UiSoundMode.ERROR);
    }

    private void reply(CommandSender sender, String path, FeedbackPriority priority, UiSoundMode sound, String... placeholders) {
        Component message = messages.component(path, placeholders);
        if (sender instanceof Player player) {
            feedback.send(player, message, priority, true, sound);
        } else {
            sender.sendMessage(message);
        }
    }
}
