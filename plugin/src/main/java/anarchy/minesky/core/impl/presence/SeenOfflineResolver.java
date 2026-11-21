package anarchy.minesky.core.impl.presence;

import org.bukkit.OfflinePlayer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.OptionalLong;

public final class SeenOfflineResolver {

    private static final ZoneId DISPLAY_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("pt-BR"))
                    .withZone(DISPLAY_ZONE);

    private SeenOfflineResolver() {}

    public static OptionalLong offlineSinceMillis(PlayerPresenceRecord record, OfflinePlayer fallback) {
        if (record != null && record.lastQuitAtMillis() != null && record.lastQuitAtMillis() > 0) {
            return OptionalLong.of(record.lastQuitAtMillis());
        }
        long lastPlayed = fallback != null ? fallback.getLastPlayed() : 0L;
        if (lastPlayed > 0) {
            return OptionalLong.of(lastPlayed);
        }
        return OptionalLong.empty();
    }

    public static String formatTimestamp(long epochMillis) {
        return DISPLAY_FORMAT.format(Instant.ofEpochMilli(epochMillis));
    }
}