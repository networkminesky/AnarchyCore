package anarchy.minesky.core.impl.tpa;

import anarchy.minesky.core.api.dialogapi.MineSkyDialog;
import anarchy.minesky.core.api.feedback.FeedbackPriority;
import anarchy.minesky.core.api.feedback.UiSoundMode;
import anarchy.minesky.core.impl.config.MessagesManager;
import anarchy.minesky.core.impl.feedback.FeedbackManager;
import anarchy.minesky.core.impl.playerprefs.PlayerPreferencesService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TpaManager {

    private final Plugin plugin;
    private final MessagesManager messages;
    private final FeedbackManager feedback;
    private final TpaSettings settings;
    private final PlayerPreferencesService playerPrefs;

    private final Map<UUID, List<TpaRequest>> incomingByTarget = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveTeleport> activeTeleports = new ConcurrentHashMap<>();

    public TpaManager(
            Plugin plugin,
            MessagesManager messages,
            FeedbackManager feedback,
            TpaSettings settings,
            PlayerPreferencesService playerPrefs) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
        this.settings = settings;
        this.playerPrefs = playerPrefs;
    }

    public void handleQuit(UUID playerId) {
        incomingByTarget.remove(playerId);
        cancelTeleport(playerId, "tpa.teleport-cancelled-quit");
        incomingByTarget.values().forEach(list -> list.removeIf(r -> r.requesterId().equals(playerId)));
    }

    public void purgeExpired() {
        long expireMs = settings.requestExpireSeconds() * 1000L;
        long now = System.currentTimeMillis();
        for (List<TpaRequest> list : incomingByTarget.values()) {
            list.removeIf(r -> now - r.createdAtMillis() > expireMs);
        }
        incomingByTarget.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    public void requestTpa(Player requester, Player target) {
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            notify(requester, messages.component("tpa.error.self"), true);
            return;
        }
        if (!playerPrefs.canReceiveTpaFrom(target, requester)) {
            notify(requester, messages.component("tpa.error.target-blocks-strangers", "target", target.getName()), true);
            return;
        }
        registerRequest(requester, target, TpaRequestType.TPA);
    }

    public void requestTpaHereWithConfirm(Player requester, Player target) {
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            notify(requester, messages.component("tpa.error.self"), true);
            return;
        }
        if (!playerPrefs.canReceiveTpaHereFrom(target, requester)) {
            notify(requester, messages.component("tpa.error.target-blocks-strangers", "target", target.getName()), true);
            return;
        }

        MineSkyDialog.builder(messages.raw("tpa.dialog.tpahere-send.title"))
                .body(messages.format("tpa.dialog.tpahere-send.body", "target", target.getName()))
                .body(messages.raw("tpa.dialog.tpahere-send.warning"))
                .confirmButton(messages.raw("tpa.dialog.confirm-yes"), p -> registerRequest(p, target, TpaRequestType.TPAHERE))
                .cancelButton(messages.raw("tpa.dialog.confirm-no"), p -> notify(p, messages.component("tpa.dialog.tpahere-send.cancelled"), false))
                .show(requester);
    }

    private void registerRequest(Player requester, Player target, TpaRequestType type) {
        purgeExpired();

        List<TpaRequest> list = incomingByTarget.computeIfAbsent(target.getUniqueId(), id -> new ArrayList<>());
        list.removeIf(r -> r.requesterId().equals(requester.getUniqueId()));

        TpaRequest request = new TpaRequest(requester.getUniqueId(), target.getUniqueId(), type, System.currentTimeMillis());
        list.add(request);

        playerPrefs.markKnown(requester.getUniqueId(), target.getUniqueId());

        if (type == TpaRequestType.TPA) {
            notify(requester, messages.component("tpa.request-sent-tpa", "target", target.getName()), false);
            notify(target, messages.component("tpa.request-received-tpa", "player", requester.getName()), false);
            feedback.send(target, messages.component("tpa.actionbar.request-tpa", "player", requester.getName()));
        } else {
            notify(requester, messages.component("tpa.request-sent-tpahere", "target", target.getName()), false);
            notify(target, messages.component("tpa.request-received-tpahere", "player", requester.getName()), false);
            feedback.send(target, messages.component("tpa.actionbar.request-tpahere", "player", requester.getName()));
        }
    }

    public void acceptRequest(Player accepter, String requesterName) {
        purgeExpired();
        TpaRequest request = findRequest(accepter, requesterName);
        if (request == null) {
            notify(accepter, messages.component("tpa.error.no-request"), true);
            return;
        }

        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester == null || !requester.isOnline()) {
            removeRequest(request);
            notify(accepter, messages.component("commands.player-not-found", "player", requesterName != null ? requesterName : "?"), true);
            return;
        }

        showAcceptDialog(accepter, requester, request);
    }

    private void showAcceptDialog(Player accepter, Player requester, TpaRequest request) {
        if (request.type() == TpaRequestType.TPAHERE) {
            MineSkyDialog.builder(messages.raw("tpa.dialog.accept-tpahere.title"))
                    .body(messages.format("tpa.dialog.accept-tpahere.body", "player", requester.getName()))
                    .body(messages.raw("tpa.dialog.accept-tpahere.warning"))
                    .body(messages.raw("tpa.dialog.accept-tpahere.trap-warning"))
                    .confirmButton(messages.raw("tpa.dialog.confirm-yes"), p -> finalizeAccept(p, request))
                    .cancelButton(messages.raw("tpa.dialog.confirm-no"), p -> notify(p, messages.component("tpa.dialog.accept-cancelled"), false))
                    .show(accepter);
        } else {
            MineSkyDialog.builder(messages.raw("tpa.dialog.accept-tpa.title"))
                    .body(messages.format("tpa.dialog.accept-tpa.body", "player", requester.getName()))
                    .body(messages.raw("tpa.dialog.accept-tpa.warning"))
                    .confirmButton(messages.raw("tpa.dialog.confirm-yes"), p -> finalizeAccept(p, request))
                    .cancelButton(messages.raw("tpa.dialog.confirm-no"), p -> notify(p, messages.component("tpa.dialog.accept-cancelled"), false))
                    .show(accepter);
        }
    }

    private void finalizeAccept(Player accepter, TpaRequest request) {
        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester == null || !requester.isOnline()) {
            removeRequest(request);
            notify(accepter, messages.component("tpa.error.requester-offline"), true);
            return;
        }

        removeRequest(request);

        Player traveller;
        Player destinationAnchor;
        if (request.type() == TpaRequestType.TPA) {
            traveller = requester;
            destinationAnchor = accepter;
            notify(accepter, messages.component("tpa.accepted-target-tpa", "player", requester.getName()), false);
            notify(requester, messages.component("tpa.accepted-traveller-tpa", "player", accepter.getName()), false);
        } else {
            traveller = accepter;
            destinationAnchor = requester;
            notify(accepter, messages.component("tpa.accepted-traveller-tpahere", "player", requester.getName()), false);
            notify(requester, messages.component("tpa.accepted-target-tpahere", "player", accepter.getName()), false);
        }

        Location destination = destinationAnchor.getLocation();
        if (traveller.hasPermission(TpaSettings.BYPASS_TIMER_PERMISSION)) {
            teleportNow(traveller, destination);
            return;
        }

        startCountdown(traveller, destination);
    }

    public void denyRequest(Player denier, String requesterName) {
        purgeExpired();
        TpaRequest request = findRequest(denier, requesterName);
        if (request == null) {
            notify(denier, messages.component("tpa.error.no-request"), true);
            return;
        }

        Player requester = Bukkit.getPlayer(request.requesterId());
        removeRequest(request);

        if (requester != null && requester.isOnline()) {
            notify(requester, messages.component("tpa.denied-requester", "player", denier.getName()), true);
        }
        notify(denier, messages.component("tpa.denied-target", "player",
                requester != null ? requester.getName() : "?"), false);
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
            cancelTeleport(player.getUniqueId(), "tpa.teleport-cancelled-move");
            return;
        }
        double dx = to.getX() - pending.frozenX();
        double dy = to.getY() - pending.frozenY();
        double dz = to.getZ() - pending.frozenZ();
        if (dx * dx + dy * dy + dz * dz > settings.moveThreshold() * settings.moveThreshold()) {
            cancelTeleport(player.getUniqueId(), "tpa.teleport-cancelled-move");
        }
    }

    private void startCountdown(Player traveller, Location destination) {
        cancelTeleport(traveller.getUniqueId(), null);

        Location loc = traveller.getLocation();
        ActiveTeleport state = new ActiveTeleport(
                loc.getX(), loc.getY(), loc.getZ(),
                destination.clone(),
                settings.countdownSeconds());
        activeTeleports.put(traveller.getUniqueId(), state);

        notify(traveller, messages.component("tpa.countdown-start", "seconds", String.valueOf(settings.countdownSeconds())), false);
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
            teleportNow(player, state.destination());
            return;
        }

        TpaSounds.play(player, settings.countdownTick(), 0.5f, 0.8f + (0.1f * (settings.countdownSeconds() - state.secondsLeft())));
        feedback.send(player, messages.component("tpa.actionbar.countdown", "seconds", String.valueOf(state.secondsLeft())), FeedbackPriority.HIGH);

        state.decrementSeconds();
        player.getScheduler().runDelayed(plugin, task -> scheduleCountdownTick(playerId), null, 20L);
    }

    private void teleportNow(Player player, Location destination) {
        player.getScheduler().run(plugin, task -> {
            if (!player.isOnline()) {
                return;
            }
            player.teleportAsync(destination).thenAccept(success -> player.getScheduler().run(plugin, after -> {
                if (!player.isOnline()) {
                    return;
                }
                if (success) {
                    notify(player, messages.component("tpa.teleport-success"), false);
                    feedback.send(player, messages.component("tpa.actionbar.teleport-done"));
                } else {
                    notify(player, messages.component("tpa.teleport-failed"), true);
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
            feedback.send(player, messages.component("tpa.actionbar.cancelled"));
        }
    }

    private TpaRequest findRequest(Player target, String requesterName) {
        List<TpaRequest> list = incomingByTarget.get(target.getUniqueId());
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (requesterName == null || requesterName.isBlank()) {
            TpaRequest latest = null;
            for (TpaRequest request : list) {
                if (latest == null || request.createdAtMillis() > latest.createdAtMillis()) {
                    latest = request;
                }
            }
            return latest;
        }
        for (TpaRequest request : list) {
            Player requester = Bukkit.getPlayer(request.requesterId());
            if (requester != null && requester.getName().equalsIgnoreCase(requesterName)) {
                return request;
            }
        }
        for (TpaRequest request : list) {
            Player requester = Bukkit.getPlayer(request.requesterId());
            if (requester == null) {
                continue;
            }
            if (requester.getName().toLowerCase().startsWith(requesterName.toLowerCase())) {
                return request;
            }
        }
        return null;
    }

    private void removeRequest(TpaRequest request) {
        List<TpaRequest> list = incomingByTarget.get(request.targetId());
        if (list == null) {
            return;
        }
        list.removeIf(r -> r.requesterId().equals(request.requesterId()) && r.type() == request.type());
        if (list.isEmpty()) {
            incomingByTarget.remove(request.targetId());
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

    public static Player resolvePlayer(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return exact;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(name)) {
                return online;
            }
        }
        return null;
    }

    private static final class ActiveTeleport {
        private final double frozenX;
        private final double frozenY;
        private final double frozenZ;
        private final Location destination;
        private int secondsLeft;

        ActiveTeleport(double frozenX, double frozenY, double frozenZ, Location destination, int secondsLeft) {
            this.frozenX = frozenX;
            this.frozenY = frozenY;
            this.frozenZ = frozenZ;
            this.destination = destination;
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

        int secondsLeft() {
            return secondsLeft;
        }

        void decrementSeconds() {
            secondsLeft--;
        }
    }
}
