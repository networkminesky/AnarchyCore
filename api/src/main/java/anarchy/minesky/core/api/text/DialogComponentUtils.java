package anarchy.minesky.core.api.text;

import anarchy.minesky.core.api.utils.HeadURLResolver;

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
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DialogComponentUtils {

    private static final Pattern HEX_AMPERSAND = Pattern.compile("&#([A-Fa-f0-9]{6})");
    /** Formato legacy de cor hex: &x&r&r&g&g&b&b */
    private static final Pattern LEGACY_HEX = Pattern.compile("&x(&[0-9a-fA-F]){6}");
    private static final Pattern HEAD_PLACEHOLDER = Pattern.compile("\\{head:([a-zA-Z0-9_\\-]+)}");
    private static final Pattern CHAT_URL = Pattern.compile("(https?://\\S+|www\\.\\S+)", Pattern.CASE_INSENSITIVE);

    /**
     * Tabela de texturas vanilla de blocos que não usam diretamente a chave do Material.
     */
    private static final Map<String, String> SPRITE_OVERRIDES = Map.ofEntries(
            Map.entry("big_dripleaf", "blocks:block/big_dripleaf_top"),
            Map.entry("small_dripleaf", "blocks:block/small_dripleaf_top"),
            Map.entry("grass_block", "blocks:block/grass_block_side"),
            Map.entry("podzol", "blocks:block/podzol_side"),
            Map.entry("mycelium", "blocks:block/mycelium_side"),
            Map.entry("dirt_path", "blocks:block/dirt_path_side"),
            Map.entry("cactus", "blocks:block/cactus_side"),
            Map.entry("crafting_table", "blocks:block/crafting_table_front"),
            Map.entry("furnace", "blocks:block/furnace_front"),
            Map.entry("blast_furnace", "blocks:block/blast_furnace_front"),
            Map.entry("smoker", "blocks:block/smoker_front"),
            Map.entry("dispenser", "blocks:block/dispenser_front"),
            Map.entry("dropper", "blocks:block/dropper_front"),
            Map.entry("observer", "blocks:block/observer_front"),
            Map.entry("tnt", "blocks:block/tnt_side"),
            Map.entry("scaffolding", "blocks:block/scaffolding_top"),
            Map.entry("ancient_debris", "blocks:block/ancient_debris_side"),
            Map.entry("basalt", "blocks:block/basalt_side"),
            Map.entry("polished_basalt", "blocks:block/polished_basalt_side"),
            Map.entry("hay_block", "blocks:block/hay_block_side"),
            Map.entry("bone_block", "blocks:block/bone_block_side"),
            Map.entry("pumpkin", "blocks:block/pumpkin_side"),
            Map.entry("melon", "blocks:block/melon_side"),
            Map.entry("daylight_detector", "blocks:block/daylight_detector_top"),
            Map.entry("respawn_anchor", "blocks:block/respawn_anchor_top"),
            Map.entry("lodestone", "blocks:block/lodestone_top"),
            Map.entry("target", "blocks:block/target_top"),
            Map.entry("bee_nest", "blocks:block/bee_nest_front"),
            Map.entry("beehive", "blocks:block/beehive_front"),
            Map.entry("stonecutter", "blocks:block/stonecutter_side"),
            Map.entry("cartography_table", "blocks:block/cartography_table_top"),
            Map.entry("fletching_table", "blocks:block/fletching_table_top"),
            Map.entry("smithing_table", "blocks:block/smithing_table_top"),
            Map.entry("enchanting_table", "blocks:block/enchanting_table_top"),
            Map.entry("loom", "blocks:block/loom_front"),
            Map.entry("lectern", "blocks:block/lectern_top"),
            Map.entry("chiseled_bookshelf", "blocks:block/chiseled_bookshelf_empty"),
            Map.entry("composter", "blocks:block/composter_side"),
            Map.entry("barrel", "blocks:block/barrel_side"),
            Map.entry("piston", "blocks:block/piston_top"),
            Map.entry("sticky_piston", "blocks:block/piston_top_sticky"),
            Map.entry("sniffer_egg", "blocks:block/sniffer_egg_not_cracked_top"),
            Map.entry("crafter", "blocks:block/crafter_top"),
            Map.entry("vault", "blocks:block/vault_front_off"),
            Map.entry("trial_spawner", "blocks:block/trial_spawner_top_inactive")
    );

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
        if (containsMiniMessage(input)) {
            try {
                return MiniMessage.miniMessage().deserialize(
                        legacyColorsToMini(input),
                        HeadURLResolver.mineSkyMiniMessageTags());
            } catch (Exception e) {
                return Component.text(input);
            }
        }
        if (input.contains("&")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
        }
        if (input.contains("§")) {
            return LegacyComponentSerializer.legacySection().deserialize(input);
        }
        try {
            return MiniMessage.miniMessage().deserialize(input, HeadURLResolver.mineSkyMiniMessageTags());
        } catch (Exception e) {
            return Component.text(input);
        }
    }

    private static boolean containsMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return input.indexOf('<') >= 0 && (
                input.contains("<#")
                        || input.contains("</")
                        || input.contains("<color:")
                        || input.contains("<c:")
                        || input.contains("<head:")
                        || input.contains("<head_url:")
                        || input.contains("<gradient")
                        || input.contains("<rainbow")
                        || input.contains("<!italic>")
                        || input.contains("<italic")
                        || input.contains("<white>")
                        || input.contains("<green>")
                        || input.contains("<gray>")
                        || input.contains("<yellow>")
                        || input.contains("<gold>")
                        || input.contains("<red>")
                        || input.contains("<bold>")
                        || input.contains("<reset>")
                        || HEAD_PLACEHOLDER.matcher(input).find());
    }

    private static String legacyColorsToMini(String input) {
        if (input == null || !input.contains("&")) {
            return input;
        }
        String withHex = convertLegacyHexToMini(input);
        return withHex
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&l", "<bold>")
                .replace("&o", "<italic>")
                .replace("&n", "<underlined>")
                .replace("&m", "<strikethrough>")
                .replace("&k", "<obfuscated>")
                .replace("&r", "<reset>");
    }

    private static String convertLegacyHexToMini(String input) {
        Matcher ampersandMatcher = HEX_AMPERSAND.matcher(input);
        StringBuilder outAmp = new StringBuilder();
        while (ampersandMatcher.find()) {
            ampersandMatcher.appendReplacement(outAmp, "<#" + ampersandMatcher.group(1) + ">");
        }
        ampersandMatcher.appendTail(outAmp);
        String step1 = outAmp.toString();

        Matcher matcher = LEGACY_HEX.matcher(step1);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String legacy = matcher.group();
            StringBuilder hex = new StringBuilder("#");
            for (int i = 2; i < legacy.length(); i += 2) {
                hex.append(legacy.charAt(i + 1));
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement("<" + hex + ">"));
        }
        matcher.appendTail(out);
        return out.toString();
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
     * Sprite de item + texto, sem {@link Material} — seguro no bootstrap Paper.
     *
     * @param itemKey chave do item vanilla (ex. {@code end_crystal})
     */
    public static Component itemSpriteLabel(String itemKey, String text) {
        if (itemKey == null || itemKey.isEmpty()) {
            return Component.text(text != null ? text : "");
        }
        String atlasPath = SPRITE_OVERRIDES.get(itemKey);
        if (atlasPath == null) {
            if (isItemSpriteBlock(itemKey)) {
                atlasPath = "items:item/" + itemKey;
            } else if (itemKey.endsWith("_sapling") || itemKey.endsWith("_leaves") || itemKey.endsWith("_planks") || itemKey.endsWith("_log")) {
                atlasPath = "blocks:block/" + itemKey;
            } else {
                atlasPath = "items:item/" + itemKey;
            }
        }
        Component sprite = deserializeSprite(atlasPath);
        Component label = Component.text(text != null ? text : "");
        if (sprite == null) {
            return label;
        }
        return sprite.append(Component.space()).append(label);
    }

    /**
     * Sprite MiniMessage para ícones de material.
     * Resolve corretamente os atlas de itens e blocos vanilla sem exibir texturas ausentes.
     */
    public static Component materialIconComponent(Material material) {
        if (material == null || material.isAir() || material.isLegacy()) {
            return Component.empty();
        }
        String spritePath = resolveMaterialSpritePath(material);
        Component sprite = deserializeSprite(spritePath);
        return sprite != null ? sprite : Component.empty();
    }

    public static String resolveMaterialSpritePath(Material material) {
        if (material == null || material.isAir() || material.isLegacy()) {
            return "";
        }
        String key;
        try {
            key = material.getKey().getKey();
        } catch (Throwable ignored) {
            key = material.name().toLowerCase(Locale.ROOT);
        }

        // 1. Tabela de override manual de blocos especiais
        String override = SPRITE_OVERRIDES.get(key);
        if (override != null) {
            return override;
        }

        // 2. Itens que não são blocos (espadas, maçãs, etc.)
        if (!material.isBlock()) {
            return "items:item/" + key;
        }

        // 3. Blocos que possuem ícone específico 2D no atlas de itens
        if (isItemSpriteBlock(key)) {
            return "items:item/" + key;
        }

        // 4. Blocos derivados que usam textura do bloco base (escadas, lajes, cercas)
        String derived = resolveDerivedBlockTexture(key);
        if (derived != null) {
            return "blocks:block/" + derived;
        }

        // 5. Blocos comuns (mudas/saplings, pedras, flores, etc. que ficam no atlas de blocos)
        return "blocks:block/" + key;
    }

    private static boolean isItemSpriteBlock(String key) {
        if (key.endsWith("_door")
                || key.endsWith("_sign")
                || key.endsWith("_hanging_sign")
                || key.endsWith("_bed")
                || key.endsWith("_candle")
                || key.endsWith("_shulker_box")) {
            return true;
        }
        return switch (key) {
            case "brewing_stand", "cauldron", "hopper", "armor_stand",
                 "campfire", "soul_campfire", "flower_pot", "repeater",
                 "comparator", "kelp", "sugar_cane", "nether_wart",
                 "bell", "chain", "cake", "lantern", "soul_lantern",
                 "bamboo", "iron_bars", "wheat_seeds", "pumpkin_seeds",
                 "melon_seeds", "beetroot_seeds", "torchflower_seeds",
                 "pitcher_pod" -> true;
            default -> false;
        };
    }

    private static String resolveDerivedBlockTexture(String key) {
        if (key.endsWith("_stairs")) {
            return resolveBaseBlockTexture(key.substring(0, key.length() - "_stairs".length()));
        }
        if (key.endsWith("_slab")) {
            return resolveBaseBlockTexture(key.substring(0, key.length() - "_slab".length()));
        }
        if (key.endsWith("_fence_gate")) {
            return resolveBaseBlockTexture(key.substring(0, key.length() - "_fence_gate".length()));
        }
        if (key.endsWith("_fence")) {
            return resolveBaseBlockTexture(key.substring(0, key.length() - "_fence".length()));
        }
        if (key.endsWith("_wall")) {
            return resolveBaseBlockTexture(key.substring(0, key.length() - "_wall".length()));
        }
        if (key.endsWith("_button")) {
            return resolveBaseBlockTexture(key.substring(0, key.length() - "_button".length()));
        }
        if (key.endsWith("_pressure_plate")) {
            return resolveBaseBlockTexture(key.substring(0, key.length() - "_pressure_plate".length()));
        }
        return null;
    }

    private static String resolveBaseBlockTexture(String base) {
        if (base == null || base.isEmpty()) return "stone";
        return switch (base) {
            case "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
                 "mangrove", "cherry", "bamboo", "crimson", "warped" -> base + "_planks";
            case "stone_brick" -> "stone_bricks";
            case "mossy_stone_brick" -> "mossy_stone_bricks";
            case "nether_brick" -> "nether_bricks";
            case "red_nether_brick" -> "red_nether_bricks";
            case "end_stone_brick" -> "end_stone_bricks";
            case "deepslate_brick" -> "deepslate_bricks";
            case "deepslate_tile" -> "deepslate_tiles";
            case "polished_blackstone_brick" -> "polished_blackstone_bricks";
            case "mud_brick" -> "mud_bricks";
            case "prismarine_brick" -> "prismarine_bricks";
            case "brick" -> "bricks";
            case "quartz" -> "quartz_block_side";
            case "smooth_quartz" -> "quartz_block_bottom";
            case "purpur" -> "purpur_block";
            case "light_weighted" -> "gold_block";
            case "heavy_weighted" -> "iron_block";
            default -> base;
        };
    }

    private static Component deserializeSprite(String atlasPath) {
        if (atlasPath == null || atlasPath.isEmpty()) {
            return null;
        }
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