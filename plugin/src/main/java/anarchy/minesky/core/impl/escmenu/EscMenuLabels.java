package anarchy.minesky.core.impl.escmenu;

import anarchy.minesky.core.api.text.DialogComponentUtils;
import net.kyori.adventure.text.Component;

final class EscMenuLabels {

    private EscMenuLabels() {}

    static Component buttonLabel(String sprite, String labelText) {
        Component parsed = DialogComponentUtils.parse(labelText);
        if (sprite == null || sprite.isBlank()) {
            return parsed;
        }
        Component spriteOnly = DialogComponentUtils.itemSpriteLabel(sprite.trim(), "");
        if (spriteOnly == Component.empty()) {
            return parsed;
        }
        return spriteOnly.append(Component.space()).append(parsed);
    }

    static Component pauseLabel(String sprite, String labelText) {
        String text = labelText != null ? labelText : "";
        if (sprite == null || sprite.isBlank()) {
            return DialogComponentUtils.parse(text);
        }
        return DialogComponentUtils.itemSpriteLabel(sprite.trim(), stripLegacyForSprite(text));
    }

    private static String stripLegacyForSprite(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("&[0-9a-fk-orA-FK-OR]", "").replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
}