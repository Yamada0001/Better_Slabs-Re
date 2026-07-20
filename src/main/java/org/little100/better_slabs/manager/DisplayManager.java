package org.little100.better_slabs.manager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.little100.better_slabs.BetterSlabs;
import org.little100.better_slabs.model.SlabFace;
import org.little100.better_slabs.model.VerticalHalf;
import org.little100.better_slabs.model.VerticalSlabCell;
import org.little100.better_slabs.util.Keys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DisplayManager {

    private static final float HALF_PI = (float) (Math.PI / 2.0);

    private final BetterSlabs plugin;
    private final Map<String, List<DisplayEntity>> displays = new ConcurrentHashMap<>();

    private static final class DisplayEntity {
        final UUID entityId;
        final UUID worldId;

        DisplayEntity(UUID entityId, UUID worldId) {
            this.entityId = entityId;
            this.worldId = worldId;
        }
    }

    public DisplayManager(BetterSlabs plugin) {
        this.plugin = plugin;
    }

    public void refresh(VerticalSlabCell cell) {
        if (cell == null) {
            return;
        }
        despawn(cell.key());
        if (cell.isEmpty()) {
            return;
        }
        Location base = cell.toLocation();
        if (base == null || base.getWorld() == null) {
            return;
        }

        if (cell.isFull() && cell.isSameMaterialPair()) {
            return;
        }

        boolean dualDifferent = cell.isFull() && !cell.isSameMaterialPair();
        boolean single = !cell.isFull();

        if (!dualDifferent && !single) {
            return;
        }

        List<DisplayEntity> entities = new ArrayList<>();
        for (VerticalHalf half : cell.getHalves()) {
            BlockDisplay display = spawnHalf(base, half, dualDifferent);
            if (display != null) {
                entities.add(new DisplayEntity(display.getUniqueId(), base.getWorld().getUID()));
            }
        }
        if (!entities.isEmpty()) {
            displays.put(cell.key(), entities);
        }
    }

    private BlockDisplay spawnHalf(Location cellOrigin, VerticalHalf half, boolean dual) {
        World world = cellOrigin.getWorld();
        if (world == null) {
            return null;
        }

        Material slabMat = half.getSlabMaterial();
        BlockData data = createSlabData(slabMat, half.getFace());
        Location spawn = cellOrigin.clone();

        BlockDisplay display = (BlockDisplay) world.spawnEntity(spawn, EntityType.BLOCK_DISPLAY);
        display.setBlock(data);
        display.setShadowRadius(0f);
        display.setShadowStrength(0f);
        display.setViewRange(1.0f);
        display.setPersistent(true);
        display.setGravity(false);
        display.setInvulnerable(true);
        display.setSilent(true);
        display.getPersistentDataContainer().set(Keys.DISPLAY_MARKER, PersistentDataType.BYTE, (byte) 1);

        float out = 0f;
        if (!dual && half.getFace().isVertical()) {
            out = 0.1f;
        }

        TransformSpec spec = transformFor(half.getFace(), out, dual);
        display.setTransformation(new Transformation(
                spec.translation,
                spec.leftRotation,
                spec.scale,
                new AxisAngle4f(0, 0, 1, 0)
        ));
        display.setInterpolationDuration(0);
        display.setInterpolationDelay(0);
        return display;
    }

    private BlockData createSlabData(Material slabMat, SlabFace face) {
        BlockData data = slabMat.createBlockData();
        if (data instanceof Slab slab) {
            if (face == SlabFace.TOP) {
                slab.setType(Slab.Type.TOP);
            } else {
                slab.setType(Slab.Type.BOTTOM);
            }
            return slab;
        }
        return data;
    }
    
    private TransformSpec transformFor(SlabFace face, float out, boolean dual) {
        float backShift = dual ? 0f : 0.01f;
        Vector3f scale;
        AxisAngle4f rot;
        Vector3f translation;

        if (face.isVertical()) {
            scale = dual ? new Vector3f(1f, 1f, 1f) : new Vector3f(1f, 1f + out, 1f);
        } else {
            scale = dual ? new Vector3f(1f, 1f, 1f) : new Vector3f(1f, 1f + out, 1f);
        }

        switch (face) {
            case NORTH -> {
                rot = new AxisAngle4f(HALF_PI, 1f, 0f, 0f);
                translation = new Vector3f(0f, 1f, -backShift);
            }
            case SOUTH -> {
                rot = new AxisAngle4f(-HALF_PI, 1f, 0f, 0f);
                translation = new Vector3f(0f, 0f, 1f + backShift);
            }
            case WEST -> {
                rot = new AxisAngle4f(-HALF_PI, 0f, 0f, 1f);
                translation = new Vector3f(-backShift, 1f, 0f);
            }
            case EAST -> {
                rot = new AxisAngle4f(HALF_PI, 0f, 0f, 1f);
                translation = new Vector3f(1f + backShift, 0f, 0f);
            }
            case TOP -> {
                rot = new AxisAngle4f(0, 0, 1, 0);
                translation = new Vector3f(0f, out, 0f);
            }
            case BOTTOM -> {
                rot = new AxisAngle4f(0, 0, 1, 0);
                translation = new Vector3f(0f, -out, 0f);
            }
            default -> {
                rot = new AxisAngle4f(0, 0, 1, 0);
                translation = new Vector3f(0f, 0f, 0f);
            }
        }
        return new TransformSpec(translation, rot, scale);
    }

    public void despawn(String cellKey) {
        List<DisplayEntity> entities = displays.remove(cellKey);
        if (entities == null) {
            return;
        }
        for (DisplayEntity de : entities) {
            try {
                World world = plugin.getServer().getWorld(de.worldId);
                if (world != null) {
                    Entity entity = world.getEntity(de.entityId);
                    if (entity != null) {
                        entity.remove();
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    public void despawnAll() {
        for (String key : new ArrayList<>(displays.keySet())) {
            try {
                despawn(key);
            } catch (Exception ignored) {
            }
        }
        displays.clear();
        try {
            for (World world : plugin.getServer().getWorlds()) {
                if (world == null) {
                    continue;
                }
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof BlockDisplay
                            && entity.getPersistentDataContainer().has(Keys.DISPLAY_MARKER, PersistentDataType.BYTE)) {
                        try {
                            entity.remove();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void respawnAllLoaded() {
        for (VerticalSlabCell cell : plugin.getSlabStorage().all()) {
            try {
                Location loc = cell.toLocation();
                if (loc == null || loc.getWorld() == null) {
                    continue;
                }
                final VerticalSlabCell c = cell;
                final Location l = loc.clone();
                plugin.getScheduler().runAt(l, () -> {
                    try {
                        if (!l.getWorld().isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4)) {
                            return;
                        }
                        plugin.getCollisionManager().apply(c);
                        refresh(c);
                    } catch (Throwable e) {
                        plugin.debug("respawn fail: " + e.getMessage());
                    }
                });
            } catch (Throwable e) {
                plugin.debug("respawn schedule fail: " + e.getMessage());
            }
        }
    }

    public void respawnChunk(World world, int chunkX, int chunkZ) {
        for (VerticalSlabCell cell : plugin.getSlabStorage().all()) {
            if (!cell.getWorldId().equals(world.getUID())) {
                continue;
            }
            if ((cell.getX() >> 4) != chunkX || (cell.getZ() >> 4) != chunkZ) {
                continue;
            }
            Location loc = cell.toLocation();
            if (loc == null) {
                continue;
            }
            final VerticalSlabCell c = cell;
            plugin.getScheduler().runAt(loc, () -> {
                try {
                    plugin.getCollisionManager().apply(c);
                    refresh(c);
                } catch (Exception e) {
                    plugin.debug("respawn chunk fail: " + e.getMessage());
                }
            });
        }
    }

    private static final class TransformSpec {
        final Vector3f translation;
        final AxisAngle4f leftRotation;
        final Vector3f scale;

        TransformSpec(Vector3f translation, AxisAngle4f leftRotation, Vector3f scale) {
            this.translation = translation;
            this.leftRotation = leftRotation;
            this.scale = scale;
        }
    }
}
