package anarchy.minesky.core.impl.dialogapi;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public final class BedrockPlatformDetector {

    private BedrockPlatformDetector() {}

    public static boolean isBedrock(Player player) {
        if (player == null) return false;
        if (isMathematicallyBedrock(player.getUniqueId())) {
            return true;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
            return false;
        }
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (NoClassDefFoundError | Exception ignored) {
            return false;
        }
    }

    /** Floodgate-style UUID prefix used by Geyser/Bedrock proxies. */
    public static boolean isMathematicallyBedrock(java.util.UUID uuid) {
        return uuid != null && uuid.toString().startsWith("00000000-0000-0000-");
    }
}