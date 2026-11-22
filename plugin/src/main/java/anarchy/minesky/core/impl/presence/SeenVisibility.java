package anarchy.minesky.core.impl.presence;

import anarchy.minesky.core.utils.Utils;
import org.bukkit.entity.Player;

public final class SeenVisibility {

    private SeenVisibility() {}

  /**
   * Jogador conta como online para {@code /seen} se estiver visível ao viewer ou o viewer puder ver vanish.
   */
    public static boolean showAsOnline(Player target, Player viewer, boolean canSeeVanish) {
        if (target == null || viewer == null || !target.isOnline()) {
            return false;
        }
        if (canSeeVanish) {
            return true;
        }
        if (Utils.isVanished(target)) {
            return false;
        }
        return viewer.canSee(target);
    }
}