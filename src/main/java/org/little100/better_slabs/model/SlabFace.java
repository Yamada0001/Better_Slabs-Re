package org.little100.better_slabs.model;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public enum SlabFace {
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    EAST(1, 0, 0),
    TOP(0, 1, 0),
    BOTTOM(0, -1, 0);

    private final int dx;
    private final int dy;
    private final int dz;

    SlabFace(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }

    public int getDz() {
        return dz;
    }

    public Vector getDirection() {
        return new Vector(dx, dy, dz);
    }

    public boolean isVertical() {
        return this == NORTH || this == SOUTH || this == WEST || this == EAST;
    }

    public boolean isHorizontal() {
        return this == TOP || this == BOTTOM;
    }

    public SlabFace opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case EAST -> WEST;
            case TOP -> BOTTOM;
            case BOTTOM -> TOP;
        };
    }

    public boolean isNorthSouth() {
        return this == NORTH || this == SOUTH;
    }

    public static SlabFace fromBlockFace(BlockFace face) {
        return switch (face) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            case UP -> TOP;
            case DOWN -> BOTTOM;
            default -> null;
        };
    }

    public static SlabFace fromClickedFace(BlockFace clickedFace) {
        SlabFace face = fromBlockFace(clickedFace);
        return face == null ? null : face.opposite();
    }

    public static SlabFace fromString(String name) {
        if (name == null) {
            return null;
        }
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
