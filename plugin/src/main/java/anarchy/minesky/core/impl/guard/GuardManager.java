package anarchy.minesky.core.impl.guard;

import anarchy.minesky.core.api.text.DialogComponentUtils;
import anarchy.minesky.core.impl.dialogapi.BedrockPlatformDetector;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.blockentity.BlockEntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockEntityData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuardManager {

    private final Plugin plugin;
    private final GuardSettings settings;
    private final GuardRepository repository;
    private final GuardPlayerSnapshot snapshots;
    private final Map<UUID, GuardSession> sessions = new ConcurrentHashMap<>();

    public GuardManager(
            Plugin plugin,
            GuardSettings settings,
            GuardRepository repository,
            GuardPlayerSnapshot snapshots) {
        this.plugin = plugin;
        this.settings = settings;
        this.repository = repository;
        this.snapshots = snapshots;
    }

    public GuardSettings settings() {
        return settings;
    }

    public GuardRepository repository() {
        return repository;
    }

    public GuardPlayerSnapshot snapshots() {
        return snapshots;
    }

    public boolean shouldSkip(Player player) {
        if (player == null || !player.isOnline()) {
            return true;
        }
        if (!settings.enabled()) {
            return true;
        }
        if (BedrockPlatformDetector.isBedrock(player)) {
            return true;
        }
        if (BedrockPlatformDetector.isMathematicallyBedrock(player.getUniqueId())) {
            return true;
        }
        return player.hasPermission(settings.bypassPermission());
    }

    public boolean hasGuardBypass(Player player) {
        return player != null && player.hasPermission(settings.bypassPermission());
    }

    public void scheduleJoinCheck(Player player) {
        if (shouldSkip(player)) {
            return;
        }
        player.getScheduler().runDelayed(
                plugin,
                task -> {
                    if (player.isOnline()) {
                        startCheck(player, "join");
                    }
                },
                null,
                settings.joinDelayTicks());
    }

    public boolean startCheck(Player player, String trigger) {
        if (shouldSkip(player)) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        if (sessions.containsKey(uuid)) {
            return false;
        }

        snapshots.refreshFromPlayer(player);

        List<GuardProbe> enabledProbes = new ArrayList<>(settings.probes().values());
        if (enabledProbes.isEmpty()) {
            return false;
        }

        int chunkSize = settings.linesPerSign();
        List<List<GuardProbe>> chunks = new ArrayList<>();
        List<GuardProbe> current = new ArrayList<>();
        for (GuardProbe probe : enabledProbes) {
            current.add(probe);
            if (current.size() >= chunkSize) {
                chunks.add(List.copyOf(current));
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) {
            chunks.add(List.copyOf(current));
        }

        GuardSession session = new GuardSession(uuid, trigger);
        var loc = player.getLocation();
        int baseX = loc.getBlockX();
        int baseY = loc.getBlockY() + settings.virtualSignYOffset();
        int baseZ = loc.getBlockZ();

        for (int i = 0; i < chunks.size(); i++) {
            Vector3i pos = new Vector3i(baseX + i, baseY, baseZ);
            session.addBatch(pos, chunks.get(i));
        }

        sessions.put(uuid, session);
        debug(player, "Iniciando check (" + trigger + ") batches=" + chunks.size() + " probes=" + enabledProbes.size());
        sendCurrentBatch(player, session);
        scheduleTimeout(player, session);
        return true;
    }

    private void scheduleTimeout(Player player, GuardSession session) {
        player.getScheduler().runDelayed(
                plugin,
                task -> {
                    GuardSession active = sessions.get(player.getUniqueId());
                    if (active != session || active.phase() != GuardSession.Phase.WAITING_RESPONSE) {
                        return;
                    }
                    finishSession(
                            player,
                            session,
                            List.of(),
                            "timeout — cliente não respondeu à placa virtual",
                            false);
                },
                null,
                settings.responseTimeoutTicks());
    }

    private void sendCurrentBatch(Player player, GuardSession session) {
        GuardSession.SignBatch batch = session.currentBatch();
        if (batch == null) {
            finishSession(player, session, session.detected(), "verificação concluída", true);
            return;
        }

        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) {
            finishSession(player, session, List.of(), "PacketEvents user indisponível", false);
            return;
        }

        Vector3i pos = batch.position();
        WrappedBlockState signState = WrappedBlockState.getDefaultState(StateTypes.OAK_SIGN);
        WrappedBlockState airState = WrappedBlockState.getDefaultState(StateTypes.AIR);

        debug(
                player,
                "Enviando batch "
                        + (session.batchIndex() + 1)
                        + "/"
                        + session.batchCount()
                        + " pos="
                        + pos
                        + " probes="
                        + batch.probes().stream().map(GuardProbe::id).toList());

        if (settings.instantPacketBurst()) {
            sendInstantSignBurst(user, pos, batch.probes(), signState, airState);
            return;
        }

        user.sendPacket(new WrapperPlayServerBlockChange(pos, signState));
        user.sendPacket(new WrapperPlayServerBlockEntityData(pos, BlockEntityTypes.SIGN, buildSignNbt(batch.probes())));

        player.getScheduler().runDelayed(
                plugin,
                openTask -> {
                    if (!player.isOnline() || sessions.get(player.getUniqueId()) != session) {
                        return;
                    }
                    user.sendPacket(new WrapperPlayServerOpenSignEditor(pos, true));
                    player.getScheduler().runDelayed(
                            plugin,
                            closeTask -> {
                                if (!player.isOnline() || sessions.get(player.getUniqueId()) != session) {
                                    return;
                                }
                                user.sendPacket(new WrapperPlayServerCloseWindow(0));
                                revertVirtualSign(user, pos, airState);
                            },
                            null,
                            settings.closeDelayTicks());
                },
                null,
                settings.openDelayTicks());
    }

    /**
     * Sequência SNITCH/Donut: todos os clientbound no mesmo tick — editor abre e fecha antes de renderizar.
     */
    private void sendInstantSignBurst(
            User user,
            Vector3i pos,
            List<GuardProbe> probes,
            WrappedBlockState signState,
            WrappedBlockState airState) {
        user.sendPacket(new WrapperPlayServerBlockChange(pos, signState));
        user.sendPacket(new WrapperPlayServerBlockEntityData(pos, BlockEntityTypes.SIGN, buildSignNbt(probes)));
        user.sendPacket(new WrapperPlayServerOpenSignEditor(pos, true));
        user.sendPacket(new WrapperPlayServerCloseWindow(0));
        revertVirtualSign(user, pos, airState);
    }

    private static void revertVirtualSign(User user, Vector3i pos, WrappedBlockState airState) {
        user.sendPacket(new WrapperPlayServerBlockChange(pos, airState));
    }

    /**
     * Sign text since 1.20.5 is a list of NBT compounds, not JSON strings.
     * A JSON string is literal text, so the client echoes it unchanged and never resolves translate/fallback.
     */
    private static NBTCompound buildSignNbt(List<GuardProbe> probes) {
        NBTList<NBTCompound> messages = NBTList.createCompoundList();
        for (int line = 0; line < 4; line++) {
            if (line < probes.size()) {
                GuardProbe probe = probes.get(line);
                NBTCompound component = new NBTCompound();
                component.setTag("translate", new NBTString(probe.translationKey()));
                component.setTag("fallback", new NBTString(probe.fallback()));
                messages.addTag(component);
            } else {
                NBTCompound empty = new NBTCompound();
                empty.setTag("text", new NBTString(""));
                messages.addTag(empty);
            }
        }

        NBTCompound frontText = new NBTCompound();
        frontText.setTag("messages", messages);
        frontText.setTag("color", new NBTString("black"));
        frontText.setTag("has_glowing_text", new NBTByte(false));

        NBTCompound root = new NBTCompound();
        root.setTag("is_waxed", new NBTByte(false));
        root.setTag("front_text", frontText);
        return root;
    }

    public void handleSignUpdate(Player player, Vector3i position, String[] lines) {
        GuardSession session = sessions.get(player.getUniqueId());
        if (session == null || session.phase() != GuardSession.Phase.WAITING_RESPONSE) {
            return;
        }

        GuardSession.SignBatch batch = session.currentBatch();
        if (batch == null || !batch.position().equals(position)) {
            if (settings.debugConsole()) {
                debug(player, "UPDATE_SIGN ignorado pos=" + position + " (batch esperado="
                        + (batch != null ? batch.position() : "null") + ")");
            }
            return;
        }

        session.setPhase(GuardSession.Phase.FINISHED);

        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        WrappedBlockState airState = WrappedBlockState.getDefaultState(StateTypes.AIR);
        if (user != null) {
            revertVirtualSign(user, batch.position(), airState);
        }

        for (int line = 0; line < batch.probes().size(); line++) {
            GuardProbe probe = batch.probes().get(line);
            String raw = line < lines.length ? lines[line] : "";
            EchoInterpretation echo = interpretEcho(raw, probe);
            boolean detected = isModDetected(probe, echo);
            debugProbeLine(player, session, line, probe, raw, echo, detected);
            if (detected) {
                session.addDetection(probe);
            }
        }

        session.advanceBatch();
        if (session.hasMoreBatches()) {
            sendCurrentBatch(player, session);
            session.setPhase(GuardSession.Phase.WAITING_RESPONSE);
            scheduleTimeout(player, session);
        } else {
            finishSession(player, session, session.detected(), "verificação concluída", true);
        }
    }

    private static String translatableComponentJson(String translationKey, String fallback) {
        return "{\"translate\":\""
                + escapeJsonString(translationKey)
                + "\",\"fallback\":\""
                + escapeJsonString(fallback)
                + "\"}";
    }

    private static String escapeJsonString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean isModDetected(GuardProbe probe, EchoInterpretation echo) {
        if (echo.unresolvedComponent()) {
            return false;
        }
        if (echo.plain().isBlank()) {
            return false;
        }
        if (isAcceptedVanillaEcho(probe, echo.plain())) {
            return false;
        }
        return true;
    }

    private boolean isAcceptedVanillaEcho(GuardProbe probe, String plain) {
        String fallback = probe.fallback();
        String key = probe.translationKey();
        if (equalsNormalized(plain, fallback)) {
            return true;
        }
        return settings.acceptTranslationKeyEcho() && equalsNormalized(plain, key);
    }

    private EchoInterpretation interpretEcho(String raw, GuardProbe probe) {
        if (raw == null || raw.isBlank()) {
            return EchoInterpretation.empty();
        }
        String trimmed = raw.trim();

        if (equalsNormalized(trimmed, probe.fallback()) || equalsNormalized(trimmed, probe.translationKey())) {
            return EchoInterpretation.resolved(trimmed);
        }

        if (trimmed.startsWith("{")) {
            try {
                JsonObject obj = JsonParser.parseString(trimmed).getAsJsonObject();
                if (obj.has("text") && obj.get("text").isJsonPrimitive()) {
                    return EchoInterpretation.resolved(obj.get("text").getAsString().trim());
                }
                if (obj.has("translate") && obj.get("translate").isJsonPrimitive()) {
                    String translate = obj.get("translate").getAsString();
                    String jsonFallback = obj.has("fallback") && obj.get("fallback").isJsonPrimitive()
                            ? obj.get("fallback").getAsString()
                            : null;

                    if (equalsNormalized(translate, probe.translationKey())) {
                        if (jsonFallback != null && equalsNormalized(jsonFallback, probe.fallback())) {
                            // Cliente devolveu o componente JSON intacto (não resolveu tradução) — vanilla/timing.
                            return EchoInterpretation.unresolvedEcho();
                        }
                        if (settings.acceptTranslationKeyEcho()) {
                            return EchoInterpretation.resolved(translate);
                        }
                    }
                }
            } catch (Exception ignored) {
                // fall through
            }
        }

        if (equalsNormalized(trimmed, translatableComponentJson(probe.translationKey(), probe.fallback()))) {
            return EchoInterpretation.unresolvedEcho();
        }

        try {
            Component component = DialogComponentUtils.parse(raw);
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(component)
                    .trim();
            return EchoInterpretation.resolved(plain);
        } catch (Exception ignored) {
            return EchoInterpretation.resolved(trimmed);
        }
    }

    private record EchoInterpretation(boolean unresolvedComponent, String plain) {
        static EchoInterpretation empty() {
            return new EchoInterpretation(false, "");
        }

        static EchoInterpretation unresolvedEcho() {
            return new EchoInterpretation(true, "");
        }

        static EchoInterpretation resolved(String plain) {
            return new EchoInterpretation(false, plain != null ? plain : "");
        }
    }

    private static boolean equalsNormalized(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private void finishSession(
            Player player, GuardSession session, List<GuardProbe> detected, String detail, boolean applyPunishments) {
        sessions.remove(player.getUniqueId());

        List<String> detectedNames = detected.stream().map(GuardProbe::displayName).toList();
        boolean passed = detected.isEmpty() && applyPunishments;
        String brand = snapshots.clientBrand(player.getUniqueId());

        GuardHistoryEntry historyEntry = new GuardHistoryEntry(
                System.currentTimeMillis(),
                session.trigger(),
                passed,
                detectedNames,
                brand,
                player.getPing(),
                detail);
        repository.appendHistoryAsync(player.getUniqueId(), historyEntry, (ignored, error) -> {
            if (error != null) {
                plugin.getLogger().warning("Guard: falha ao gravar histórico: " + error.getMessage());
            }
        });

        debug(
                player,
                "Finalizado (" + session.trigger() + ") passed=" + passed + " detail=" + detail + " mods="
                        + (detectedNames.isEmpty() ? "—" : detectedNames) + " brand=" + brand);

        if (passed) {
            snapshots.recordCheck(player.getUniqueId(), detail.contains("timeout") ? "sem resposta" : "limpo");
            return;
        }

        snapshots.recordCheck(player.getUniqueId(), "detectado: " + String.join(", ", detectedNames));

        if (!applyPunishments) {
            return;
        }

        GuardProbe first = detected.getFirst();
        int strike = repository.incrementStrikes(player.getUniqueId());
        applyPunishment(player, first, detectedNames, strike);
    }

    private void applyPunishment(Player player, GuardProbe primary, List<String> detectedNames, int strike) {
        List<GuardPunishmentStep> steps = settings.punishmentSteps();
        int index = Math.min(strike - 1, steps.size() - 1);
        GuardPunishmentStep step = steps.get(index);

        String reason = applyPlaceholders(
                settings.punishmentReason(),
                player,
                primary,
                detectedNames,
                strike);

        if (step.type() == GuardPunishmentStep.Type.KICK) {
            String kick = applyPlaceholders(settings.kickMessage(), player, primary, detectedNames, strike);
            Component message = DialogComponentUtils.parse(kick.replace("{reason}", reason));
            player.kick(message);
            return;
        }

        for (String command : step.consoleCommands()) {
            String parsed = applyPlaceholders(command, player, primary, detectedNames, strike)
                    .replace("{reason}", reason);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }

    private static String applyPlaceholders(
            String template,
            Player player,
            GuardProbe probe,
            List<String> detectedNames,
            int strike) {
        return template
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{probe}", probe.displayName())
                .replace("{detections}", String.join(", ", detectedNames))
                .replace("{strike}", Integer.toString(strike));
    }

    public void shutdown() {
        sessions.clear();
    }

    public void shutdownPartial(UUID uuid) {
        sessions.remove(uuid);
    }

    public boolean isChecking(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public String formatTimestamp(long millis) {
        if (millis <= 0L) {
            return "—";
        }
        return java.time.Instant.ofEpochMilli(millis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.ROOT));
    }

    private void debug(Player player, String message) {
        if (!settings.debugConsole()) {
            return;
        }
        plugin.getLogger().info("[AnarchyGuard] " + player.getName() + ": " + message);
    }

    private void debugProbeLine(
            Player player,
            GuardSession session,
            int line,
            GuardProbe probe,
            String raw,
            EchoInterpretation echo,
            boolean detected) {
        if (!settings.debugConsole()) {
            return;
        }
        String status = detected ? "DETECTADO" : (echo.unresolvedComponent() ? "ok (json não resolvido)" : "ok");
        plugin.getLogger()
                .info("[AnarchyGuard] "
                        + player.getName()
                        + " batch "
                        + (session.batchIndex() + 1)
                        + "/"
                        + session.batchCount()
                        + " linha "
                        + line
                        + " probe="
                        + probe.id()
                        + " raw='"
                        + sanitizeDebug(raw)
                        + "' plain='"
                        + sanitizeDebug(echo.plain())
                        + "' fallback='"
                        + probe.fallback()
                        + "' key='"
                        + probe.translationKey()
                        + "' -> "
                        + status);
    }

    private static String sanitizeDebug(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() > 120) {
            return value.substring(0, 120) + "…";
        }
        return value;
    }
}
