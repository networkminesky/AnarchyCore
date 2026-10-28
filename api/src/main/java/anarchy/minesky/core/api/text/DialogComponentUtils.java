package anarchy.minesky.core.api.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DialogComponentUtils {

    private static final Pattern HEX_AMPERSAND = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private DialogComponentUtils() {}

    /**
     * Converte {@code &#RRGGBB} (LuckPerms e similares) para o formato legacy {@code &x&r&r&g&g&b&b}.
     */
    public static String normalizeLegacyColors(String input) {
        if (input == null || input.isEmpty()) return input;
        Matcher matcher = HEX_AMPERSAND.matcher(input);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder legacyHex = new StringBuilder("&x");
            for (int i = 0; i < hex.length(); i++) {
                legacyHex.append('&').append(hex.charAt(i));
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(legacyHex.toString()));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) return Component.empty();
        input = normalizeLegacyColors(input);
        if (input.contains("&")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
        }
        if (input.contains("§")) {
            return LegacyComponentSerializer.legacySection().deserialize(input);
        }
        try {
            return MiniMessage.miniMessage().deserialize(input);
        } catch (Exception e) {
            return Component.text(input);
        }
    }

    public static String toBedrock(Component component) {
        if (component == null) return "";
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}