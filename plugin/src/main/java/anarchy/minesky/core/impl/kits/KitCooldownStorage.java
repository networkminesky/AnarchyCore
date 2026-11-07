package anarchy.minesky.core.impl.kits;

import anarchy.minesky.core.api.playerdata.PlayerDataAPI;
import anarchy.minesky.core.api.playerdata.PlayerDataResult;
import anarchy.minesky.core.api.playerdata.PlayerDataSnapshot;
import anarchy.minesky.core.api.playerdata.PlayerDataUpdates;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cooldowns de resgate de kit em {@code ac_player_data.data} → {@code kits.claims.<kitId>} (epoch ms).
 * Cache por sessão; persistência assíncrona via {@link PlayerDataAPI}.
 */
public final class KitCooldownStorage {

    static final String CLAIMS_KEY_PREFIX = "kits.claims.";

    private final Plugin plugin;
    private final PlayerDataAPI playerData;
    private final Map<UUID, Map<String, Long>> cache = new ConcurrentHashMap<>();

    public KitCooldownStorage(Plugin plugin, PlayerDataAPI playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
    }

    public void preload(UUID playerId) {
        if (playerId == null || playerData == null || !playerData.isDatabaseActive()) {
            return;
        }
        playerData.getPlayerDataAsync(playerId, result -> {
            if (!(result instanceof PlayerDataResult.Success<PlayerDataSnapshot> success)) {
                return;
            }
            Map<String, Long> claims = parseClaimsRoot(success.value().data());
            cache.put(playerId, new ConcurrentHashMap<>(claims));
        });
    }

    public void unload(UUID playerId) {
        if (playerId != null) {
            cache.remove(playerId);
        }
    }

    public long lastClaimMillis(UUID playerId, String kitId) {
        if (playerId == null || kitId == null) {
            return 0L;
        }
        Map<String, Long> kits = cache.get(playerId);
        if (kits == null) {
            return 0L;
        }
        return kits.getOrDefault(KitStorage.normalizeId(kitId), 0L);
    }

    public void recordClaim(UUID playerId, String kitId) {
        if (playerId == null || kitId == null) {
            return;
        }
        String id = KitStorage.normalizeId(kitId);
        long now = System.currentTimeMillis();
        cache.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>()).put(id, now);

        if (playerData == null || !playerData.isDatabaseActive()) {
            plugin.getLogger().warning(
                    "[Kits] PlayerData indisponível — cooldown de " + id + " não foi persistido para " + playerId);
            return;
        }

        playerData.setPlayerDataAsync(
                playerId,
                PlayerDataUpdates.single(CLAIMS_KEY_PREFIX + id, now),
                result -> {
                    if (!result.isSuccess()) {
                        plugin.getLogger().warning(
                                "[Kits] Falha ao gravar cooldown de " + id + " para " + playerId
                                        + (result.errorOrNull() != null ? ": " + result.errorOrNull() : ""));
                    }
                });
    }

    public int secondsRemaining(UUID playerId, String kitId, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return 0;
        }
        long last = lastClaimMillis(playerId, kitId);
        if (last <= 0L) {
            return 0;
        }
        long elapsedMillis = System.currentTimeMillis() - last;
        long cooldownMillis = cooldownSeconds * 1000L;
        if (elapsedMillis >= cooldownMillis) {
            return 0;
        }
        return (int) Math.ceil((cooldownMillis - elapsedMillis) / 1000.0);
    }

    /**
     * Importa {@code kit-cooldowns.yml} legado para o PlayerData (uma vez) e renomeia o arquivo.
     */
    public void migrateLegacyFile() {
        if (playerData == null || !playerData.isDatabaseActive()) {
            return;
        }
        File file = new File(plugin.getDataFolder(), "kit-cooldowns.yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null || players.getKeys(false).isEmpty()) {
            renameLegacyFile(file);
            return;
        }

        int migratedPlayers = 0;
        for (String uuidKey : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            ConfigurationSection kits = players.getConfigurationSection(uuidKey);
            if (kits == null) {
                continue;
            }
            Map<String, Object> patch = new LinkedHashMap<>();
            for (String kitId : kits.getKeys(false)) {
                long value = kits.getLong(kitId, 0L);
                if (value > 0L) {
                    patch.put(CLAIMS_KEY_PREFIX + KitStorage.normalizeId(kitId), value);
                }
            }
            if (patch.isEmpty()) {
                continue;
            }
            migratedPlayers++;
            playerData.setPlayerDataAsync(uuid, PlayerDataUpdates.of(patch), ignored -> {});
        }

        if (migratedPlayers > 0) {
            plugin.getLogger().info(
                    "[Kits] Migrados cooldowns de " + migratedPlayers + " jogador(es) de kit-cooldowns.yml para PlayerData.");
        }
        renameLegacyFile(file);
    }

    private void renameLegacyFile(File file) {
        File backup = new File(plugin.getDataFolder(), "kit-cooldowns.yml.migrated");
        try {
            Files.move(file.toPath(), backup.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            plugin.getLogger().warning("[Kits] Não foi possível arquivar kit-cooldowns.yml: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Long> parseClaimsRoot(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return Map.of();
        }
        Object kits = data.get("kits");
        if (!(kits instanceof Map<?, ?> kitsMap)) {
            return Map.of();
        }
        Object claims = kitsMap.get("claims");
        if (!(claims instanceof Map<?, ?> claimsMap)) {
            return Map.of();
        }
        Map<String, Long> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : claimsMap.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            Object raw = entry.getValue();
            long millis;
            if (raw instanceof Number number) {
                millis = number.longValue();
            } else {
                try {
                    millis = Long.parseLong(raw.toString());
                } catch (NumberFormatException ignored) {
                    continue;
                }
            }
            if (millis > 0L) {
                out.put(entry.getKey().toString().toLowerCase(), millis);
            }
        }
        return out;
    }
}
