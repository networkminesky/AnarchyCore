package anarchy.minesky.core.impl.feedback;

import anarchy.minesky.core.api.text.DialogComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

final class FeedbackTextSplitter {

    private FeedbackTextSplitter() {}

    static List<Component> split(String raw, int maxVisibleChars) {
        if (raw == null || raw.isBlank()) {
            return List.of(Component.empty());
        }
        String normalized = DialogComponentUtils.normalizeLegacyColors(raw.trim());
        if (visibleLength(normalized) <= maxVisibleChars) {
            return List.of(DialogComponentUtils.parse(normalized));
        }

        List<String> chunks = new ArrayList<>();
        String remaining = normalized;
        while (!remaining.isEmpty()) {
            String chunk = extractChunk(remaining, maxVisibleChars);
            if (chunk.isEmpty()) {
                chunk = remaining.substring(0, Math.min(remaining.length(), maxVisibleChars + 8));
            }
            chunks.add(chunk);
            remaining = remaining.substring(chunk.length());
        }

        return chunks.stream().map(DialogComponentUtils::parse).toList();
    }

    static List<Component> split(Component component, int maxVisibleChars) {
        if (component == null || component.equals(Component.empty())) {
            return List.of(Component.empty());
        }
        if (ComponentLength.visible(component) <= maxVisibleChars) {
            return List.of(component);
        }
        String legacy = LegacyComponentSerializer.legacyAmpersand().serialize(component);
        return split(legacy, maxVisibleChars);
    }

    private static int visibleLength(String legacy) {
        return ComponentLength.visible(DialogComponentUtils.parse(legacy));
    }

    private static String extractChunk(String input, int maxVisible) {
        StringBuilder chunk = new StringBuilder();
        int visible = 0;
        int i = 0;
        int lastSpaceIndex = -1;

        while (i < input.length() && visible < maxVisible) {
            if (input.charAt(i) == '&' && i + 1 < input.length()) {
                if (input.charAt(i + 1) == 'x' && i + 13 < input.length()) {
                    chunk.append(input, i, i + 14);
                    i += 14;
                    continue;
                }
                chunk.append(input, i, i + 2);
                i += 2;
                continue;
            }
            if (input.charAt(i) == '§' && i + 1 < input.length()) {
                chunk.append(input, i, i + 2);
                i += 2;
                continue;
            }
            char c = input.charAt(i);
            if (c == ' ') {
                lastSpaceIndex = chunk.length();
            }
            chunk.append(c);
            visible++;
            i++;
        }

        if (i < input.length() && lastSpaceIndex > 0) {
            chunk.setLength(lastSpaceIndex);
        }

        return chunk.toString();
    }
}
