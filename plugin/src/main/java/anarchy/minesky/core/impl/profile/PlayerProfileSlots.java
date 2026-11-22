package anarchy.minesky.core.impl.profile;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Set;

/**
 * Layout fixo do baú de perfil (4×9 = 36 slots).
 */
public final class PlayerProfileSlots {

    public static final int SIZE = 36;
    public static final int BACK = 27;

    public enum StatCell {
        MONEY(10, Material.EMERALD),
        SHARDS(11, Material.AMETHYST_SHARD),
        KILLS(12, Material.DIAMOND_SWORD),
        DEATHS(13, Material.SKELETON_SKULL),
        PLAYTIME(14, Material.CLOCK),
        BLOCKS_PLACED(15, Material.STONE),
        BLOCKS_BROKEN(16, Material.COBBLESTONE),
        MOBS_KILLED(19, Material.ZOMBIE_HEAD),
        BACK_BUTTON(27, Material.RED_STAINED_GLASS_PANE);

        private final int slot;
        private final Material icon;

        StatCell(int slot, Material icon) {
            this.slot = slot;
            this.icon = icon;
        }

        public int slot() {
            return slot;
        }

        public Material icon() {
            return icon;
        }
    }

    private static final Set<Integer> STAT_SLOTS = EnumSet.allOf(StatCell.class).stream()
            .map(StatCell::slot)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private PlayerProfileSlots() {}

    public static boolean isStatSlot(int slot) {
        return STAT_SLOTS.contains(slot);
    }

    public static Set<Integer> allStatSlots() {
        return STAT_SLOTS;
    }

    public static int[] dataRowTwo() {
        return new int[] {10, 11, 12, 13, 14, 15, 16};
    }
}