package anarchy.minesky.core.impl.guard.fog;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FogSettings {

    public enum Strategy {
        DESTROY,
        TELEPORT
    }

    private final Plugin plugin;
    private boolean enabled = true;
    private Strategy strategy = Strategy.DESTROY;
    private int checkIntervalTicks = 2;
    private double maxDistance = 48.0;
    private double minDistance = 4.0;
    private double anticipation = 1.1;
    private int outOfSightThreshold = 3;
    private double fovCosine = 0.25;
    private double fovHysteresis = 0.08;
    private double velocityExtrapolate = 5.0;
    private boolean nametagEnabled = true;
    private boolean locatorWhenStealthed = true;
    private int nametagMoveIntervalTicks = 4;
    private double nametagMoveEpsilonSq = 0.04;
    private String bypassPermission = "anarchycore.guard.fog.bypass";
    private Set<String> disabledWorlds = Set.of();
    private List<String> vanishMetadataKeys = List.of("vanished", "vanish");

    public FogSettings(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "guard.yml");
        if (!file.exists()) {
            plugin.saveResource("guard.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        var section = config.getConfigurationSection("fog-of-war");
        if (section == null) {
            enabled = true;
            return;
        }
        enabled = section.getBoolean("enabled", true);
        try {
            strategy = Strategy.valueOf(section.getString("strategy", "DESTROY").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            strategy = Strategy.DESTROY;
        }
        checkIntervalTicks = Math.max(1, section.getInt("check-interval-ticks", 2));
        maxDistance = section.getDouble("visibility.max-distance", 48.0);
        minDistance = section.getDouble("visibility.min-distance", 4.0);
        anticipation = section.getDouble("visibility.anticipation", 1.1);
        outOfSightThreshold = Math.max(1, section.getInt("out-of-sight.threshold", 3));
        fovCosine = section.getDouble("out-of-sight.fov-cosine", 0.25);
        fovHysteresis = Math.max(0.0, section.getDouble("out-of-sight.fov-hysteresis", 0.08));
        velocityExtrapolate = section.getDouble("visibility.velocity-ticks", 5.0);
        nametagEnabled = section.getBoolean("nametag.enabled", true);
        locatorWhenStealthed = section.getBoolean("locator-bar.hide-when-stealthed", true);
        nametagMoveIntervalTicks = Math.max(1, section.getInt("nametag.move-interval-ticks", 4));
        double epsilon = section.getDouble("nametag.move-epsilon", 0.2);
        nametagMoveEpsilonSq = epsilon * epsilon;
        bypassPermission = section.getString("permission-bypass", "anarchycore.guard.fog.bypass");
        disabledWorlds = new HashSet<>(section.getStringList("disabled-worlds"));
        List<String> keys = section.getStringList("vanish.metadata-keys");
        vanishMetadataKeys = keys.isEmpty() ? List.of("vanished", "vanish") : List.copyOf(keys);
    }

    public boolean enabled() {
        return enabled;
    }

    public Strategy strategy() {
        return strategy;
    }

    public int checkIntervalTicks() {
        return checkIntervalTicks;
    }

    public double maxDistance() {
        return maxDistance;
    }

    public double maxDistanceSq() {
        return maxDistance * maxDistance;
    }

    public double minDistance() {
        return minDistance;
    }

    public double minDistanceSq() {
        return minDistance * minDistance;
    }

    public double anticipation() {
        return anticipation;
    }

    public int outOfSightThreshold() {
        return outOfSightThreshold;
    }

    public double fovCosine() {
        return fovCosine;
    }

    public double fovHysteresis() {
        return fovHysteresis;
    }

    public double velocityExtrapolate() {
        return velocityExtrapolate;
    }

    public boolean nametagEnabled() {
        return nametagEnabled;
    }

    public boolean locatorWhenStealthed() {
        return locatorWhenStealthed;
    }

    public int nametagMoveIntervalTicks() {
        return nametagMoveIntervalTicks;
    }

    public double nametagMoveEpsilonSq() {
        return nametagMoveEpsilonSq;
    }

    public String bypassPermission() {
        return bypassPermission;
    }

    public boolean worldDisabled(String worldName) {
        return worldName != null && disabledWorlds.contains(worldName);
    }

    public boolean isVanished(org.bukkit.entity.Player player) {
        if (player == null) {
            return false;
        }
        for (String key : vanishMetadataKeys) {
            if (player.hasMetadata(key)) {
                return true;
            }
        }
        return false;
    }
}
