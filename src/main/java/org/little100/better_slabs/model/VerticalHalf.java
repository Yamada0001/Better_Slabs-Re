package org.little100.better_slabs.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class VerticalHalf {

    private final Material slabMaterial;
    private final SlabFace face;

    public VerticalHalf(Material slabMaterial, SlabFace face) {
        this.slabMaterial = Objects.requireNonNull(slabMaterial, "slabMaterial");
        this.face = Objects.requireNonNull(face, "face");
    }

    public Material getSlabMaterial() {
        return slabMaterial;
    }

    public SlabFace getFace() {
        return face;
    }

    public boolean isVertical() {
        return face.isVertical();
    }

    public boolean isHorizontal() {
        return face.isHorizontal();
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("slab", slabMaterial.name());
        map.put("face", face.name());
        return map;
    }

    public static VerticalHalf deserialize(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        Material material = Material.matchMaterial(section.getString("slab", ""));
        SlabFace face = SlabFace.fromString(section.getString("face", ""));
        if (material == null || face == null) {
            return null;
        }
        return new VerticalHalf(material, face);
    }

    public static VerticalHalf deserialize(Map<?, ?> map) {
        if (map == null) {
            return null;
        }
        Object slab = map.get("slab");
        Object faceObj = map.get("face");
        if (slab == null || faceObj == null) {
            return null;
        }
        Material material = Material.matchMaterial(String.valueOf(slab));
        SlabFace face = SlabFace.fromString(String.valueOf(faceObj));
        if (material == null || face == null) {
            return null;
        }
        return new VerticalHalf(material, face);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VerticalHalf that)) {
            return false;
        }
        return slabMaterial == that.slabMaterial && face == that.face;
    }

    @Override
    public int hashCode() {
        return Objects.hash(slabMaterial, face);
    }

    @Override
    public String toString() {
        return "VerticalHalf{" + slabMaterial + "@" + face + '}';
    }
}
