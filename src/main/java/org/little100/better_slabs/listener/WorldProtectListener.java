package org.little100.better_slabs.listener;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.little100.better_slabs.BetterSlabs;
import org.little100.better_slabs.model.VerticalSlabCell;

import java.util.Iterator;
import java.util.List;

public final class WorldProtectListener implements Listener {

    // 提取为静态常量，避免每次调用都新建数组
    private static final BlockFace[] ALL_FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
            BlockFace.UP, BlockFace.DOWN
    };

    private final BetterSlabs plugin;

    public WorldProtectListener(BetterSlabs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplodeList(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplodeList(event.blockList());
    }

    private void handleExplodeList(List<Block> blocks) {
        Iterator<Block> it = blocks.iterator();
        while (it.hasNext()) {
            Block block = it.next();
            VerticalSlabCell cell = plugin.getSlabStorage().get(VerticalSlabCell.keyOf(block.getLocation()));
            if (cell == null || cell.isEmpty()) {
                continue;
            }
            it.remove();
            plugin.getScheduler().runAt(block.getLocation(), () ->
                    plugin.getSlabManager().tryBreak(null, block));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (touchesManaged(event.getBlocks()) || isManaged(event.getBlock().getRelative(event.getDirection()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (touchesManaged(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (isManaged(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    private boolean touchesManaged(List<Block> blocks) {
        for (Block block : blocks) {
            if (isManaged(block)) {
                return true;
            }
            for (BlockFace face : ALL_FACES) {
                if (isManaged(block.getRelative(face))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isManaged(Block block) {
        if (block == null) {
            return false;
        }
        return plugin.getSlabStorage().get(VerticalSlabCell.keyOf(block.getLocation())) != null;
    }
}
