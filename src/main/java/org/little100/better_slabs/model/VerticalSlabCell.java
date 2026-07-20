package org.little100.better_slabs.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class VerticalSlabCell {

    private final UUID worldId;
    private final int x;
    private final int y;
    private final int z;
    private final List<VerticalHalf> halves = new ArrayList<>(2);

    public VerticalSlabCell(UUID worldId, int x, int y, int z) {
        this.worldId = Objects.requireNonNull(worldId, "worldId");
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public VerticalSlabCell(Location location) {
        this(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public UUID getWorldId() {
        return worldId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String key() {
        return worldId + ":" + x + ":" + y + ":" + z;
    }

    public static String keyOf(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":"
                + location.getBlockY() + ":" + location.getBlockZ();
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldId);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z);
    }

    public Block toBlock() {
        Location location = toLocation();
        return location == null ? null : location.getBlock();
    }

    public List<VerticalHalf> getHalves() {
        return List.copyOf(halves);
    }

    public int size() {
        return halves.size();
    }

    public boolean isEmpty() {
        return halves.isEmpty();
    }

    public boolean isFull() {
        return halves.size() >= 2;
    }

    public boolean hasFace(SlabFace face) {
        for (VerticalHalf half : halves) {
            if (half.getFace() == face) {
                return true;
            }
        }
        return false;
    }

    public Optional<VerticalHalf> getHalf(SlabFace face) {
        for (VerticalHalf half : halves) {
            if (half.getFace() == face) {
                return Optional.of(half);
            }
        }
        return Optional.empty();
    }

    public boolean canAdd(VerticalHalf half) {
        if (half == null || isFull()) {
            return false;
        }
        if (hasFace(half.getFace())) {
            return false;
        }
        if (halves.isEmpty()) {
            return true;
        }
        VerticalHalf existing = halves.get(0);
        if (existing.isVertical() != half.isVertical()) {
            return false;
        }
        return existing.getFace().opposite() == half.getFace();
    }

    public boolean addHalf(VerticalHalf half) {
        if (!canAdd(half)) {
            return false;
        }
        halves.add(half);
        return true;
    }

    public void forceAddHalf(VerticalHalf half) {
        if (half == null || halves.size() >= 2) {
            return;
        }
        if (hasFace(half.getFace())) {
            return;
        }
        halves.add(half);
    }

    public boolean removeHalf(SlabFace face) {
        return halves.removeIf(h -> h.getFace() == face);
    }

    public void clear() {
        halves.clear();
    }

    public boolean isSameMaterialPair() {
        if (halves.size() != 2) {
            return false;
        }
        return halves.get(0).getSlabMaterial() == halves.get(1).getSlabMaterial();
    }

    public boolean isVerticalCell() {
        return !halves.isEmpty() && halves.get(0).isVertical();
    }

    public boolean isHorizontalCell() {
        return !halves.isEmpty() && halves.get(0).isHorizontal();
    }

    public Material firstSlabMaterial() {
        return halves.isEmpty() ? null : halves.get(0).getSlabMaterial();
    }

    public static BoundingBox halfBox(int x, int y, int z, SlabFace face) {
        return switch (face) {
            case NORTH -> new BoundingBox(x, y, z, x + 1, y + 1, z + 0.5);
            case SOUTH -> new BoundingBox(x, y, z + 0.5, x + 1, y + 1, z + 1);
            case WEST -> new BoundingBox(x, y, z, x + 0.5, y + 1, z + 1);
            case EAST -> new BoundingBox(x + 0.5, y, z, x + 1, y + 1, z + 1);
            case TOP -> new BoundingBox(x, y + 0.5, z, x + 1, y + 1, z + 1);
            case BOTTOM -> new BoundingBox(x, y, z, x + 1, y + 0.5, z + 1);
        };
    }

    public List<BoundingBox> solidBoxes() {
        List<BoundingBox> boxes = new ArrayList<>(halves.size());
        for (VerticalHalf half : halves) {
            boxes.add(halfBox(x, y, z, half.getFace()));
        }
        return boxes;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("world", worldId.toString());
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        List<Map<String, Object>> halfList = new ArrayList<>();
        for (VerticalHalf half : halves) {
            halfList.add(half.serialize());
        }
        map.put("halves", halfList);
        return map;
    }

    public static VerticalSlabCell deserialize(Map<?, ?> map) {
        if (map == null) {
            return null;
        }
        Object worldObj = map.get("world");
        if (worldObj == null) {
            return null;
        }
        UUID worldId;
        try {
            worldId = UUID.fromString(String.valueOf(worldObj));
        } catch (IllegalArgumentException e) {
            World world = Bukkit.getWorld(String.valueOf(worldObj));
            if (world == null) {
                return null;
            }
            worldId = world.getUID();
        }
        Object xObj = map.get("x");
        Object yObj = map.get("y");
        Object zObj = map.get("z");
        if (!(xObj instanceof Number) || !(yObj instanceof Number) || !(zObj instanceof Number)) {
            return null;
        }
        int x = ((Number) xObj).intValue();
        int y = ((Number) yObj).intValue();
        int z = ((Number) zObj).intValue();
        VerticalSlabCell cell = new VerticalSlabCell(worldId, x, y, z);
        Object halvesObj = map.get("halves");
        if (halvesObj instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> halfMap) {
                    VerticalHalf half = VerticalHalf.deserialize(halfMap);
                    if (half != null) {
                        cell.halves.add(half);
                    }
                }
            }
        }
        return cell.isEmpty() ? null : cell;
    }

    public static VerticalSlabCell deserialize(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        return deserialize(section.getValues(false));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VerticalSlabCell that)) {
            return false;
        }
        return x == that.x && y == that.y && z == that.z && worldId.equals(that.worldId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldId, x, y, z);
    }
}
