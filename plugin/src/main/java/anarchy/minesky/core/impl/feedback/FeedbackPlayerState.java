package anarchy.minesky.core.impl.feedback;

import anarchy.minesky.core.api.feedback.FeedbackPriority;
import net.kyori.adventure.text.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

final class FeedbackPlayerState {

    private final Deque<QueuedMessage> normalQueue = new ArrayDeque<>();
    private final Deque<QueuedMessage> lowQueue = new ArrayDeque<>();

    private int playbackGeneration;
    private List<Component> activeParts = List.of();
    private int partIndex;

    synchronized void enqueue(QueuedMessage message, FeedbackPriority priority) {
        switch (priority) {
            case HIGH -> {
                playbackGeneration++;
                normalQueue.clear();
                lowQueue.clear();
                activeParts = List.of();
                partIndex = 0;
                normalQueue.addFirst(message);
            }
            case NORMAL -> normalQueue.addLast(message);
            case LOW -> lowQueue.addLast(message);
        }
    }

    synchronized QueuedMessage pollNext() {
        QueuedMessage next = normalQueue.poll();
        if (next != null) {
            return next;
        }
        return lowQueue.poll();
    }

    synchronized boolean hasQueued() {
        return !normalQueue.isEmpty() || !lowQueue.isEmpty();
    }

    synchronized void clearAll() {
        playbackGeneration++;
        normalQueue.clear();
        lowQueue.clear();
        activeParts = List.of();
        partIndex = 0;
    }

    synchronized int generation() {
        return playbackGeneration;
    }

    synchronized void beginActive(List<Component> parts) {
        this.activeParts = parts;
        this.partIndex = 0;
    }

    synchronized boolean hasActiveParts() {
        return partIndex < activeParts.size();
    }

    synchronized Component currentPart() {
        if (partIndex >= activeParts.size()) {
            return null;
        }
        return activeParts.get(partIndex);
    }

    synchronized void advancePart() {
        partIndex++;
    }

    synchronized void finishActive() {
        activeParts = List.of();
        partIndex = 0;
    }

    synchronized boolean isBusy() {
        return hasActiveParts() || hasQueued();
    }

    record QueuedMessage(List<Component> parts, FeedbackPriority priority) {}
}
