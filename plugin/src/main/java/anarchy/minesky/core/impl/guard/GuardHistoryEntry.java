package anarchy.minesky.core.impl.guard;

import java.util.List;

public record GuardHistoryEntry(
        long timestampMillis,
        String trigger,
        boolean passed,
        List<String> detectedMods,
        String clientBrand,
        int ping,
        String detail) {}
