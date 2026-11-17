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
            if (chunk.length() >= remaining.length()) {
                break;
            }
            String tail = remaining.substring(chunk.length());
            remaining = legacyStateAtEnd(chunk).toPrefix() + tail;
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

    private static LegacyFormatState legacyStateAtEnd(String legacy) {
        LegacyFormatState state = new LegacyFormatState();
        int i = 0;
        while (i < legacy.length()) {
            if (legacy.charAt(i) == '&' && i + 1 < legacy.length()) {
                if (legacy.charAt(i + 1) == 'x' && i + 13 < legacy.length() && isLegacyHex(legacy, i, '&')) {
                    state.applyColor(legacy.substring(i, i + 14));
                    i += 14;
                    continue;
                }
                char code = legacy.charAt(i + 1);
                if (isColorCode(code)) {
                    state.applyColor("&" + code);
                } else {
                    state.applyDecoration(code);
                }
                i += 2;
                continue;
            }
            if (legacy.charAt(i) == '§' && i + 1 < legacy.length()) {
                if (legacy.charAt(i + 1) == 'x' && i + 13 < legacy.length() && isLegacyHex(legacy, i, '§')) {
                    state.applyColor(sectionHexToAmpersand(legacy.substring(i, i + 14)));
                    i += 14;
                    continue;
                }
                char code = legacy.charAt(i + 1);
                if (isColorCode(code)) {
                    state.applyColor("&" + code);
                } else {
                    state.applyDecoration(code);
                }
                i += 2;
                continue;
            }
            i++;
        }
        return state;
    }

    private static boolean isColorCode(char code) {
        char lower = Character.toLowerCase(code);
        return (lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f');
    }

    private static boolean isLegacyHex(String input, int index, char indicator) {
        if (index + 13 >= input.length() || input.charAt(index + 1) != 'x') {
            return false;
        }
        for (int i = index + 2; i <= index + 13; i += 2) {
            if (input.charAt(i) != indicator) {
                return false;
            }
        }
        return true;
    }

    private static String sectionHexToAmpersand(String sectionHex) {
        StringBuilder out = new StringBuilder("&x");
        for (int i = 3; i < sectionHex.length(); i += 2) {
            out.append('&').append(sectionHex.charAt(i + 1));
        }
        return out.toString();
    }

    private static final class LegacyFormatState {
        private String colorCodes = "";
        private boolean obfuscated;
        private boolean bold;
        private boolean italic;
        private boolean underline;
        private boolean strikethrough;

        void applyColor(String codes) {
            colorCodes = codes;
            obfuscated = false;
            bold = false;
            italic = false;
            underline = false;
            strikethrough = false;
        }

        void applyDecoration(char code) {
            switch (Character.toLowerCase(code)) {
                case 'k' -> obfuscated = true;
                case 'l' -> bold = true;
                case 'm' -> strikethrough = true;
                case 'n' -> underline = true;
                case 'o' -> italic = true;
                case 'r' -> reset();
                default -> { }
            }
        }

        void reset() {
            colorCodes = "";
            obfuscated = false;
            bold = false;
            italic = false;
            underline = false;
            strikethrough = false;
        }

        String toPrefix() {
            if (colorCodes.isEmpty() && !obfuscated && !bold && !italic && !underline && !strikethrough) {
                return "";
            }
            StringBuilder out = new StringBuilder(colorCodes);
            if (obfuscated) {
                out.append("&k");
            }
            if (bold) {
                out.append("&l");
            }
            if (strikethrough) {
                out.append("&m");
            }
            if (underline) {
                out.append("&n");
            }
            if (italic) {
                out.append("&o");
            }
            return out.toString();
        }
    }
}
