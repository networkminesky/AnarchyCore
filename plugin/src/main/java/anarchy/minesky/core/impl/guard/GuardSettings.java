package anarchy.minesky.core.impl.guard;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GuardSettings {

    private final Plugin plugin;
    private volatile boolean enabled = true;
    private boolean debugConsole = false;
    private boolean instantPacketBurst = true;
    private boolean acceptTranslationKeyEcho = true;
    private int joinDelayTicks = 40;
    private int linesPerSign = 4;
    private int openDelayTicks = 2;
    private int closeDelayTicks = 4;
    private int responseTimeoutTicks = 60;
    private int virtualSignYOffset = 3;
    private String bypassPermission = "anarchycore.guard.bypass";
    private String punishmentReason = "Cliente/mod proibido detectado ({probe})";
    private String kickMessage = "&c{reason}";
    private List<GuardPunishmentStep> punishmentSteps = List.of();
    private Map<String, GuardProbe> probes = Map.of();
    private int maxHistoryEntries = 100;

    public GuardSettings(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "guard.yml");
        if (!file.exists()) {
            plugin.saveResource("guard.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        enabled = config.getBoolean("enabled", true);
        debugConsole = config.getBoolean("debug", false);
        instantPacketBurst = config.getBoolean("instant-packet-burst", true);
        acceptTranslationKeyEcho = config.getBoolean("accept-translation-key-echo", true);
        joinDelayTicks = config.getInt("join-delay-ticks", 40);
        linesPerSign = Math.clamp(config.getInt("lines-per-sign", 4), 1, 4);
        openDelayTicks = config.getInt("open-delay-ticks", 2);
        closeDelayTicks = config.getInt("close-delay-ticks", 4);
        responseTimeoutTicks = config.getInt("response-timeout-ticks", 60);
        virtualSignYOffset = config.getInt("virtual-sign-y-offset", -5);
        bypassPermission = config.getString("permission-bypass", "anarchycore.guard.bypass");
        maxHistoryEntries = config.getInt("history.max-entries-per-player", 100);

        ConfigurationSection punishSection = config.getConfigurationSection("punishments");
        if (punishSection != null) {
            punishmentReason = punishSection.getString("reason", punishmentReason);
            kickMessage = punishSection.getString("kick-message", kickMessage);
            punishmentSteps = loadSteps(punishSection.getConfigurationSection("steps"));
        } else {
            punishmentSteps = List.of(GuardPunishmentStep.kick());
        }

        probes = loadProbes(config.getConfigurationSection("probes"));
    }

    private static Map<String, GuardProbe> loadProbes(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, GuardProbe> map = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection probeSection = section.getConfigurationSection(id);
            if (probeSection == null) {
                continue;
            }
            if (!probeSection.getBoolean("enabled", true)) {
                continue;
            }
            String name = probeSection.getString("name", id);
            String key = probeSection.getString("translation-key");
            if (key == null || key.isBlank()) {
                continue;
            }
            String fallback = probeSection.getString("fallback");
            if (fallback == null || fallback.isBlank()) {
                fallback = "acguard:" + id;
            }
            map.put(id, new GuardProbe(id, name, key.trim(), fallback));
        }
        return Collections.unmodifiableMap(map);
    }

    private static List<GuardPunishmentStep> loadSteps(ConfigurationSection section) {
        if (section == null) {
            return List.of(GuardPunishmentStep.kick());
        }
        List<GuardPunishmentStep> steps = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection stepSection = section.getConfigurationSection(key);
            if (stepSection == null) {
                continue;
            }
            String type = stepSection.getString("type", "kick");
            if ("console".equalsIgnoreCase(type)) {
                List<String> commands = stepSection.getStringList("commands");
                steps.add(GuardPunishmentStep.console(commands));
            } else {
                steps.add(GuardPunishmentStep.kick());
            }
        }
        if (steps.isEmpty()) {
            steps.add(GuardPunishmentStep.kick());
        }
        return List.copyOf(steps);
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean debugConsole() {
        return debugConsole;
    }

    public boolean instantPacketBurst() {
        return instantPacketBurst;
    }

    public boolean acceptTranslationKeyEcho() {
        return acceptTranslationKeyEcho;
    }

    public int joinDelayTicks() {
        return joinDelayTicks;
    }

    public int linesPerSign() {
        return linesPerSign;
    }

    public int openDelayTicks() {
        return openDelayTicks;
    }

    public int closeDelayTicks() {
        return closeDelayTicks;
    }

    public int responseTimeoutTicks() {
        return responseTimeoutTicks;
    }

    public int virtualSignYOffset() {
        return virtualSignYOffset;
    }

    public String bypassPermission() {
        return bypassPermission;
    }

    public String punishmentReason() {
        return punishmentReason;
    }

    public String kickMessage() {
        return kickMessage;
    }

    public List<GuardPunishmentStep> punishmentSteps() {
        return punishmentSteps;
    }

    public Map<String, GuardProbe> probes() {
        return probes;
    }

    public int maxHistoryEntries() {
        return maxHistoryEntries;
    }
}
