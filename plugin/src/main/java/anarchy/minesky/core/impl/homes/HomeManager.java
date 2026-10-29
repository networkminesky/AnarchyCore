package anarchy.minesky.core.impl.homes;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.MaterialPickerDialogs;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.dialogapi.inputs.TextInput;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.tpa.TpaSounds;
import anarchy.minesky.core.impl.warps.WarpStorage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class HomeManager {

    private static final String INPUT_DISPLAY = "display";

    private final Plugin plugin;
    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final HomeSettings settings;
    private final HomeRepository repository;

    private final ConcurrentMap<UUID, ActiveTeleport> activeTeleports = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, List<String>> homeIdCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<String>> homeIdsByNickCache = new ConcurrentHashMap<>();

    public HomeManager(
            Plugin plugin,
            MessagesManager messages,
            FeedbackManager feedback,
            HomeSettings settings,
            HomeRepository repository) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
        this.settings = settings;
        this.repository = repository;
    }

    public void handleQuit(UUID playerId) {
        cancelTeleport(playerId, "homes.teleport-cancelled-quit");
        homeIdCache.remove(playerId);
    }

    /** Atualiza cache de IDs (async) — usado no join e após salvar home. */
    public void refreshHomeIdCache(UUID ownerUuid) {
        if (!repository.isActive()) {
            return;
        }
        repository.listHomesAsync(ownerUuid, (homes, error) -> {
            if (error != null || homes == null) {
                return;
            }
            homeIdCache.put(ownerUuid, homes.stream().map(HomeDefinition::id).toList());
        });
    }

    public List<String> completeHomeArgument(Player player, String token) {
        if (token == null) {
            token = "";
        }
        String lowerToken = token.toLowerCase(Locale.ROOT);
        int colon = token.indexOf(':');
        if (colon >= 0) {
            if (!player.hasPermission(HomeSettings.OTHERS_PERMISSION)) {
                return List.of();
            }
            String ownerPart = token.substring(0, colon);
            List<String> ids = resolveHomeIdsForOwner(ownerPart);
            List<String> out = new ArrayList<>();
            for (String id : ids) {
                String suggestion = ownerPart + ":" + id;
                if (suggestion.toLowerCase(Locale.ROOT).startsWith(lowerToken)) {
                    out.add(suggestion);
                }
            }
            return out;
        }

        List<String> out = new ArrayList<>();
        for (String id : cachedIds(player.getUniqueId())) {
            if (id.toLowerCase(Locale.ROOT).startsWith(lowerToken)) {
                out.add(id);
            }
        }
        if (player.hasPermission(HomeSettings.OTHERS_PERMISSION)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player)) {
                    continue;
                }
                String name = online.getName();
                String withColon = name + ":";
                if (name.toLowerCase(Locale.ROOT).startsWith(lowerToken)
                        || withColon.toLowerCase(Locale.ROOT).startsWith(lowerToken)) {
                    out.add(withColon);
                }
            }
        }
        return out;
    }

    public List<String> completeOwnHomeIds(Player player, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return cachedIds(player.getUniqueId()).stream()
                .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }

    private List<String> cachedIds(UUID ownerUuid) {
        List<String> ids = homeIdCache.get(ownerUuid);
        if (ids == null) {
            refreshHomeIdCache(ownerUuid);
            return List.of();
        }
        return ids;
    }

    private List<String> resolveHomeIdsForOwner(String ownerName) {
        Player online = Bukkit.getPlayerExact(ownerName);
        if (online != null) {
            List<String> ids = cachedIds(online.getUniqueId());
            if (ids.isEmpty()) {
                refreshHomeIdCache(online.getUniqueId());
            }
            return ids;
        }
        String nickKey = ownerName.toLowerCase(Locale.ROOT);
        List<String> cached = homeIdsByNickCache.get(nickKey);
        if (cached != null) {
            return cached;
        }
        warmNickHomeCache(ownerName, nickKey);
        return List.of();
    }

    private void warmNickHomeCache(String ownerName, String nickKey) {
        if (!repository.isActive()) {
            return;
        }
        repository.resolveOwnerUuidAsync(ownerName, (uuid, error) -> {
            if (error != null || uuid == null) {
                homeIdsByNickCache.put(nickKey, List.of());
                return;
            }
            repository.listHomesAsync(uuid, (homes, listError) -> {
                if (listError != null || homes == null) {
                    homeIdsByNickCache.put(nickKey, List.of());
                    return;
                }
                List<String> ids = homes.stream().map(HomeDefinition::id).toList();
                homeIdCache.put(uuid, ids);
                homeIdsByNickCache.put(nickKey, ids);
            });
        });
    }

    private void rememberHomeIds(UUID ownerUuid, List<HomeDefinition> homes) {
        List<String> ids = homes.stream().map(HomeDefinition::id).toList();
        homeIdCache.put(ownerUuid, ids);
    }

    public void openHomeList(Player player) {
        openHomeList(player, false);
    }

    public void openHomeList(Player player, boolean showAllEmptySlots) {
        if (!repository.isActive()) {
            notify(player, messages.component("homes.error.database-disabled"), true);
            return;
        }
        int limit = settings.resolveLimit(player);
        repository.listHomesAsync(player.getUniqueId(), (homes, error) -> player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) {
                return;
            }
            if (error != null) {
                notify(player, messages.component("homes.error.database"), true);
                return;
            }
            rememberHomeIds(player.getUniqueId(), homes);
            showHomeListDialog(player, homes, limit, showAllEmptySlots);
        }, null));
    }

    public void beginSetHome(Player player, String homeId) {
        if (!repository.isActive()) {
            notify(player, messages.component("homes.error.database-disabled"), true);
            return;
        }
        if (!WarpStorage.isValidId(homeId)) {
            notify(player, messages.component("homes.error.invalid-id"), true);
            return;
        }
        String id = WarpStorage.normalizeId(homeId);
        int limit = settings.resolveLimit(player);
        repository.listHomesAsync(player.getUniqueId(), (homes, error) -> player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) {
                return;
            }
            if (error != null) {
                notify(player, messages.component("homes.error.database"), true);
                return;
            }
            rememberHomeIds(player.getUniqueId(), homes);
            boolean exists = homes.stream().anyMatch(h -> h.id().equals(id));
            if (!exists && homes.size() >= limit) {
                notify(player, messages.component("homes.error.limit-reached", "limit", String.valueOf(limit)), true);
                return;
            }
            openDisplayNameDialog(player, id, exists ? findDisplay(homes, id) : id);
        }, null));
    }

    public void beginQuickCreateHome(Player player) {
        if (!repository.isActive()) {
            notify(player, messages.component("homes.error.database-disabled"), true);
            return;
        }
        int limit = settings.resolveLimit(player);
        repository.listHomesAsync(player.getUniqueId(), (homes, error) -> player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) {
                return;
            }
            if (error != null) {
                notify(player, messages.component("homes.error.database"), true);
                return;
            }
            if (homes.size() >= limit) {
                notify(player, messages.component("homes.error.limit-reached", "limit", String.valueOf(limit)), true);
                return;
            }
            String suggested = suggestNextHomeId(homes, limit);
            if (suggested == null) {
                notify(player, messages.component("homes.error.limit-reached", "limit", String.valueOf(limit)), true);
                return;
            }
            openDisplayNameDialog(player, suggested, suggested);
        }, null));
    }

    public void teleportToHome(Player traveller, String homeId) {
        teleportToHome(traveller, traveller.getUniqueId(), homeId);
    }

    public void teleportToOtherHome(Player traveller, String ownerName, String homeId) {
        if (!traveller.hasPermission(HomeSettings.OTHERS_PERMISSION)) {
            notify(traveller, messages.component("commands.no-permission"), true);
            return;
        }
        if (!WarpStorage.isValidId(homeId)) {
            notify(traveller, messages.component("homes.error.invalid-id"), true);
            return;
        }
        String id = WarpStorage.normalizeId(homeId);
        Player online = Bukkit.getPlayerExact(ownerName);
        if (online != null) {
            teleportToHome(traveller, online.getUniqueId(), id);
            return;
        }
        repository.resolveOwnerUuidAsync(ownerName, (uuid, error) -> traveller.getScheduler().run(plugin, task -> {
            if (!traveller.isOnline()) {
                return;
            }
            if (error != null || uuid == null) {
                notify(traveller, messages.component("commands.player-not-found", "player", ownerName), true);
                return;
            }
            teleportToHome(traveller, uuid, id);
        }, null));
    }

    private void teleportToHome(Player traveller, UUID ownerUuid, String homeId) {
        if (!repository.isActive()) {
            notify(traveller, messages.component("homes.error.database-disabled"), true);
            return;
        }
        repository.getHomeAsync(ownerUuid, homeId, (home, error) -> traveller.getScheduler().run(plugin, task -> {
            if (!traveller.isOnline()) {
                return;
            }
            if (error != null) {
                notify(traveller, messages.component("homes.error.database"), true);
                return;
            }
            if (home == null) {
                notify(traveller, messages.component("homes.error.not-found", "home", homeId), true);
                return;
            }
            if (!ownerUuid.equals(traveller.getUniqueId())
                    && !traveller.hasPermission(HomeSettings.OTHERS_PERMISSION)) {
                notify(traveller, messages.component("commands.no-permission"), true);
                return;
            }
            Location destination = home.toLocation();
            if (destination == null) {
                notify(traveller, messages.component("homes.error.world-missing", "home", home.displayName()), true);
                return;
            }
            if (traveller.hasPermission(HomeSettings.BYPASS_TIMER_PERMISSION)) {
                teleportNow(traveller, destination, home.displayName());
                return;
            }
            startCountdown(traveller, destination, home.displayName());
        }, null));
    }

    public void onPlayerMoved(Player player, Location from, Location to) {
        ActiveTeleport pending = activeTeleports.get(player.getUniqueId());
        if (pending == null) {
            return;
        }
        if (from.getWorld() == null || to.getWorld() == null) {
            return;
        }
        if (!from.getWorld().equals(to.getWorld())) {
            cancelTeleport(player.getUniqueId(), "homes.teleport-cancelled-move");
            return;
        }
        double dx = to.getX() - pending.frozenX();
        double dy = to.getY() - pending.frozenY();
        double dz = to.getZ() - pending.frozenZ();
        if (dx * dx + dy * dy + dz * dz > settings.moveThreshold() * settings.moveThreshold()) {
            cancelTeleport(player.getUniqueId(), "homes.teleport-cancelled-move");
        }
    }

    private void showHomeListDialog(Player player, List<HomeDefinition> homes, int limit, boolean showAllEmpty) {
        if (homes.isEmpty() && limit == 0) {
            notify(player, messages.component("homes.error.no-slots"), true);
            return;
        }

        MineSkyDialog.Builder builder = MineSkyDialog.builder(messages.raw("homes.dialog.list.title"))
                .bodyItemIcon(new ItemStack(settings.listHeaderIcon()))
                .multiActionColumns(1);

        for (HomeDefinition home : homes) {
            builder.button(DialogButton.builder()
                    .label(home.menuButtonLabel())
                    .width(260)
                    .onClick(p -> openHomeDetail(p, home))
                    .build());
        }

        int remaining = Math.max(0, limit - homes.size());
        int preview = settings.emptySlotsPreview();
        int emptyButtons = showAllEmpty || remaining <= preview ? remaining : preview;

        for (int i = 0; i < emptyButtons; i++) {
            builder.button(DialogButton.builder()
                    .label(messages.raw("homes.dialog.create-slot"))
                    .width(260)
                    .onClick(p -> beginQuickCreateHome(p))
                    .build());
        }

        if (!showAllEmpty && remaining > preview) {
            builder.button(DialogButton.builder()
                    .label(messages.raw("homes.dialog.show-all-slots"))
                    .width(260)
                    .onClick(p -> openHomeList(p, true))
                    .build());
        }

        builder.show(player);
    }

    public void deleteHome(Player player, String homeId) {
        if (!repository.isActive()) {
            notify(player, messages.component("homes.error.database-disabled"), true);
            return;
        }
        if (!WarpStorage.isValidId(homeId)) {
            notify(player, messages.component("homes.error.invalid-id"), true);
            return;
        }
        String id = WarpStorage.normalizeId(homeId);
        repository.deleteHomeAsync(player.getUniqueId(), id, (deleted, error) -> player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) {
                return;
            }
            if (error != null) {
                notify(player, messages.component("homes.error.database"), true);
                return;
            }
            if (!deleted) {
                notify(player, messages.component("homes.error.not-found", "home", id), true);
                return;
            }
            refreshHomeIdCache(player.getUniqueId());
            notify(player, messages.component("homes.delete-success", "home", id), false);
        }, null));
    }

    private void openHomeDetail(Player player, HomeDefinition home) {
        MineSkyDialog.Builder builder = MineSkyDialog.builder(messages.raw("homes.dialog.detail.title"))
                .body(messages.format("homes.dialog.detail.body", "id", home.id()))
                .multiActionColumns(1)
                .button(DialogButton.builder()
                        .label(messages.raw("homes.dialog.detail.teleport"))
                        .width(260)
                        .onClick(p -> teleportToHome(p, home.id()))
                        .build())
                .button(DialogButton.builder()
                        .label(messages.raw("homes.dialog.detail.edit-name"))
                        .width(260)
                        .onClick(p -> openEditNameDialog(p, home))
                        .build());
        if (player.hasPermission(HomeSettings.ICON_PERMISSION)) {
            builder.button(DialogButton.builder()
                    .label(messages.raw("homes.dialog.detail.edit-icon"))
                    .width(260)
                    .onClick(p -> openEditIconPicker(p, home))
                    .build());
        }
        builder.button(DialogButton.builder()
                        .label(messages.raw("homes.dialog.detail.back"))
                        .width(200)
                        .onClick(p -> openHomeList(p))
                        .build())
                .show(player);
    }

    private void openEditNameDialog(Player player, HomeDefinition home) {
        MineSkyDialog.builder(messages.raw("homes.dialog.edit-name-title"))
                .body(messages.format("homes.dialog.set-body", "id", home.id()))
                .addInput(TextInput.builder(INPUT_DISPLAY, messages.raw("homes.dialog.input-display"))
                        .placeholder(messages.raw("homes.dialog.input-display-placeholder"))
                        .initial(home.displayName())
                        .maxLength(64)
                        .build())
                .button(messages.raw("homes.dialog.save-button"), (editor, response) -> {
                    String display = response.getText(INPUT_DISPLAY, home.id()).trim();
                    if (display.isEmpty()) {
                        display = home.id();
                    }
                    persistHome(editor, home.withDisplayName(display), "homes.edit-name-success");
                })
                .button(messages.raw("homes.dialog.cancel-button"), p -> openHomeDetail(p, home))
                .show(player);
    }

    private void openEditIconPicker(Player player, HomeDefinition home) {
        if (!player.hasPermission(HomeSettings.ICON_PERMISSION)) {
            notify(player, messages.component("commands.no-permission"), true);
            return;
        }
        MaterialPickerDialogs.open(player, messages.raw("homes.dialog.pick-icon-title"), material -> {
            if (material == null || material.isAir()) {
                return;
            }
            persistHome(player, home.withIcon(material), "homes.edit-icon-success");
        });
    }

    private void openDisplayNameDialog(Player player, String homeId, String initialDisplay) {
        openDisplayNameDialog(player, homeId, initialDisplay, true);
    }

    private void openDisplayNameDialog(Player player, String homeId, String initialDisplay, boolean createAtLocation) {
        MineSkyDialog.builder(messages.raw("homes.dialog.set-title"))
                .body(messages.format("homes.dialog.set-body", "id", homeId))
                .addInput(TextInput.builder(INPUT_DISPLAY, messages.raw("homes.dialog.input-display"))
                        .placeholder(messages.raw("homes.dialog.input-display-placeholder"))
                        .initial(initialDisplay)
                        .maxLength(64)
                        .build())
                .button(messages.raw("homes.dialog.save-button"), (editor, response) -> {
                    String display = response.getText(INPUT_DISPLAY, homeId).trim();
                    if (display.isEmpty()) {
                        display = homeId;
                    }
                    if (createAtLocation) {
                        saveHomeAtPlayerLocation(editor, homeId, display);
                    }
                })
                .button(messages.raw("homes.dialog.cancel-button"), p ->
                        notify(p, messages.component("homes.dialog.cancelled"), false))
                .show(player);
    }

    private void saveHomeAtPlayerLocation(Player player, String homeId, String displayName) {
        HomeDefinition home = new HomeDefinition(
                player.getUniqueId(),
                homeId,
                displayName,
                settings.defaultHomeIcon(),
                player.getLocation());
        persistHome(player, home, "homes.set-success");
    }

    private void persistHome(Player player, HomeDefinition home, String successMessagePath) {
        repository.saveHomeAsync(home, (ignored, error) -> player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) {
                return;
            }
            if (error != null) {
                notify(player, messages.component("homes.error.database"), true);
                return;
            }
            refreshHomeIdCache(player.getUniqueId());
            notify(player, messages.component(successMessagePath, "home", home.displayName()), false);
        }, null));
    }

    private static String findDisplay(List<HomeDefinition> homes, String id) {
        for (HomeDefinition home : homes) {
            if (home.id().equals(id)) {
                return home.displayName();
            }
        }
        return id;
    }

    static String suggestNextHomeId(List<HomeDefinition> homes, int limit) {
        Set<String> used = new HashSet<>();
        for (HomeDefinition home : homes) {
            used.add(home.id().toLowerCase(Locale.ROOT));
        }
        for (int i = 1; i <= limit; i++) {
            String candidate = "home" + i;
            if (!used.contains(candidate)) {
                return candidate;
            }
        }
        for (int i = 1; i <= limit; i++) {
            String candidate = "home-" + i;
            if (!used.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private void startCountdown(Player traveller, Location destination, String homeLabel) {
        cancelTeleport(traveller.getUniqueId(), null);
        Location loc = traveller.getLocation();
        ActiveTeleport state = new ActiveTeleport(
                loc.getX(), loc.getY(), loc.getZ(),
                destination.clone(),
                homeLabel,
                settings.countdownSeconds());
        activeTeleports.put(traveller.getUniqueId(), state);
        notify(traveller, messages.component("homes.countdown-start",
                "seconds", String.valueOf(settings.countdownSeconds()),
                "home", homeLabel), false);
        traveller.getScheduler().run(plugin, task -> scheduleCountdownTick(traveller.getUniqueId()), null);
    }

    private void scheduleCountdownTick(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            activeTeleports.remove(playerId);
            return;
        }
        ActiveTeleport state = activeTeleports.get(playerId);
        if (state == null) {
            return;
        }
        if (state.secondsLeft() <= 0) {
            activeTeleports.remove(playerId);
            teleportNow(player, state.destination(), state.homeLabel());
            return;
        }
        TpaSounds.play(player, settings.countdownTick(), 0.5f,
                0.8f + (0.1f * (settings.countdownSeconds() - state.secondsLeft())));
        feedback.send(player, messages.component("homes.actionbar.countdown",
                "seconds", String.valueOf(state.secondsLeft()),
                "home", state.homeLabel()), FeedbackPriority.HIGH);
        state.decrementSeconds();
        player.getScheduler().runDelayed(plugin, task -> scheduleCountdownTick(playerId), null, 20L);
    }

    private void teleportNow(Player player, Location destination, String homeLabel) {
        player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) {
                return;
            }
            player.teleportAsync(destination).thenAccept(success -> player.getScheduler().run(plugin, after -> {
                if (!player.isOnline()) {
                    return;
                }
                if (success) {
                    notify(player, messages.component("homes.teleport-success", "home", homeLabel), false);
                    feedback.send(player, messages.component("homes.actionbar.teleport-done"));
                } else {
                    notify(player, messages.component("homes.teleport-failed"), true);
                }
            }, null));
        }, null);
    }

    private void cancelTeleport(UUID playerId, String messagePath) {
        ActiveTeleport removed = activeTeleports.remove(playerId);
        if (removed == null) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        if (messagePath != null) {
            notify(player, messages.component(messagePath), true);
            feedback.send(player, messages.component("homes.actionbar.cancelled"));
        }
    }

    private void notify(Player player, Component message, boolean error) {
        feedback.send(
                player,
                message,
                error ? FeedbackPriority.HIGH : FeedbackPriority.NORMAL,
                true,
                error ? UiSoundMode.ERROR : UiSoundMode.SUCCESS);
    }

    private static final class ActiveTeleport {
        private final double frozenX;
        private final double frozenY;
        private final double frozenZ;
        private final Location destination;
        private final String homeLabel;
        private int secondsLeft;

        ActiveTeleport(
                double frozenX,
                double frozenY,
                double frozenZ,
                Location destination,
                String homeLabel,
                int secondsLeft) {
            this.frozenX = frozenX;
            this.frozenY = frozenY;
            this.frozenZ = frozenZ;
            this.destination = destination;
            this.homeLabel = homeLabel;
            this.secondsLeft = secondsLeft;
        }

        double frozenX() {
            return frozenX;
        }

        double frozenY() {
            return frozenY;
        }

        double frozenZ() {
            return frozenZ;
        }

        Location destination() {
            return destination;
        }

        String homeLabel() {
            return homeLabel;
        }

        int secondsLeft() {
            return secondsLeft;
        }

        void decrementSeconds() {
            secondsLeft--;
        }
    }
}
