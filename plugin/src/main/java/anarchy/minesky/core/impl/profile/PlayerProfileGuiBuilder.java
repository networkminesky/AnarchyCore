package anarchy.minesky.core.impl.profile;

import anarchy.minesky.core.utils.ItemRichText;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PlayerProfileGuiBuilder {

    private PlayerProfileGuiBuilder() {}

    public static ItemStack[] buildContents(PlayerProfileSnapshot profile) {
        ItemStack[] contents = new ItemStack[PlayerProfileSlots.SIZE];
        for (int slot = 0; slot < contents.length; slot++) {
            if (PlayerProfileSlots.isStatSlot(slot)) {
                continue;
            }
            contents[slot] = fillerPane();
        }
        contents[PlayerProfileSlots.StatCell.MONEY.slot()] =
                statItem(PlayerProfileSlots.StatCell.MONEY, "&aᴅɪɴʜᴇɪʀᴏ", formatMoney(profile.money()));
        contents[PlayerProfileSlots.StatCell.SHARDS.slot()] =
                statItem(PlayerProfileSlots.StatCell.SHARDS, "&dꜰʀᴀɢᴍᴇɴᴛᴏꜱ", PlayerProfileFormatter.formatShardsLine(profile.shards()));
        contents[PlayerProfileSlots.StatCell.KILLS.slot()] =
                statItem(PlayerProfileSlots.StatCell.KILLS, "&cᴀʙᴀᴛᴇꜱ", Integer.toString(profile.kills()));
        contents[PlayerProfileSlots.StatCell.DEATHS.slot()] =
                statItem(PlayerProfileSlots.StatCell.DEATHS, "&7ᴍᴏʀᴛᴇꜱ", Integer.toString(profile.deaths()));
        contents[PlayerProfileSlots.StatCell.PLAYTIME.slot()] =
                statItem(
                        PlayerProfileSlots.StatCell.PLAYTIME,
                        "&eᴛᴇᴍᴘᴏ ᴊᴏɢᴀᴅᴏ",
                        LeaderboardFormatBridge.playtime(profile.playtimeSeconds()));
        contents[PlayerProfileSlots.StatCell.BLOCKS_PLACED.slot()] =
                statItem(
                        PlayerProfileSlots.StatCell.BLOCKS_PLACED,
                        "&fʙʟᴏᴄᴏꜱ ᴄᴏʟᴏᴄᴀᴅᴏꜱ",
                        Integer.toString(profile.blocksPlaced()));
        contents[PlayerProfileSlots.StatCell.BLOCKS_BROKEN.slot()] =
                statItem(
                        PlayerProfileSlots.StatCell.BLOCKS_BROKEN,
                        "&8ʙʟᴏᴄᴏꜱ ꞯᴜᴇʙʀᴀᴅᴏꜱ",
                        Integer.toString(profile.blocksBroken()));
        contents[PlayerProfileSlots.StatCell.MOBS_KILLED.slot()] =
                statItem(
                        PlayerProfileSlots.StatCell.MOBS_KILLED,
                        "&2ᴍᴏʙꜱ ᴍᴏʀᴛᴏꜱ",
                        Integer.toString(profile.mobsKilled()));
        contents[PlayerProfileSlots.StatCell.BACK_BUTTON.slot()] = backButton();
        return contents;
    }

    private static ItemStack fillerPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.space());
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static ItemStack statItem(PlayerProfileSlots.StatCell cell, String title, String value) {
        ItemStack stack = new ItemStack(cell.icon());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(ItemRichText.parse(title));
            List<Component> lore = new ArrayList<>();
            lore.add(ItemRichText.parse("&f" + value));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack backButton() {
        ItemStack stack = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(ItemRichText.parse("&cᴠᴏʟᴛᴀʀ"));
            meta.lore(List.of(ItemRichText.parse("&7ᴠᴏʟᴛᴀʀ ᴀᴏ ᴘᴇʀꜰɪʟ ʀᴇꜱᴜᴍɪᴅᴏ")));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static String formatMoney(double money) {
        return "$ " + anarchy.minesky.core.impl.leaderboards.LeaderboardFormatter.formatCompactNumber(money);
    }

    public static String toSmallCaps(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        String lower = text.toLowerCase(Locale.ROOT);
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            sb.append(switch (c) {
                case 'a' -> 'ᴀ';
                case 'b' -> 'ʙ';
                case 'c' -> 'ᴄ';
                case 'd' -> 'ᴅ';
                case 'e' -> 'ᴇ';
                case 'f' -> 'ꜰ';
                case 'g' -> 'ɢ';
                case 'h' -> 'ʜ';
                case 'i' -> 'ɪ';
                case 'j' -> 'ᴊ';
                case 'k' -> 'ᴋ';
                case 'l' -> 'ʟ';
                case 'm' -> 'ᴍ';
                case 'n' -> 'ɴ';
                case 'o' -> 'ᴏ';
                case 'p' -> 'ᴘ';
                case 'q' -> 'ꞯ';
                case 'r' -> 'ʀ';
                case 's' -> 'ꜱ';
                case 't' -> 'ᴛ';
                case 'u' -> 'ᴜ';
                case 'v' -> 'ᴠ';
                case 'w' -> 'ᴡ';
                case 'x' -> 'x';
                case 'y' -> 'ʏ';
                case 'z' -> 'ᴢ';
                default -> c;
            });
        }
        return sb.toString();
    }

    /** Evita dependência circular nos testes — delega ao formatter de leaderboard. */
    public static final class LeaderboardFormatBridge {
        private LeaderboardFormatBridge() {}

        public static String playtime(long seconds) {
            return anarchy.minesky.core.impl.leaderboards.LeaderboardFormatter.formatPlaytime(seconds);
        }
    }

    public static String loreForCell(PlayerProfileSlots.StatCell cell, PlayerProfileSnapshot profile) {
        return switch (cell) {
            case MONEY -> formatMoney(profile.money());
            case SHARDS -> PlayerProfileFormatter.formatShardsLine(profile.shards());
            case KILLS -> Integer.toString(profile.kills());
            case DEATHS -> Integer.toString(profile.deaths());
            case PLAYTIME -> LeaderboardFormatBridge.playtime(profile.playtimeSeconds());
            case BLOCKS_PLACED -> Integer.toString(profile.blocksPlaced());
            case BLOCKS_BROKEN -> Integer.toString(profile.blocksBroken());
            case MOBS_KILLED -> Integer.toString(profile.mobsKilled());
            case BACK_BUTTON -> "ᴠᴏʟᴛᴀʀ";
        };
    }

    public static String slotTitle(PlayerProfileSlots.StatCell cell) {
        return switch (cell) {
            case MONEY -> "ᴅɪɴʜᴇɪʀᴏ";
            case SHARDS -> "ꜰʀᴀɢᴍᴇɴᴛᴏꜱ";
            case KILLS -> "ᴀʙᴀᴛᴇꜱ";
            case DEATHS -> "ᴍᴏʀᴛᴇꜱ";
            case PLAYTIME -> "ᴛᴇᴍᴘᴏ ᴊᴏɢᴀᴅᴏ";
            case BLOCKS_PLACED -> "ʙʟᴏᴄᴏꜱ ᴄᴏʟᴏᴄᴀᴅᴏꜱ";
            case BLOCKS_BROKEN -> "ʙʟᴏᴄᴏꜱ ꞯᴜᴇʙʀᴀᴅᴏꜱ";
            case MOBS_KILLED -> "ᴍᴏʙꜱ ᴍᴏʀᴛᴏꜱ";
            case BACK_BUTTON -> "ᴠᴏʟᴛᴀʀ";
        };
    }
}