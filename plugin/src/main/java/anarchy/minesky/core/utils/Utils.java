package anarchy.minesky.core.utils;

import org.bukkit.ChatColor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final Pattern hexPattern = Pattern.compile("&#[A-Fa-f0-9]{6}");

    private static String createHex(String hexString) {
        hexString = hexString.replace("&", "");
        return net.md_5.bungee.api.ChatColor.of(hexString).toString();
    }

    public static String hex(String message) {
        Matcher matcher = hexPattern.matcher(message);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(result, createHex(matcher.group()));
        }

        matcher.appendTail(result);
        message = result.toString();

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static boolean isVanished(org.bukkit.entity.Player player) {
        if (player == null) {
            return false;
        }
        for (String key : List.of("vanished", "vanish")) {
            if (player.hasMetadata(key)) {
                return true;
            }
        }
        return false;
    }
}
