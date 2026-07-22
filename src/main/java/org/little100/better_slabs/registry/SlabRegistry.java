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
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SlabRegistry {

    private final BetterSlabs plugin;
    private final List<Material> ordered = new ArrayList<>();
    private final Set<Material> ignored = EnumSet.noneOf(Material.class);
    // 缓存 resolveFullBlock 结果，避免每次调用都执行 switch + matchMaterial
    private final Map<Material, Material> fullBlockCache = new ConcurrentHashMap<>();

    public SlabRegistry(BetterSlabs plugin) {
        this.plugin = plugin;
    }

    public void scan() {
        ordered.clear();
        ignored.clear();
        fullBlockCache.clear();

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
        found.sort(Comparator.comparing(Material::name));

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
        // 使用 Tag.SLABS 获取原版台阶方块
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

    public boolean isSupported(Material material) {
        return ordered.contains(material);
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
        // 缓存命中直接返回，避免重复计算
        Material cached = fullBlockCache.get(slabMaterial);
        if (cached != null) {
            return cached;
        }
        Material result = doResolveFullBlock(slabMaterial);
        fullBlockCache.put(slabMaterial, result);
        return result;
    }

    private Material doResolveFullBlock(Material slabMaterial) {
        String name = slabMaterial.name();
        if (!name.endsWith("_SLAB")) {
            return slabMaterial;
        }
        String base = name.substring(0, name.length() - "_SLAB".length());

        // 仅保留与 default (matchMaterial(base)) 有实质差异的分支
        Material special = switch (base) {
            case "OAK", "SPRUCE", "BIRCH", "JUNGLE", "ACACIA", "DARK_OAK",
                 "MANGROVE", "CHERRY", "BAMBOO", "CRIMSON", "WARPED" ->
                    Material.matchMaterial(base + "_PLANKS");
            case "PETRIFIED_OAK" -> Material.OAK_PLANKS;
            case "STONE_BRICK" -> Material.STONE_BRICKS;
            case "MOSSY_STONE_BRICK" -> Material.MOSSY_STONE_BRICKS;
            case "BRICK" -> Material.BRICKS;
            case "END_STONE_BRICK" -> Material.END_STONE_BRICKS;
            case "NETHER_BRICK" -> Material.NETHER_BRICKS;
            case "RED_NETHER_BRICK" -> Material.RED_NETHER_BRICKS;
            case "CUT_SANDSTONE" -> Material.CUT_SANDSTONE;
            case "SMOOTH_SANDSTONE" -> Material.SMOOTH_SANDSTONE;
            case "RED_SANDSTONE" -> Material.RED_SANDSTONE;
            case "CUT_RED_SANDSTONE" -> Material.CUT_RED_SANDSTONE;
            case "SMOOTH_RED_SANDSTONE" -> Material.SMOOTH_RED_SANDSTONE;
            case "QUARTZ" -> Material.QUARTZ_BLOCK;
            case "SMOOTH_QUARTZ" -> Material.SMOOTH_QUARTZ;
            case "PRISMARINE_BRICK" -> Material.PRISMARINE_BRICKS;
            case "DARK_PRISMARINE" -> Material.DARK_PRISMARINE;
            case "PURPUR" -> Material.PURPUR_BLOCK;
            case "POLISHED_BLACKSTONE_BRICK" -> Material.POLISHED_BLACKSTONE_BRICKS;
            case "DEEPSLATE_BRICK" -> Material.DEEPSLATE_BRICKS;
            case "DEEPSLATE_TILE" -> Material.DEEPSLATE_TILES;
            case "TUFF_BRICK" -> Material.matchMaterial("TUFF_BRICKS");
            case "MUD_BRICK" -> Material.MUD_BRICKS;
            // 以下 case 的结果与 default (matchMaterial(base)) 相同，已移除:
            // STONE, SMOOTH_STONE, COBBLESTONE, MOSSY_COBBLESTONE, SANDSTONE,
            // PRISMARINE, BLACKSTONE, POLISHED_BLACKSTONE, COBBLED_DEEPSLATE,
            // POLISHED_DEEPSLATE, ANDESITE, POLISHED_ANDESITE, DIORITE,
            // POLISHED_DIORITE, GRANITE, POLISHED_GRANITE, BAMBOO_MOSAIC,
            // CUT_COPPER 系列, TUFF, POLISHED_TUFF
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
        // 仅使用 Adventure API 设置 lore，不再回退到已弃用的 setLore
        try {
            java.util.List<net.kyori.adventure.text.Component> lore = plugin.getLangConfig().getStringList("item.vertical-slab-lore")
                    .stream()
                    .map(net.kyori.adventure.text.Component::text)
                    .collect(java.util.stream.Collectors.toList());
            meta.lore(lore);
        } catch (Throwable ignored) {
        }
        // 附魔光效，处理 getByKey 可能返回 null
        try {
            org.bukkit.enchantments.Enchantment unbreaking = org.bukkit.enchantments.Enchantment.getByKey(
                    org.bukkit.NamespacedKey.minecraft("unbreaking"));
            if (unbreaking != null) {
                meta.addEnchant(unbreaking, 1, true);
            }
        } catch (Throwable ignored) {
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