package anarchy.minesky.core.impl.profile;

import anarchy.minesky.core.impl.clans.AnarchyClansQueryProvider;
import anarchy.minesky.core.impl.economy.AnarchyEconomyQueryProvider;
import anarchy.minesky.core.impl.stats.PlayerStatsRecord;
import anarchy.minesky.core.impl.stats.PlayerStatsRepository;
import anarchy.minesky.core.impl.stats.PlayerStatsSeed;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class PlayerProfileLoader {

    private final Plugin plugin;
    private final PlayerStatsRepository statsRepository;
    private final AnarchyClansQueryProvider combat;
    private final AnarchyEconomyQueryProvider economy;

    public PlayerProfileLoader(
            Plugin plugin,
            PlayerStatsRepository statsRepository,
            AnarchyClansQueryProvider combat,
            AnarchyEconomyQueryProvider economy) {
        this.plugin = plugin;
        this.statsRepository = statsRepository;
        this.combat = combat;
        this.economy = economy;
    }

    public void loadForViewer(Player viewer, Player target, Consumer<PlayerProfileSnapshot> onReady) {
        if (viewer == null || target == null) {
            return;
        }
        target.getScheduler().run(plugin, task -> {
            if (!target.isOnline()) {
                loadOffline(viewer, target.getUniqueId(), target.getName(), onReady);
                return;
            }
            PlayerStatsRecord progression = PlayerStatsSeed.fromVanillaProgression(target);
            UUID uuid = target.getUniqueId();
            String name = target.getName();
            Bukkit.getAsyncScheduler().runNow(plugin, async -> {
                PlayerStatsRecord merged = mergeCombat(progression, uuid);
                fetchEconomyAndDeliver(viewer, uuid, name, merged, onReady);
            });
        }, null);
    }

    public void loadSelf(Player viewer, Consumer<PlayerProfileSnapshot> onReady) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }
        loadForViewer(viewer, viewer, onReady);
    }

    public void loadOffline(Player viewer, OfflinePlayer target, Consumer<PlayerProfileSnapshot> onReady) {
        if (viewer == null || target == null) {
            return;
        }
        if (target.isOnline() && target.getPlayer() != null) {
            loadForViewer(viewer, target.getPlayer(), onReady);
            return;
        }
        loadOffline(viewer, target.getUniqueId(), target.getName(), onReady);
    }

    public void loadOffline(Player viewer, UUID uuid, String displayName, Consumer<PlayerProfileSnapshot> onReady) {
        if (viewer == null || uuid == null) {
            return;
        }
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            if (!offline.hasPlayedBefore()) {
                Optional<PlayerStatsRecord> file = VanillaStatsFileReader.read(plugin, uuid, displayName);
                boolean dbHit = statsRepository.isActive() && statsRepository.find(uuid).isPresent();
                if (file.isEmpty() && !dbHit && !combat.isActive()) {
                    viewer.getScheduler().run(plugin, entity -> onReady.accept(null), null);
                    return;
                }
            }
            PlayerStatsRecord stats = resolveOfflineStats(uuid, displayName);
            fetchEconomyAndDeliver(
                    viewer, uuid, stats != null ? stats.playerName() : displayName, stats, onReady);
        });
    }

    private PlayerStatsRecord resolveOfflineStats(UUID uuid, String displayName) {
        PlayerStatsRecord base = null;
        if (statsRepository.isActive()) {
            base = statsRepository.find(uuid).orElse(null);
        }
        if (base == null) {
            base = VanillaStatsFileReader.read(plugin, uuid, displayName).orElse(null);
        }
        if (base == null && combat.isActive()) {
            int kills = combat.killsSync(uuid);
            int deaths = combat.deathsSync(uuid);
            if (kills > 0 || deaths > 0) {
                String name = displayName != null ? displayName : uuid.toString();
                base = new PlayerStatsRecord(uuid, name, 0, 0, 0, 0, 0, 0);
            }
        }
        return base != null ? mergeCombat(base, uuid) : null;
    }

    private PlayerStatsRecord mergeCombat(PlayerStatsRecord base, UUID uuid) {
        if (base == null) {
            return null;
        }
        if (!combat.isActive()) {
            return base;
        }
        return base.withCombat(combat.killsSync(uuid), combat.deathsSync(uuid));
    }

    private void fetchEconomyAndDeliver(
            Player viewer,
            UUID uuid,
            String displayName,
            PlayerStatsRecord stats,
            Consumer<PlayerProfileSnapshot> onReady) {
        CompletableFuture<Double> moneyFuture =
                economy.isAvailable() ? economy.balanceMoney(uuid) : CompletableFuture.completedFuture(0.0);
        CompletableFuture<Double> shardsFuture =
                economy.isAvailable() ? economy.balanceShards(uuid) : CompletableFuture.completedFuture(0.0);
        CompletableFuture<Integer> killsFuture = combat.kills(uuid);
        CompletableFuture<Integer> deathsFuture = combat.deaths(uuid);
        CompletableFuture.allOf(moneyFuture, shardsFuture, killsFuture, deathsFuture)
                .thenAccept(ignored -> {
                    double money = moneyFuture.join();
                    double shards = shardsFuture.join();
                    PlayerStatsRecord withCombat = stats != null
                            ? stats.withCombat(killsFuture.join(), deathsFuture.join())
                            : null;
                    PlayerProfileSnapshot snapshot =
                            PlayerProfileSnapshot.fromStats(uuid, displayName, withCombat, money, shards);
                    viewer.getScheduler().run(plugin, entity -> {
                        if (!viewer.isOnline()) {
                            return;
                        }
                        onReady.accept(snapshot);
                    }, null);
                })
                .exceptionally(error -> {
                    PlayerStatsRecord withCombat = stats != null
                            ? stats.withCombat(combat.killsSync(uuid), combat.deathsSync(uuid))
                            : null;
                    PlayerProfileSnapshot snapshot =
                            PlayerProfileSnapshot.fromStats(uuid, displayName, withCombat, 0, 0);
                    viewer.getScheduler().run(plugin, entity -> {
                        if (viewer.isOnline()) {
                            onReady.accept(snapshot);
                        }
                    }, null);
                    return null;
                });
    }
}