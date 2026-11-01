package anarchy.minesky.core.api.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DialogComponentUtils {

    private static final Pattern HEX_AMPERSAND = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern CHAT_URL = Pattern.compile("(https?://\\S+|www\\.\\S+)", Pattern.CASE_INSENSITIVE);

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

    /**
     * Torna URLs detectadas no texto clicáveis ({@link ClickEvent.Action#OPEN_URL}).
     */
    public static Component applyClickableUrls(Component component) {
        if (component == null || component.equals(Component.empty())) {
            return component == null ? Component.empty() : component;
        }
        return component.replaceText(
                TextReplacementConfig.builder()
                        .match(CHAT_URL)
                        .replacement((match, input) -> linkForUrlMatch(match.group(), input))
                        .build());
    }

    private static Component linkForUrlMatch(String matched, TextComponent.Builder input) {
        UrlSlice slice = trimUrlPunctuation(matched);
        Style style = input.build().style();
        if (slice.url().isEmpty()) {
            return Component.text(matched, style);
        }
        String href = slice.url().regionMatches(true, 0, "www.", 0, 4)
                ? "https://" + slice.url()
                : slice.url();
        if (!isAllowedChatUrl(href)) {
            return Component.text(matched, style);
        }
        Component linked = Component.text(slice.display(), style
                        .colorIfAbsent(NamedTextColor.AQUA)
                        .decorationIfAbsent(TextDecoration.UNDERLINED, TextDecoration.State.TRUE))
                .clickEvent(ClickEvent.openUrl(href));
        if (slice.leading().isEmpty() && slice.trailing().isEmpty()) {
            return linked;
        }
        return Component.text(slice.leading(), style)
                .append(linked)
                .append(Component.text(slice.trailing(), style));
    }

    private static UrlSlice trimUrlPunctuation(String matched) {
        int start = 0;
        int end = matched.length();
        while (end > start && isTrailingUrlPunctuation(matched.charAt(end - 1))) {
            end--;
        }
        return new UrlSlice(matched.substring(0, start), matched.substring(start, end), matched.substring(end));
    }

    private static boolean isTrailingUrlPunctuation(char c) {
        return c == '.' || c == ',' || c == ';' || c == ':' || c == '!' || c == '?' || c == ')'
                || c == ']' || c == '}' || c == '"';
    }

    private static boolean isAllowedChatUrl(String href) {
        try {
            URI uri = URI.create(href);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return false;
            }
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private record UrlSlice(String leading, String url, String trailing) {
        String display() {
            return url;
        }
    }

    /**
     * Sprite de item + texto, sem {@link Material} — seguro no bootstrap Paper (antes dos registries Bukkit).
     *
     * @param itemKey chave do item vanilla (ex. {@code end_crystal})
     */
    public static Component itemSpriteLabel(String itemKey, String text) {
        Component sprite = deserializeSprite("items:item/" + itemKey);
        Component label = Component.text(text);
        if (sprite == null) {
            return label;
        }
        return sprite.append(Component.space()).append(label);
    }

    /**
     * Sprite MiniMessage para ícones de material.
     * Escadas, cercas, placas etc. existem no atlas de {@code items}; usar {@code blocks} só quando não há item.
     */
    public static Component materialIconComponent(Material material) {
        if (material == null || material.isAir()) {
            return Component.empty();
        }
        String key = material.getKey().getKey();
        if (material.isItem()) {
            Component itemSprite = deserializeSprite("items:item/" + key);
            if (itemSprite != null) {
                return itemSprite;
            }
        }
        if (material.isBlock()) {
            Component blockSprite = deserializeSprite("blocks:block/" + key);
            if (blockSprite != null) {
                return blockSprite;
            }
        }
        return deserializeSprite("items:item/" + key);
    }

    private static Component deserializeSprite(String atlasPath) {
        try {
            return MiniMessage.miniMessage().deserialize("<sprite:" + atlasPath + ">");
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String toBedrock(Component component) {
        if (component == null) return "";
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}