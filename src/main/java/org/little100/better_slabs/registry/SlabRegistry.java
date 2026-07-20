package org.little100.better_slabs.registry;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.little100.better_slabs.BetterSlabs;
import org.little100.better_slabs.util.Keys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SlabRegistry {

    private final BetterSlabs plugin;
    private final List<Material> ordered = new ArrayList<>();
    private final Set<Material> ignored = EnumSet.noneOf(Material.class);

    public SlabRegistry(BetterSlabs plugin) {
        this.plugin = plugin;
    }

    public void scan() {
        ordered.clear();
        ignored.clear();

        for (String raw : plugin.getConfig().getStringList("ignore-slabs")) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String name = raw.trim().toUpperCase(Locale.ROOT).replace("MINECRAFT:", "");
            Material material = Material.matchMaterial(name);
            if (material != null) {
                ignored.add(material);
            } else {
                plugin.getLogger().warning("ignore-slabs: unknown material '" + raw + "'");
            }
        }

        List<Material> found = new ArrayList<>();
        Set<Material> fromTag = collectFromVanillaTags();
        if (!fromTag.isEmpty()) {
            found.addAll(fromTag);
        } else {
            for (Material material : Material.values()) {
                if (isNameLikeSlab(material)) {
                    found.add(material);
                }
            }
        }

        found.removeIf(ignored::contains);
        found.sort((a, b) -> a.name().compareTo(b.name()));

        for (Material material : found) {
            if (isUsableSlab(material)) {
                ordered.add(material);
            }
        }

        plugin.getLogger().info("Registered " + ordered.size() + " vertical slab materials"
                + " (source: " + (fromTag.isEmpty() ? "name-fallback" : "vanilla-tags")
                + ", ignored: " + ignored.size() + ")");
    }

    private Set<Material> collectFromVanillaTags() {
        Set<Material> out = EnumSet.noneOf(Material.class);
        try {
            java.lang.reflect.Field field = Tag.class.getField("ITEMS_SLABS");
            @SuppressWarnings("unchecked")
            Tag<Material> tag = (Tag<Material>) field.get(null);
            if (tag != null) {
                out.addAll(tag.getValues());
            }
        } catch (Throwable ignored) {
        }
        try {
            if (Tag.SLABS != null) {
                out.addAll(Tag.SLABS.getValues());
            }
        } catch (Throwable ignored) {
        }
        out.removeIf(m -> m == null || !m.isItem() || !m.isBlock());
        return out;
    }

    public static boolean isNameLikeSlab(Material material) {
        if (material == null || material.isAir() || !material.isItem() || !material.isBlock()) {
            return false;
        }
        String name = material.name();
        if (!name.endsWith("_SLAB")) {
            return false;
        }
        return !name.contains("DOUBLE") && !name.startsWith("LEGACY_");
    }

    public static boolean isUsableSlab(Material material) {
        return material != null && !material.isAir() && material.isItem() && material.isBlock();
    }

    public boolean isIgnored(Material material) {
        return material != null && ignored.contains(material);
    }

    public boolean isSupported(Material material) {
        return ordered.contains(material);
    }

    public Set<Material> getSupportedMaterials() {
        return Collections.unmodifiableSet(EnumSet.copyOf(ordered));
    }

    public List<Material> getOrderedMaterials() {
        return Collections.unmodifiableList(ordered);
    }

    public int getSupportedCount() {
        return ordered.size();
    }

    public Material resolveFullBlock(Material slabMaterial) {
        if (slabMaterial == null) {
            return Material.STONE;
        }
        String name = slabMaterial.name();
        if (!name.endsWith("_SLAB")) {
            return slabMaterial;
        }
        String base = name.substring(0, name.length() - "_SLAB".length());

        Material special = switch (base) {
            case "OAK", "SPRUCE", "BIRCH", "JUNGLE", "ACACIA", "DARK_OAK",
                 "MANGROVE", "CHERRY", "BAMBOO", "CRIMSON", "WARPED" ->
                    Material.matchMaterial(base + "_PLANKS");
            case "PETRIFIED_OAK" -> Material.OAK_PLANKS;
            case "STONE" -> Material.STONE;
            case "SMOOTH_STONE" -> Material.SMOOTH_STONE;
            case "COBBLESTONE" -> Material.COBBLESTONE;
            case "MOSSY_COBBLESTONE" -> Material.MOSSY_COBBLESTONE;
            case "STONE_BRICK" -> Material.STONE_BRICKS;
            case "MOSSY_STONE_BRICK" -> Material.MOSSY_STONE_BRICKS;
            case "BRICK" -> Material.BRICKS;
            case "END_STONE_BRICK" -> Material.END_STONE_BRICKS;
            case "NETHER_BRICK" -> Material.NETHER_BRICKS;
            case "RED_NETHER_BRICK" -> Material.RED_NETHER_BRICKS;
            case "SANDSTONE" -> Material.SANDSTONE;
            case "CUT_SANDSTONE" -> Material.CUT_SANDSTONE;
            case "SMOOTH_SANDSTONE" -> Material.SMOOTH_SANDSTONE;
            case "RED_SANDSTONE" -> Material.RED_SANDSTONE;
            case "CUT_RED_SANDSTONE" -> Material.CUT_RED_SANDSTONE;
            case "SMOOTH_RED_SANDSTONE" -> Material.SMOOTH_RED_SANDSTONE;
            case "QUARTZ" -> Material.QUARTZ_BLOCK;
            case "SMOOTH_QUARTZ" -> Material.SMOOTH_QUARTZ;
            case "PRISMARINE" -> Material.PRISMARINE;
            case "PRISMARINE_BRICK" -> Material.PRISMARINE_BRICKS;
            case "DARK_PRISMARINE" -> Material.DARK_PRISMARINE;
            case "PURPUR" -> Material.PURPUR_BLOCK;
            case "BLACKSTONE" -> Material.BLACKSTONE;
            case "POLISHED_BLACKSTONE" -> Material.POLISHED_BLACKSTONE;
            case "POLISHED_BLACKSTONE_BRICK" -> Material.POLISHED_BLACKSTONE_BRICKS;
            case "CUT_COPPER", "EXPOSED_CUT_COPPER", "WEATHERED_CUT_COPPER", "OXIDIZED_CUT_COPPER",
                 "WAXED_CUT_COPPER", "WAXED_EXPOSED_CUT_COPPER", "WAXED_WEATHERED_CUT_COPPER",
                 "WAXED_OXIDIZED_CUT_COPPER" -> Material.matchMaterial(base);
            case "COBBLED_DEEPSLATE" -> Material.COBBLED_DEEPSLATE;
            case "POLISHED_DEEPSLATE" -> Material.POLISHED_DEEPSLATE;
            case "DEEPSLATE_BRICK" -> Material.DEEPSLATE_BRICKS;
            case "DEEPSLATE_TILE" -> Material.DEEPSLATE_TILES;
            case "TUFF" -> Material.matchMaterial("TUFF");
            case "POLISHED_TUFF" -> Material.matchMaterial("POLISHED_TUFF");
            case "TUFF_BRICK" -> Material.matchMaterial("TUFF_BRICKS");
            case "MUD_BRICK" -> Material.MUD_BRICKS;
            case "ANDESITE" -> Material.ANDESITE;
            case "POLISHED_ANDESITE" -> Material.POLISHED_ANDESITE;
            case "DIORITE" -> Material.DIORITE;
            case "POLISHED_DIORITE" -> Material.POLISHED_DIORITE;
            case "GRANITE" -> Material.GRANITE;
            case "POLISHED_GRANITE" -> Material.POLISHED_GRANITE;
            case "BAMBOO_MOSAIC" -> Material.BAMBOO_MOSAIC;
            default -> Material.matchMaterial(base);
        };

        if (special != null && special.isBlock()) {
            return special;
        }

        Material bricks = Material.matchMaterial(base + "S");
        if (bricks != null && bricks.isBlock()) {
            return bricks;
        }
        Material block = Material.matchMaterial(base + "_BLOCK");
        if (block != null && block.isBlock()) {
            return block;
        }
        return slabMaterial;
    }

    public ItemStack createVerticalSlabItem(Material slabMaterial, int amount) {
        if (!isSupported(slabMaterial)) {
            return null;
        }
        ItemStack stack = new ItemStack(slabMaterial, Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.getPersistentDataContainer().set(Keys.VERTICAL_SLAB, PersistentDataType.STRING, slabMaterial.name());
        try {
            java.util.List<net.kyori.adventure.text.Component> lore = plugin.getLangConfig().getStringList("item.vertical-slab-lore")
                    .stream()
                    .map(net.kyori.adventure.text.Component::text)
                    .collect(java.util.stream.Collectors.toList());
            meta.lore(lore);
        } catch (Throwable t) {
            try {
                meta.setLore(plugin.getLangConfig().getStringList("item.vertical-slab-lore"));
            } catch (Throwable ignored) {}
        }
        try {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
        } catch (Throwable t) {
            try {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.getByKey(
                        org.bukkit.NamespacedKey.minecraft("unbreaking")), 1, true);
            } catch (Throwable ignored) {}
        }
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        stack.setItemMeta(meta);
        return stack;
    }

    public ItemStack convertToNormalSlab(ItemStack verticalSlab) {
        if (!isVerticalSlabItem(verticalSlab)) {
            return null;
        }
        Material slabMaterial = getSlabFromItem(verticalSlab);
        if (slabMaterial == null) {
            return null;
        }
        return new ItemStack(slabMaterial, verticalSlab.getAmount());
    }

    public boolean isVerticalSlabItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(Keys.VERTICAL_SLAB, PersistentDataType.STRING);
    }

    public Material getSlabFromItem(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String stored = meta.getPersistentDataContainer().get(Keys.VERTICAL_SLAB, PersistentDataType.STRING);
            if (stored != null) {
                Material material = Material.matchMaterial(stored);
                if (material != null && isSupported(material)) {
                    return material;
                }
            }
        }
        Material type = stack.getType();
        return isSupported(type) ? type : null;
    }

    public static String prettyName(Material material) {
        String raw = material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = raw.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}