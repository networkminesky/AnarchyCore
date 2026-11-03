package anarchy.minesky.core.utils;

import anarchy.minesky.core.api.text.DialogComponentUtils;
import anarchy.minesky.core.api.utils.HeadURLResolver;
import anarchy.minesky.core.listener.ChatAntifloodListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Formata texto de chat/broadcast com legacy (&), MiniMessage e {@link HeadURLResolver#headUrlTag()}.
 */
public final class RichMessageParser {

    private RichMessageParser() {}

    public static Component format(CommandSender sender, String rawMessage) {
        boolean colors = hasColorPermission(sender);
        boolean urls = hasUrlPermission(sender);
        if (!colors && !urls) {
            return Component.text(rawMessage);
        }
        Component body = colors ? parseColored(rawMessage) : Component.text(rawMessage);
        if (urls) {
            body = DialogComponentUtils.applyClickableUrls(body);
        }
        return body;
    }

    public static boolean hasColorPermission(CommandSender sender) {
        return !(sender instanceof Player player) || player.hasPermission(ChatAntifloodListener.COLOR_PERMISSION);
    }

    public static boolean hasUrlPermission(CommandSender sender) {
        return !(sender instanceof Player player) || player.hasPermission(ChatAntifloodListener.URL_PERMISSION);
    }

    /** Parseia {@code &}, {@code &#RRGGBB}, {@code §} e MiniMessage (incl. {@code <head_url:...>}). */
    public static Component parseRichText(String input) {
        return parseColored(input);
    }

    private static Component parseColored(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        input = DialogComponentUtils.normalizeLegacyColors(input);
        if (input.indexOf('<') >= 0) {
            return HeadURLResolver.deserializeMiniMessage(input);
        }
        if (input.contains("&")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
        }
        if (input.contains("§")) {
            return LegacyComponentSerializer.legacySection().deserialize(input);
        }
        return HeadURLResolver.deserializeMiniMessage(input);
    }
}
