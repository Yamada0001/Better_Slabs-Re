package org.little100.better_slabs.model;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record VerticalHalf(@NotNull Material slabMaterial, @NotNull SlabFace face) {

    public VerticalHalf {
        Objects.requireNonNull(slabMaterial, "slabMaterial");
        Objects.requireNonNull(face, "face");
    }

    // 保持与旧 API 兼容的 getter 名称，避免大面积调用方改动
    public @NotNull Material getSlabMaterial() {
        return slabMaterial;
    }

    public @NotNull SlabFace getFace() {
        return face;
    }

    public boolean isVertical() {
        return face.isVertical();
    }

    public boolean isHorizontal() {
        return face.isHorizontal();
    }

}
