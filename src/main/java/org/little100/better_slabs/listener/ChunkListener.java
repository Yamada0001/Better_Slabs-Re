package org.little100.better_slabs.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.little100.better_slabs.BetterSlabs;
import org.little100.better_slabs.model.VerticalSlabCell;

public final class ChunkListener implements Listener {

    private final BetterSlabs plugin;

    public ChunkListener(BetterSlabs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        int cx = event.getChunk().getX();
        int cz = event.getChunk().getZ();
        plugin.getScheduler().runAt(
                event.getWorld().getBlockAt(cx << 4, 64, cz << 4).getLocation(),
                () -> plugin.getDisplayManager().respawnChunk(event.getWorld(), cx, cz)
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        int cx = event.getChunk().getX();
        int cz = event.getChunk().getZ();
        for (VerticalSlabCell cell : plugin.getSlabStorage().getCellsInChunk(event.getWorld().getUID(), cx, cz)) {
            plugin.getDisplayManager().despawn(cell.key());
        }
    }
}
