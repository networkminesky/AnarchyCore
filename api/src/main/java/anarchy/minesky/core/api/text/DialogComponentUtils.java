package anarchy.minesky.core.api.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;

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