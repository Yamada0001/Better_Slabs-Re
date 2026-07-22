package org.little100.better_slabs.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.little100.better_slabs.BetterSlabs;
import org.little100.better_slabs.model.VerticalSlabCell;

public final class NeighborUpdateListener implements Listener {

    // 实际为全部 6 个面，修正命名
    private static final BlockFace[] NEIGHBOR_FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
            BlockFace.UP, BlockFace.DOWN
    };

    private final BetterSlabs plugin;

    public NeighborUpdateListener(BetterSlabs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        refreshAround(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (plugin.getCollisionManager().isManagedHost(event.getBlock())
                || plugin.getSlabStorage().get(VerticalSlabCell.keyOf(event.getBlock().getLocation())) != null) {
            return;
        }
        refreshAround(event.getBlock());
    }

    private void refreshAround(Block center) {
        if (center == null) {
            return;
        }
        for (BlockFace face : NEIGHBOR_FACES) {
            Block neighbor = center.getRelative(face);
            String key = VerticalSlabCell.keyOf(neighbor.getLocation());
            VerticalSlabCell cell = plugin.getSlabStorage().get(key);
            if (cell == null || cell.isEmpty() || cell.isFull()) {
                continue;
            }
            Location loc = neighbor.getLocation();
            plugin.getScheduler().runAt(loc, () -> {
                try {
                    plugin.getDisplayManager().refresh(cell);
                } catch (Exception e) {
                    plugin.debug("refresh neighbor fail: " + e.getMessage());
                }
            });
        }
    }
}
