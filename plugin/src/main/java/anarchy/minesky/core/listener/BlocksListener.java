package anarchy.minesky.core.listener;

import anarchy.minesky.core.impl.blocks.BlockManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class BlocksListener implements Listener {

    private final BlockManager blocks;

    public BlocksListener(BlockManager blocks) {
        this.blocks = blocks;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        Location to = event.getTo();
        Location from = event.getFrom();
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        if (to.getWorld() == null) {
            return;
        }
        blocks.onPlayerBlockCellChange(
                event.getPlayer(),
                to.getWorld().getName(),
                to.getBlockX(),
                to.getBlockY(),
                to.getBlockZ());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getWorld() == null) {
            return;
        }
        blocks.onBlockClick(
                event.getPlayer(),
                clicked.getWorld().getName(),
                clicked.getX(),
                clicked.getY(),
                clicked.getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        blocks.handleQuit(event.getPlayer().getUniqueId());
    }
}
