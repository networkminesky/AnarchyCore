package anarchy.minesky.core.impl.profile;

import anarchy.minesky.core.impl.stats.PlayerStatsRecord;
import anarchy.minesky.core.impl.stats.PlayerStatsSeed;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Lê {@code <mundo>/stats/<uuid>.json} quando o cache MariaDB não existe.
 */
public final class VanillaStatsFileReader {

    private VanillaStatsFileReader() {}

    public static Optional<PlayerStatsRecord> read(Plugin plugin, UUID uuid, String playerName) {
        File file = locateFile(plugin, uuid);
        if (file == null) {
            return Optional.empty();
        }
        try {
            String raw = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            if (raw.isBlank()) {
                return Optional.empty();
            }
            JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
            JsonObject statsRoot = root.getAsJsonObject("stats");
            if (statsRoot == null) {
                return Optional.empty();
            }
            JsonObject custom = statsRoot.getAsJsonObject("minecraft:custom");
            int mobKills = readInt(custom, "minecraft:mob_kills");
            long playtimeSeconds = PlayerStatsSeed.ticksToSeconds(readInt(custom, "minecraft:play_time"));
            int blocksBroken = sumCategory(statsRoot, "minecraft:mined");
            int blocksPlaced = sumUsedBlocks(statsRoot);
            String name = playerName != null ? playerName : uuid.toString();
            return Optional.of(new PlayerStatsRecord(
                    uuid,
                    name,
                    0,
                    0,
                    playtimeSeconds,
                    blocksPlaced,
                    blocksBroken,
                    mobKills));
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static File locateFile(Plugin plugin, UUID uuid) {
        String fileName = uuid + ".json";
        File container = plugin.getServer().getWorldContainer();
        File preferred = new File(container, "world/stats/" + fileName);
        if (preferred.isFile()) {
            return preferred;
        }
        File[] dirs = container.listFiles(File::isDirectory);
        if (dirs == null) {
            return null;
        }
        for (File world : dirs) {
            File candidate = new File(world, "stats/" + fileName);
            if (candidate.isFile()) {
                return candidate;
            }
        }
        return null;
    }

    private static int readInt(JsonObject object, String key) {
        if (object == null) {
            return 0;
        }
        JsonElement el = object.get(key);
        if (el == null || !el.isJsonPrimitive()) {
            return 0;
        }
        try {
            return el.getAsInt();
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int sumCategory(JsonObject statsRoot, String category) {
        JsonObject section = statsRoot.getAsJsonObject(category);
        if (section == null) {
            return 0;
        }
        int total = 0;
        for (Map.Entry<String, JsonElement> entry : section.entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) {
                continue;
            }
            try {
                total += entry.getValue().getAsInt();
            } catch (NumberFormatException ignored) {
            }
        }
        return total;
    }

    private static int sumUsedBlocks(JsonObject statsRoot) {
        JsonObject used = statsRoot.getAsJsonObject("minecraft:used");
        if (used == null) {
            return 0;
        }
        int total = 0;
        for (Map.Entry<String, JsonElement> entry : used.entrySet()) {
            Material material = materialKey(entry.getKey());
            if (material == null || material.isLegacy() || !material.isBlock()) {
                continue;
            }
            try {
                total += entry.getValue().getAsInt();
            } catch (NumberFormatException ignored) {
            }
        }
        return total;
    }

    private static Material materialKey(String namespaced) {
        if (namespaced == null) {
            return null;
        }
        String id = namespaced;
        int colon = namespaced.indexOf(':');
        if (colon >= 0) {
            id = namespaced.substring(colon + 1);
        }
        return Material.matchMaterial(id.toUpperCase(Locale.ROOT));
    }
}