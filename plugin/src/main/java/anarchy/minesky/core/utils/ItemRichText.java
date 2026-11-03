package anarchy.minesky.core.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * Texto de item (nome/lore): o cliente aplica itálico por padrão em componentes customizados.
 */
public final class ItemRichText {

    private ItemRichText() {}

    public static Component parse(String input) {
        return withoutDefaultItalic(RichMessageParser.parseRichText(input));
    }

    private static Component withoutDefaultItalic(Component component) {
        Style style = component.style();
        if (style.decoration(TextDecoration.ITALIC) != TextDecoration.State.TRUE) {
            style = style.toBuilder().decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE).build();
        }

        List<Component> children = component.children();
        if (children.isEmpty()) {
            return component.style(style);
        }

        List<Component> mapped = new ArrayList<>(children.size());
        for (Component child : children) {
            mapped.add(withoutDefaultItalic(child));
        }
        return component.style(style).children(mapped);
    }
}
