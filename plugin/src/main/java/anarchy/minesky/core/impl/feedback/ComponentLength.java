package anarchy.minesky.core.impl.feedback;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

final class ComponentLength {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private ComponentLength() {}

    static int visible(Component component) {
        if (component == null) {
            return 0;
        }
        return PLAIN.serialize(component).length();
    }
}
