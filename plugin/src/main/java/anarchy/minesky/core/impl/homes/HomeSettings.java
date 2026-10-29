package anarchy.minesky.core.impl.homes;

import anarchy.minesky.core.impl.sounds.CoreSounds;
import anarchy.minesky.core.impl.warps.WarpStorage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public final class HomeSettings {

    public static final String BYPASS_TIMER_PERMISSION = "anarchycore.home.bypasstimer";
    public static final String OTHERS_PERMISSION = "anarchycore.home.others";
    public static final String ICON_PERMISSION = "anarchycore.home.icon";

    public record LimitGroup(String permission, int limit) {
    }

    private final Plugin plugin;

    private int defaultLimit = 1;
    private int emptySlotsPreview = 4;
    private int countdownSeconds = 5;
    private double moveThreshold = 0.5;
    private float soundVolume = 0.8f;
    private float soundPitch = 1.0f;
    private Sound countdownTick = Sound.BLOCK_NOTE_BLOCK_HAT;
    private Material listHeaderIcon = Material.BLUE_BED;
    private Material listIcon = Material.BLUE_BED;
    private List<LimitGroup> groups = List.of();

    public HomeSettings(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        defaultLimit = Math.max(0, config.getInt("homes.default-limit", 1));
        emptySlotsPreview = Math.max(1, config.getInt("homes.empty-slots-preview", 4));
        countdownSeconds = Math.max(0, config.getInt("homes.countdown-seconds", 5));
        moveThreshold = Math.max(0.01, config.getDouble("homes.move-threshold", 0.5));
        soundVolume = (float) config.getDouble("homes.sounds.volume", 0.8);
        soundPitch = (float) config.getDouble("homes.sounds.pitch", 1.0);
        countdownTick = CoreSounds.parseSound(config.getString("homes.sounds.countdown-tick"), countdownTick);
        listHeaderIcon = WarpStorage.parseMaterial(config.getString("homes.list-header-icon"), Material.BLUE_BED);
        listIcon = WarpStorage.parseMaterial(
                config.getString("homes.default-home-icon", config.getString("homes.list-icon", "BLUE_BED")),
                Material.BLUE_BED);

        List<LimitGroup> loaded = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("homes.groups");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection group = section.getConfigurationSection(key);
                if (group == null) {
                    continue;
                }
                String permission = group.getString("permission", "anarchycore.home.group." + key);
                int limit = Math.max(0, group.getInt("limit", 1));
                loaded.add(new LimitGroup(permission, limit));
            }
        }
        groups = List.copyOf(loaded);
    }

    public int resolveLimit(Player player) {
        int limit = defaultLimit;
        for (LimitGroup group : groups) {
            if (player.hasPermission(group.permission())) {
                limit = Math.max(limit, group.limit());
            }
        }
        return limit;
    }

    public int defaultLimit() {
        return defaultLimit;
    }

    public int emptySlotsPreview() {
        return emptySlotsPreview;
    }

    public int countdownSeconds() {
        return countdownSeconds;
    }

    public double moveThreshold() {
        return moveThreshold;
    }

    public float soundVolume() {
        return soundVolume;
    }

    public float soundPitch() {
        return soundPitch;
    }

    public Sound countdownTick() {
        return countdownTick;
    }

    public Material listHeaderIcon() {
        return listHeaderIcon;
    }

    public Material listIcon() {
        return listIcon;
    }

    /** Ícone padrão ao criar home (config {@code homes.default-home-icon}). */
    public Material defaultHomeIcon() {
        return listIcon;
    }
}
