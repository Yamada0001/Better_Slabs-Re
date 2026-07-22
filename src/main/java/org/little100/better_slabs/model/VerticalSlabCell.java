package org.little100.better_slabs.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VerticalSlabCell {

    private final UUID worldId;
    private final int x;
    private final int y;
    private final int z;
    // 使用同步 List 保障多线程读写安全，避免 ConcurrentModificationException
    private final List<VerticalHalf> halves = new ArrayList<>(2);
    private final Object halvesLock = new Object();

    public VerticalSlabCell(@NotNull UUID worldId, int x, int y, int z) {
        this.worldId = Objects.requireNonNull(worldId, "worldId");
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public VerticalSlabCell(@NotNull Location location) {
        this(requireWorldId(location),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private static @NotNull UUID requireWorldId(@NotNull Location location) {
        Objects.requireNonNull(location, "location");
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Location has no world");
        }
        return location.getWorld().getUID();
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

    public static @NotNull String keyOf(@NotNull Location location) {
        Objects.requireNonNull(location, "location");
        if (location.getWorld() == null) {
            return "null:" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
        }
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
        synchronized (halvesLock) {
            return List.copyOf(halves);
        }
    }

    public int size() {
        synchronized (halvesLock) {
            return halves.size();
        }
    }

    public boolean isEmpty() {
        synchronized (halvesLock) {
            return halves.isEmpty();
        }
    }

    public boolean isFull() {
        synchronized (halvesLock) {
            return halves.size() >= 2;
        }
    }

    public boolean hasFace(SlabFace face) {
        synchronized (halvesLock) {
            for (VerticalHalf half : halves) {
                if (half.getFace() == face) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean canAdd(VerticalHalf half) {
        if (half == null || isFull()) {
            return false;
        }
        synchronized (halvesLock) {
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
    }

    public boolean addHalf(VerticalHalf half) {
        if (!canAdd(half)) {
            return false;
        }
        synchronized (halvesLock) {
            halves.add(half);
        }
        return true;
    }

    public void forceAddHalf(VerticalHalf half) {
        if (half == null) {
            return;
        }
        synchronized (halvesLock) {
            if (halves.size() >= 2) {
                return;
            }
            if (hasFace(half.getFace())) {
                return;
            }
            halves.add(half);
        }
    }

    public void clear() {
        synchronized (halvesLock) {
            halves.clear();
        }
    }

    public boolean isSameMaterialPair() {
        synchronized (halvesLock) {
            if (halves.size() != 2) {
                return false;
            }
            return halves.get(0).getSlabMaterial() == halves.get(1).getSlabMaterial();
        }
    }

    public boolean isVerticalCell() {
        synchronized (halvesLock) {
            return !halves.isEmpty() && halves.get(0).isVertical();
        }
    }

    public boolean isHorizontalCell() {
        synchronized (halvesLock) {
            return !halves.isEmpty() && halves.get(0).isHorizontal();
        }
    }

    public Material firstSlabMaterial() {
        synchronized (halvesLock) {
            return halves.isEmpty() ? null : halves.get(0).getSlabMaterial();
        }
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

    @Override
    public boolean equals(@Nullable Object o) {
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
