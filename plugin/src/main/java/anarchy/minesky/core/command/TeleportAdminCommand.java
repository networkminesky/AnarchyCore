package anarchy.minesky.core.command;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * {@code /tp [origem] destino} e {@code /tphere destino}.
 * A posição do destino é lida na região dele (Folia) antes do teleporte assíncrono.
 */
public final class TeleportAdminCommand implements CommandExecutor, TabCompleter {

    public static final String TP_PERMISSION = "anarchycore.tp";
    public static final String OTHERS_PERMISSION = "anarchycore.tp.others";
    public static final String HERE_PERMISSION = "anarchycore.tphere";

    private final Plugin plugin;
    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final boolean here;

    public TeleportAdminCommand(Plugin plugin, MessagesManager messages, FeedbackManager feedback, boolean here) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
        this.here = here;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (here) {
            return tphere(sender, args);
        }
        return tp(sender, args);
    }

    private boolean tp(CommandSender sender, String[] args) {
        if (args.length == 0 || args.length > 6) {
            reply(sender, "tp.usage", FeedbackPriority.HIGH, UiSoundMode.ERROR);
            return true;
        }

        if (isCoordinate(args[0])) {
            return tpCoords(sender, null, args, 0);
        }
        if (args.length >= 3 && isCoordinate(args[1])) {
            return tpCoords(sender, args[0], args, 1);
        }

        if (args.length > 2) {
            reply(sender, "tp.usage", FeedbackPriority.HIGH, UiSoundMode.ERROR);
            return true;
        }

        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                reply(sender, "tp.console-needs-two", FeedbackPriority.HIGH, UiSoundMode.ERROR);
                return true;
            }
            if (!player.hasPermission(TP_PERMISSION)) {
                deny(player);
                return true;
            }
            Player destination = CommandPlayers.resolve(args[0]);
            if (destination == null) {
                missing(sender, args[0]);
                return true;
            }
            if (destination.getUniqueId().equals(player.getUniqueId())) {
                centerOnBlock(player);
                return true;
            }
            teleport(sender, player, destination, "tp.self-to", "tp.arrived");
            return true;
        }

        if (!sender.hasPermission(OTHERS_PERMISSION)) {
            deny(sender);
            return true;
        }
        Player traveller = CommandPlayers.resolve(args[0]);
        if (traveller == null) {
            missing(sender, args[0]);
            return true;
        }
        Player destination = CommandPlayers.resolve(args[1]);
        if (destination == null) {
            missing(sender, args[1]);
            return true;
        }
        if (traveller.getUniqueId().equals(destination.getUniqueId())) {
            reply(sender, "tp.same-player", FeedbackPriority.HIGH, UiSoundMode.ERROR);
            return true;
        }
        teleport(sender, traveller, destination, "tp.other", "tp.moved");
        return true;
    }

    /**
     * {@code /tp x y z [yaw] [pitch]} ou {@code /tp <jogador> x y z [yaw] [pitch]}.
     * {@code ~} e {@code ~delta} são relativos à posição de quem viaja.
     */
    private boolean tpCoords(CommandSender sender, String travellerName, String[] args, int offset) {
        int count = args.length - offset;
        if (count < 3 || count > 5) {
            reply(sender, "tp.usage", FeedbackPriority.HIGH, UiSoundMode.ERROR);
            return true;
        }

        Player traveller;
        if (travellerName == null) {
            if (!(sender instanceof Player player)) {
                reply(sender, "tp.console-coords", FeedbackPriority.HIGH, UiSoundMode.ERROR);
                return true;
            }
            if (!player.hasPermission(TP_PERMISSION)) {
                deny(player);
                return true;
            }
            traveller = player;
        } else {
            if (!sender.hasPermission(OTHERS_PERMISSION)) {
                deny(sender);
                return true;
            }
            traveller = CommandPlayers.resolve(travellerName);
            if (traveller == null) {
                missing(sender, travellerName);
                return true;
            }
        }

        String xToken = args[offset];
        String yToken = args[offset + 1];
        String zToken = args[offset + 2];
        String yawToken = count >= 4 ? args[offset + 3] : null;
        String pitchToken = count == 5 ? args[offset + 4] : null;

        traveller.getScheduler().run(plugin, task -> {
            if (!traveller.isOnline()) {
                reply(sender, "tp.offline", FeedbackPriority.HIGH, UiSoundMode.ERROR);
                return;
            }
            Location origin = traveller.getLocation();
            Double x = parseCoord(xToken, origin.getX());
            Double y = parseCoord(yToken, origin.getY());
            Double z = parseCoord(zToken, origin.getZ());
            if (x == null || y == null || z == null || Math.abs(x) > 3.0E7 || Math.abs(z) > 3.0E7) {
                reply(sender, "tp.invalid-coords", FeedbackPriority.HIGH, UiSoundMode.ERROR);
                return;
            }
            if (y < origin.getWorld().getMinHeight() || y >= origin.getWorld().getMaxHeight()) {
                reply(sender, "tp.out-of-bounds", FeedbackPriority.HIGH, UiSoundMode.ERROR, "y", format(y));
                return;
            }
            float yaw = origin.getYaw();
            float pitch = origin.getPitch();
            if (yawToken != null) {
                Double parsedYaw = parseCoord(yawToken, yaw);
                if (parsedYaw == null) {
                    reply(sender, "tp.invalid-coords", FeedbackPriority.HIGH, UiSoundMode.ERROR);
                    return;
                }
                yaw = parsedYaw.floatValue();
            }
            if (pitchToken != null) {
                Double parsedPitch = parseCoord(pitchToken, pitch);
                if (parsedPitch == null) {
                    reply(sender, "tp.invalid-coords", FeedbackPriority.HIGH, UiSoundMode.ERROR);
                    return;
                }
                pitch = Math.max(-90f, Math.min(90f, parsedPitch.floatValue()));
            }

            Location target = new Location(origin.getWorld(), x, y, z, yaw, pitch);
            String xLabel = format(x);
            String yLabel = format(y);
            String zLabel = format(z);
            traveller.teleportAsync(target, PlayerTeleportEvent.TeleportCause.COMMAND).thenAccept(success -> {
                if (!success) {
                    reply(sender, "tp.failed", FeedbackPriority.HIGH, UiSoundMode.ERROR);
                    return;
                }
                if (sender.equals(traveller)) {
                    reply(sender, "tp.coords", FeedbackPriority.NORMAL, UiSoundMode.SUCCESS,
                            "x", xLabel, "y", yLabel, "z", zLabel);
                } else {
                    reply(sender, "tp.coords-other", FeedbackPriority.NORMAL, UiSoundMode.SUCCESS,
                            "player", traveller.getName(), "x", xLabel, "y", yLabel, "z", zLabel);
                    notify(traveller, "tp.moved-coords",
                            "player", sender.getName(), "x", xLabel, "y", yLabel, "z", zLabel);
                }
            });
        }, null);
        return true;
    }

    /** {@code /tp <próprio nick>}: centro do bloco (X/Z), sem mudar altura nem olhar. */
    private void centerOnBlock(Player player) {
        player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) {
                return;
            }
            Location origin = player.getLocation();
            int blockX = origin.getBlockX();
            int blockY = origin.getBlockY();
            int blockZ = origin.getBlockZ();
            Location target = new Location(
                    origin.getWorld(),
                    blockX + 0.5,
                    origin.getY(),
                    blockZ + 0.5,
                    origin.getYaw(),
                    origin.getPitch());
            player.teleportAsync(target, PlayerTeleportEvent.TeleportCause.COMMAND).thenAccept(success -> {
                if (!success) {
                    reply(player, "tp.failed", FeedbackPriority.HIGH, UiSoundMode.ERROR);
                    return;
                }
                reply(player, "tp.centered", FeedbackPriority.NORMAL, UiSoundMode.SUCCESS,
                        "x", Integer.toString(blockX), "y", Integer.toString(blockY), "z", Integer.toString(blockZ));
            });
        }, null);
    }

    /** Número absoluto, {@code ~} (mesma posição) ou {@code ~+2}/{@code ~-1.5}. Vírgula vira ponto. */
    static Double parseCoord(String token, double origin) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String value = token.replace(',', '.').trim();
        if (value.charAt(0) == '~') {
            if (value.length() == 1) {
                return origin;
            }
            Double delta = parseNumber(value.substring(1));
            return delta == null ? null : origin + delta;
        }
        return parseNumber(value);
    }

    private static Double parseNumber(String value) {
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static boolean isCoordinate(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        char first = token.charAt(0);
        return first == '~' || first == '-' || first == '+' || first == '.' || (first >= '0' && first <= '9');
    }

    private static String format(double value) {
        if (Math.rint(value) == value && Math.abs(value) < 1.0E7) {
            return Long.toString((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private boolean tphere(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            reply(sender, "commands.player-only", FeedbackPriority.HIGH, UiSoundMode.ERROR);
            return true;
        }
        if (!player.hasPermission(HERE_PERMISSION)) {
            deny(player);
            return true;
        }
        if (args.length != 1) {
            reply(sender, "tphere.usage", FeedbackPriority.HIGH, UiSoundMode.ERROR);
            return true;
        }
        Player traveller = CommandPlayers.resolve(args[0]);
        if (traveller == null) {
            missing(sender, args[0]);
            return true;
        }
        if (traveller.getUniqueId().equals(player.getUniqueId())) {
            reply(sender, "tphere.self", FeedbackPriority.HIGH, UiSoundMode.ERROR);
            return true;
        }
        teleport(sender, traveller, player, "tphere.executor", "tphere.target");
        return true;
    }

    private void teleport(
            CommandSender sender,
            Player traveller,
            Player destination,
            String executorPath,
            String travellerPath) {
        destination.getScheduler().run(plugin, task -> {
            if (!destination.isOnline() || !traveller.isOnline()) {
                reply(sender, "tp.offline", FeedbackPriority.HIGH, UiSoundMode.ERROR);
                return;
            }
            Location target = destination.getLocation().clone();
            traveller.getScheduler().run(plugin, travel -> {
                if (!traveller.isOnline() || !destination.isOnline()) {
                    reply(sender, "tp.offline", FeedbackPriority.HIGH, UiSoundMode.ERROR);
                    return;
                }
                traveller.teleportAsync(target, PlayerTeleportEvent.TeleportCause.COMMAND).thenAccept(success -> {
                    if (!success) {
                        reply(sender, "tp.failed", FeedbackPriority.HIGH, UiSoundMode.ERROR);
                        return;
                    }
                    reply(sender, executorPath, FeedbackPriority.NORMAL, UiSoundMode.SUCCESS,
                            "player", traveller.getName(), "target", destination.getName());
                    if (!sender.equals(traveller)) {
                        notify(traveller, travellerPath, "player", sender.getName(), "target", destination.getName());
                    }
                });
            }, null);
        }, null);
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (here) {
            if (args.length != 1 || !sender.hasPermission(HERE_PERMISSION)) {
                return List.of();
            }
            return PlayerNameTabComplete.onlineNames(args, 0);
        }
        if (!sender.hasPermission(TP_PERMISSION) && !sender.hasPermission(OTHERS_PERMISSION)) {
            return List.of();
        }
        if (inCoordinateArgs(args)) {
            String current = args[args.length - 1];
            return "~".startsWith(current) ? List.of("~") : List.of();
        }
        if (args.length == 1) {
            return PlayerNameTabComplete.onlineNames(args, 0);
        }
        if (args.length == 2 && sender.hasPermission(OTHERS_PERMISSION)) {
            return PlayerNameTabComplete.onlineNames(args, 1);
        }
        return List.of();
    }

    private static boolean inCoordinateArgs(String[] args) {
        if (args.length == 0) {
            return false;
        }
        if (isCoordinate(args[0])) {
            return true;
        }
        return args.length >= 2 && isCoordinate(args[1]);
    }

    private void missing(CommandSender sender, String name) {
        reply(sender, "commands.player-not-found", FeedbackPriority.HIGH, UiSoundMode.ERROR, "player", name);
    }

    private void deny(CommandSender sender) {
        reply(sender, "commands.no-permission", FeedbackPriority.HIGH, UiSoundMode.ERROR);
    }

    private void notify(Player player, String path, String... placeholders) {
        feedback.send(player, messages.component(path, placeholders), FeedbackPriority.NORMAL, true, UiSoundMode.SUCCESS);
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
