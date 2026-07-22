package org.little100.better_slabs.model;

import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 半砖六面方向枚举。
 * dx/dy/dz 及相关 getter/getDirection/isNorthSouth 已移除（全项目无调用）。
 */
public enum SlabFace {
    NORTH,
    SOUTH,
    WEST,
    EAST,
    TOP,
    BOTTOM;

    public boolean isVertical() {
        return this == NORTH || this == SOUTH || this == WEST || this == EAST;
    }

    public boolean isHorizontal() {
        return this == TOP || this == BOTTOM;
    }

    public @NotNull SlabFace opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case EAST -> WEST;
            case TOP -> BOTTOM;
            case BOTTOM -> TOP;
        };
    }

    public static @Nullable SlabFace fromBlockFace(@NotNull BlockFace face) {
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

    public static @Nullable SlabFace fromClickedFace(@NotNull BlockFace clickedFace) {
        SlabFace face = fromBlockFace(clickedFace);
        return face == null ? null : face.opposite();
    }

    public static @Nullable SlabFace fromString(@Nullable String name) {
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
