package anarchy.minesky.core.impl.guard;

import com.github.retrooper.packetevents.util.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class GuardSession {

    enum Phase {
        WAITING_RESPONSE,
        FINISHED
    }

    private final UUID playerId;
    private final String trigger;
    private final List<SignBatch> batches = new ArrayList<>();
    private int batchIndex;
    private Phase phase = Phase.WAITING_RESPONSE;
    private final List<GuardProbe> detected = new ArrayList<>();
    private long startedAtMillis = System.currentTimeMillis();

    GuardSession(UUID playerId, String trigger) {
        this.playerId = playerId;
        this.trigger = trigger;
    }

    void addBatch(Vector3i position, List<GuardProbe> probes) {
        batches.add(new SignBatch(position, List.copyOf(probes)));
    }

    boolean hasMoreBatches() {
        return batchIndex < batches.size();
    }

    SignBatch currentBatch() {
        if (batchIndex >= batches.size()) {
            return null;
        }
        return batches.get(batchIndex);
    }

    void advanceBatch() {
        batchIndex++;
        phase = Phase.WAITING_RESPONSE;
    }

    int batchIndex() {
        return batchIndex;
    }

    int batchCount() {
        return batches.size();
    }

    UUID playerId() {
        return playerId;
    }

    String trigger() {
        return trigger;
    }

    Phase phase() {
        return phase;
    }

    void setPhase(Phase phase) {
        this.phase = phase;
    }

    List<GuardProbe> detected() {
        return detected;
    }

    void addDetection(GuardProbe probe) {
        if (detected.stream().noneMatch(p -> p.id().equals(probe.id()))) {
            detected.add(probe);
        }
    }

    long startedAtMillis() {
        return startedAtMillis;
    }

    record SignBatch(Vector3i position, List<GuardProbe> probes) {}
}
