package anarchy.minesky.core.impl.warps;

import anarchy.minesky.core.api.dialogapi.DialogButton;
import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.dialogapi.inputs.TextInput;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.tpa.TpaSounds;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WarpManager {

    private static final String INPUT_DISPLAY = "display";
    private static final String INPUT_ICON = "icon";

    private final Plugin plugin;
    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final WarpSettings settings;
    private final WarpStorage storage;

    private final Map<UUID, ActiveTeleport> activeTeleports = new ConcurrentHashMap<>();

    public WarpManager(
            Plugin plugin,
            MessagesManager messages,
            FeedbackManager feedback,
            WarpSettings settings,
            WarpStorage storage) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
        this.settings = settings;
        this.storage = storage;
    }

    public WarpStorage storage() {
        return storage;
    }

    public void reloadStorage() {
        storage.load();
    }

    public void handleQuit(UUID playerId) {
        cancelTeleport(playerId, "warps.teleport-cancelled-quit");
    }

    public void openWarpList(Player player) {
        List<WarpDefinition> warps = storage.accessibleBy(player);
        if (warps.isEmpty()) {
            notify(player, messages.component("warps.error.none-visible"), true);
            return;
        }

        MineSkyDialog.Builder builder = MineSkyDialog.builder(messages.raw("warps.dialog.list.title"))
                .bodyItemIcon(new ItemStack(settings.listHeaderIcon()))
                .multiActionColumns(1);

        for (WarpDefinition warp : warps) {
            builder.button(DialogButton.builder()
                    .label(warp.menuButtonLabel())
                    .width(250)
                    .onClick(p -> teleportToWarp(p, warp.id()))
                    .build());
        }

        builder.show(player);
    }

    public void teleportToWarp(Player player, String warpId) {
        String id = WarpStorage.normalizeId(warpId);
        WarpDefinition warp = storage.get(id).orElse(null);
        if (warp == null) {
            notify(player, messages.component("warps.error.not-found", "warp", warpId), true);
            return;
        }
        if (!player.hasPermission(warp.permissionNode())) {
            notify(player, messages.component("warps.error.no-permission", "warp", id), true);
            return;
        }

        Location destination = warp.toLocation();
        if (destination == null) {
            notify(player, messages.component("warps.error.world-missing", "warp", id), true);
            return;
        }

        if (player.hasPermission(WarpSettings.BYPASS_TIMER_PERMISSION)) {
            teleportNow(player, destination, warp.displayName());
            return;
        }

        startCountdown(player, destination, warp.displayName());
    }

    public void beginSetWarp(Player player, String warpId) {
        if (!player.hasPermission(WarpSettings.ADMIN_PERMISSION)) {
            notify(player, messages.component("commands.no-permission"), true);
            return;
        }
        if (!WarpStorage.isValidId(warpId)) {
            notify(player, messages.component("warps.error.invalid-id"), true);
            return;
        }

        String id = WarpStorage.normalizeId(warpId);
        openEditorDialog(player, id, null, Material.COMPASS, true);
    }

    public void beginEditWarp(Player player, String warpId) {
        if (!player.hasPermission(WarpSettings.ADMIN_PERMISSION)) {
            notify(player, messages.component("commands.no-permission"), true);
            return;
        }
        if (!WarpStorage.isValidId(warpId)) {
            notify(player, messages.component("warps.error.invalid-id"), true);
            return;
        }

        String id = WarpStorage.normalizeId(warpId);
        WarpDefinition existing = storage.get(id).orElse(null);
        if (existing == null) {
            notify(player, messages.component("warps.error.not-found", "warp", id), true);
            return;
        }

        openEditorDialog(player, id, existing.displayName(), existing.iconMaterial(), false);
    }

    private void openEditorDialog(
            Player player,
            String id,
            String initialDisplay,
            Material initialIcon,
            boolean create) {
        String titleKey = create ? "warps.dialog.editor.create-title" : "warps.dialog.editor.edit-title";
        String bodyKey = create ? "warps.dialog.editor.create-body" : "warps.dialog.editor.edit-body";

        MineSkyDialog.builder(messages.raw(titleKey))
                .body(messages.format(bodyKey, "id", id))
                .addInput(TextInput.builder(INPUT_DISPLAY, messages.raw("warps.dialog.editor.input-display"))
                        .placeholder(messages.raw("warps.dialog.editor.input-display-placeholder"))
                        .initial(initialDisplay != null ? initialDisplay : id)
                        .maxLength(64)
                        .build())
                .addInput(TextInput.builder(INPUT_ICON, messages.raw("warps.dialog.editor.input-icon"))
                        .placeholder(messages.raw("warps.dialog.editor.input-icon-placeholder"))
                        .initial(initialIcon.name())
                        .maxLength(64)
                        .build())
                .button(messages.raw("warps.dialog.editor.save-button"), (editor, response) -> {
                    String display = response.getText(INPUT_DISPLAY, id).trim();
                    if (display.isEmpty()) {
                        display = id;
                    }
                    Material icon = WarpStorage.parseMaterial(response.getText(INPUT_ICON), initialIcon);
                    if (create) {
                        WarpDefinition warp = WarpStorage.createAt(id, display, icon, editor.getLocation());
                        storage.put(warp);
                        notify(editor, messages.component("warps.set-success", "warp", id), false);
                    } else {
                        WarpDefinition existing = storage.get(id).orElse(null);
                        if (existing == null) {
                            notify(editor, messages.component("warps.error.not-found", "warp", id), true);
                            return;
                        }
                        storage.put(existing.withDisplay(display, icon));
                        notify(editor, messages.component("warps.edit-success", "warp", id), false);
                    }
                })
                .button(messages.raw("warps.dialog.editor.cancel-button"), p ->
                        notify(p, messages.component("warps.dialog.editor.cancelled"), false))
                .show(player);
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
            cancelTeleport(player.getUniqueId(), "warps.teleport-cancelled-move");
            return;
        }
        double dx = to.getX() - pending.frozenX();
        double dy = to.getY() - pending.frozenY();
        double dz = to.getZ() - pending.frozenZ();
        if (dx * dx + dy * dy + dz * dz > settings.moveThreshold() * settings.moveThreshold()) {
            cancelTeleport(player.getUniqueId(), "warps.teleport-cancelled-move");
        }
    }

    private void startCountdown(Player traveller, Location destination, String warpLabel) {
        cancelTeleport(traveller.getUniqueId(), null);

        Location loc = traveller.getLocation();
        ActiveTeleport state = new ActiveTeleport(
                loc.getX(), loc.getY(), loc.getZ(),
                destination.clone(),
                warpLabel,
                settings.countdownSeconds());
        activeTeleports.put(traveller.getUniqueId(), state);

        notify(traveller, messages.component("warps.countdown-start",
                "seconds", String.valueOf(settings.countdownSeconds()),
                "warp", warpLabel), false);
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
            teleportNow(player, state.destination(), state.warpLabel());
            return;
        }

        TpaSounds.play(player, settings.countdownTick(), 0.5f,
                0.8f + (0.1f * (settings.countdownSeconds() - state.secondsLeft())));
        feedback.send(player, messages.component("warps.actionbar.countdown",
                "seconds", String.valueOf(state.secondsLeft()),
                "warp", state.warpLabel()), FeedbackPriority.HIGH);

        state.decrementSeconds();
        player.getScheduler().runDelayed(plugin, task -> scheduleCountdownTick(playerId), null, 20L);
    }

    private void teleportNow(Player player, Location destination, String warpLabel) {
        player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) {
                return;
            }
            player.teleportAsync(destination).thenAccept(success -> player.getScheduler().run(plugin, after -> {
                if (!player.isOnline()) {
                    return;
                }
                if (success) {
                    TpaSounds.play(player, settings.teleport(), settings.soundVolume(), settings.soundPitch());
                    notify(player, messages.component("warps.teleport-success", "warp", warpLabel), false);
                    feedback.send(player, messages.component("warps.actionbar.teleport-done"));
                } else {
                    notify(player, messages.component("warps.teleport-failed"), true);
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
        TpaSounds.play(player, settings.cancel(), settings.soundVolume(), 0.7f);
        if (messagePath != null) {
            notify(player, messages.component(messagePath), true);
            feedback.send(player, messages.component("warps.actionbar.cancelled"));
        }
    }

    private void notify(Player player, Component message, boolean error) {
        feedback.send(player, message, error ? FeedbackPriority.HIGH : FeedbackPriority.NORMAL);
    }

    private static final class ActiveTeleport {
        private final double frozenX;
        private final double frozenY;
        private final double frozenZ;
        private final Location destination;
        private final String warpLabel;
        private int secondsLeft;

        ActiveTeleport(
                double frozenX,
                double frozenY,
                double frozenZ,
                Location destination,
                String warpLabel,
                int secondsLeft) {
            this.frozenX = frozenX;
            this.frozenY = frozenY;
            this.frozenZ = frozenZ;
            this.destination = destination;
            this.warpLabel = warpLabel;
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

        String warpLabel() {
            return warpLabel;
        }

        int secondsLeft() {
            return secondsLeft;
        }

        void decrementSeconds() {
            secondsLeft--;
        }
    }
}
