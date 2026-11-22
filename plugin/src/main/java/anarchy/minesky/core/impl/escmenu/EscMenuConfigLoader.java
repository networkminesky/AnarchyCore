package anarchy.minesky.core.impl.escmenu;

import anarchy.minesky.core.api.text.DialogComponentUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class EscMenuConfigLoader {

    private EscMenuConfigLoader() {}

    public static EscMenuDefinition loadFromDataDirectory(Path dataDirectory) throws IOException {
        EscMenuFiles.ensureDefaultFile(dataDirectory);
        YamlConfiguration disk = YamlConfiguration.loadConfiguration(EscMenuFiles.resolve(dataDirectory).toFile());
        try (InputStream defaults = EscMenuFiles.bundledDefaults()) {
            YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaults, StandardCharsets.UTF_8));
            disk.setDefaults(bundled);
            disk.options().copyDefaults(true);
        }
        return parse(disk);
    }

    public static EscMenuDefinition loadMerged(YamlConfiguration disk, YamlConfiguration defaults) {
        if (defaults != null) {
            disk.setDefaults(defaults);
            disk.options().copyDefaults(true);
        }
        return parse(disk);
    }

    public static EscMenuDefinition loadFromResourceOnly() throws IOException {
        try (InputStream defaults = EscMenuFiles.bundledDefaults()) {
            YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaults, StandardCharsets.UTF_8));
            return parse(bundled);
        }
    }

    public static EscMenuDefinition parse(YamlConfiguration yaml) {
        String pauseSprite = yaml.getString("pause.sprite");
        String pauseLabel = yaml.getString("pause.label", "&bMenu do Servidor");
        Component pause = EscMenuLabels.pauseLabel(pauseSprite, pauseLabel);

        String hubTitleRaw = yaml.getString("hub.title", "&fMineSky");
        int columns = clamp(yaml.getInt("hub.columns", 2), 1, 4);
        int buttonWidth = clamp(yaml.getInt("hub.button-width", 160), 80, 400);

        boolean exitEnabled = yaml.getBoolean("hub.exit.enabled", false);
        String exitLabelRaw = yaml.getString("hub.exit.label", "&7Voltar");
        int exitWidth = clamp(yaml.getInt("hub.exit.width", buttonWidth), 80, 400);

        boolean commandEnabled = yaml.getBoolean("command.enabled", true);
        String commandName = yaml.getString("command.name", "menu").trim().toLowerCase(Locale.ROOT);
        List<String> aliases = new ArrayList<>();
        for (String alias : yaml.getStringList("command.aliases")) {
            if (alias != null && !alias.isBlank()) {
                aliases.add(alias.trim().toLowerCase(Locale.ROOT));
            }
        }

        List<Map<?, ?>> rawButtons = yaml.getMapList("buttons");
        if (rawButtons == null || rawButtons.isEmpty()) {
            throw new IllegalArgumentException("esc-menu.yml: lista 'buttons' vazia ou ausente");
        }

        Set<String> seenIds = new HashSet<>();
        List<EscMenuButton> buttons = new ArrayList<>();
        for (int index = 0; index < rawButtons.size(); index++) {
            Map<?, ?> map = rawButtons.get(index);
            if (map == null || map.isEmpty()) {
                throw new IllegalArgumentException("buttons[" + index + "] deve ser um mapa");
            }
            buttons.add(parseButton(map, buttonWidth, index, seenIds));
        }

        return new EscMenuDefinition(
                pause,
                DialogComponentUtils.parse(hubTitleRaw),
                columns,
                buttonWidth,
                exitEnabled,
                exitEnabled ? DialogComponentUtils.parse(exitLabelRaw) : null,
                exitWidth,
                commandEnabled,
                commandName,
                List.copyOf(aliases),
                List.copyOf(buttons),
                parseFlows(yaml));
    }

    private static EscMenuFlows parseFlows(YamlConfiguration yaml) {
        EscMenuFlows defaults = EscMenuFlows.defaults();
        int pageSize = clamp(yaml.getInt("flows.selector.page-size", defaults.selectorPageSize()), 6, 48);
        int selectorWidth = clamp(yaml.getInt("flows.selector.button-width", defaults.selectorButtonWidth()), 80, 400);

        String tpaTo = trimOrDefault(yaml.getString("flows.tpa.command-to"), defaults.tpaCommandTo());
        String tpaHere = trimOrDefault(yaml.getString("flows.tpa.command-here"), defaults.tpaCommandHere());

        double payMin = yaml.getDouble("flows.pay.min", defaults.payMin());
        double payMax = yaml.getDouble("flows.pay.max", defaults.payMax());
        if (payMax < payMin) {
            payMax = payMin;
        }

        List<EscMenuFlows.PayPreset> presets = new ArrayList<>();
        List<Map<?, ?>> rawPresets = yaml.getMapList("flows.pay.presets");
        if (rawPresets == null || rawPresets.isEmpty()) {
            presets.addAll(defaults.payPresets());
        } else {
            for (Map<?, ?> map : rawPresets) {
                Object amountObj = map.get("amount");
                if (amountObj == null) {
                    continue;
                }
                double amount = amountObj instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(amountObj));
                String label = stringValue(map.get("label"));
                if (label == null || label.isBlank()) {
                    label = "&a$" + amount;
                }
                presets.add(new EscMenuFlows.PayPreset(amount, DialogComponentUtils.parse(label)));
            }
        }
        if (presets.isEmpty()) {
            presets.addAll(defaults.payPresets());
        }

        String inputKey = trimOrDefault(yaml.getString("flows.pay.input-key"), defaults.payInputKey());
        String payFallback = trimOrDefault(yaml.getString("flows.pay.fallback-command"), defaults.payFallbackCommand());

        return new EscMenuFlows(
                pageSize,
                selectorWidth,
                tpaTo,
                tpaHere,
                payMin,
                payMax,
                List.copyOf(presets),
                inputKey,
                payFallback);
    }

    private static String trimOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private static EscMenuButton parseButton(
            Map<?, ?> section, int defaultWidth, int index, Set<String> seenIds) {
        String id = EscMenuKeys.normalizeId(stringValue(section.get("id")));
        if (!seenIds.add(id)) {
            throw new IllegalArgumentException("buttons[" + index + "]: id duplicado '" + id + "'");
        }
        String label = stringValue(section.get("label"));
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("buttons[" + index + "]: label ausente");
        }
        String sprite = stringValue(section.get("sprite"));
        EscMenuButtonType type = EscMenuButtonType.parse(stringValue(section.get("type")));
        String command = trimToNull(stringValue(section.get("command")));
        String handler = stringValue(section.get("handler"));
        if (handler == null || handler.isBlank()) {
            handler = id;
        } else {
            handler = handler.trim().toLowerCase(Locale.ROOT);
        }
        String permission = trimToNull(stringValue(section.get("permission")));
        int width = defaultWidth;
        if (section.containsKey("width")) {
            Object rawWidth = section.get("width");
            width = clamp(rawWidth instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(rawWidth)), 80, 400);
        }

        if (type == EscMenuButtonType.COMMAND) {
            if (command == null) {
                throw new IllegalArgumentException("buttons[" + index + "] (" + id + "): COMMAND exige 'command'");
            }
        }

        return new EscMenuButton(
                id,
                EscMenuLabels.buttonLabel(sprite, label),
                trimToNull(sprite),
                type,
                normalizeCommand(command),
                handler,
                permission,
                width);
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private static String normalizeCommand(String command) {
        if (command == null) {
            return null;
        }
        String trimmed = command.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}