package org.little100.better_slabs.manager;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.little100.better_slabs.BetterSlabs;
import org.little100.better_slabs.model.SlabFace;
import org.little100.better_slabs.model.VerticalHalf;
import org.little100.better_slabs.model.VerticalSlabCell;

import java.util.ArrayList;
import java.util.List;

public final class SlabManager {

    private final BetterSlabs plugin;

    public SlabManager(BetterSlabs plugin) {
        this.plugin = plugin;
    }

    public boolean tryPlace(Player player, ItemStack hand, Block clicked, BlockFace clickedFace) {
        if (player == null || hand == null || clicked == null || clickedFace == null) {
            return false;
        }
        if (!player.hasPermission("betterslabs.place")) {
            return false;
        }

        boolean isVerticalItem = plugin.getSlabRegistry().isVerticalSlabItem(hand);
        Material slabMaterial = plugin.getSlabRegistry().getSlabFromItem(hand);
        if (slabMaterial == null || !plugin.getSlabRegistry().isSupported(slabMaterial)) {
            return false;
        }

        if (isVerticalItem) {
            return tryPlaceVerticalItem(player, hand, clicked, clickedFace, slabMaterial);
        }

        return tryHorizontalStack(player, hand, clicked, clickedFace, slabMaterial);
    }

    private boolean tryPlaceVerticalItem(Player player, ItemStack hand, Block clicked, BlockFace clickedFace, Material slabMaterial) {
        String clickedKey = VerticalSlabCell.keyOf(clicked.getLocation());
        VerticalSlabCell existingClicked = plugin.getSlabStorage().get(clickedKey);
        if (existingClicked != null && existingClicked.isVerticalCell() && existingClicked.size() == 1) {
            SlabFace have = existingClicked.getHalves().get(0).getFace();
            SlabFace need = have.opposite();
            return placeAt(player, hand, clicked, need, slabMaterial);
        }
        String fromHead = plugin.getCollisionManager().cellKeyFromHead(clicked);
        if (fromHead != null) {
            VerticalSlabCell headCell = plugin.getSlabStorage().get(fromHead);
            if (headCell != null && headCell.isVerticalCell() && headCell.size() == 1) {
                SlabFace have = headCell.getHalves().get(0).getFace();
                return placeAt(player, hand, headCell.toBlock(), have.opposite(), slabMaterial);
            }
        }

        SlabFace face;
        Block target = clicked.getRelative(clickedFace);

        if (clickedFace == BlockFace.UP || clickedFace == BlockFace.DOWN) {
            face = horizontalLook(player);
        } else {
            face = SlabFace.fromClickedFace(clickedFace);
            if (face == null || !face.isVertical()) {
                face = horizontalLook(player);
            }
        }

        String targetKey = VerticalSlabCell.keyOf(target.getLocation());
        VerticalSlabCell existingTarget = plugin.getSlabStorage().get(targetKey);
        if (existingTarget != null && existingTarget.isVerticalCell() && existingTarget.size() == 1) {
            SlabFace have = existingTarget.getHalves().get(0).getFace();
            if (!existingTarget.hasFace(face)) {
                if (have == face) {
                    face = have.opposite();
                }
            } else {
                face = have.opposite();
            }
            return placeAt(player, hand, target, face, slabMaterial);
        }

        return placeAt(player, hand, target, face, slabMaterial);
    }

    private SlabFace horizontalLook(Player player) {
        float yaw = player.getLocation().getYaw();
        yaw = (yaw % 360 + 360) % 360;
        if (yaw >= 315 || yaw < 45) {
            return SlabFace.SOUTH;
        }
        if (yaw < 135) {
            return SlabFace.WEST;
        }
        if (yaw < 225) {
            return SlabFace.NORTH;
        }
        return SlabFace.EAST;
    }

    private boolean placeAt(Player player, ItemStack hand, Block target, SlabFace face, Material slabMaterial) {
        if (target == null || face == null) {
            return false;
        }

        String key = VerticalSlabCell.keyOf(target.getLocation());
        VerticalSlabCell existing = plugin.getSlabStorage().get(key);

        if (existing == null) {
            Material type = target.getType();
            if (!type.isAir()) {
                boolean isHead = type == Material.PLAYER_HEAD || type == Material.PLAYER_WALL_HEAD;
                boolean isSingleSlab = type.name().endsWith("_SLAB")
                        && target.getBlockData() instanceof Slab slab
                        && slab.getType() != Slab.Type.DOUBLE;
                boolean isManagedBarrier = type == Material.BARRIER
                        && plugin.getCollisionManager().isManagedHost(target);
                if (!isHead && !isSingleSlab && !isManagedBarrier) {
                    return false;
                }
            }
        }

        VerticalSlabCell cell = existing != null ? existing : new VerticalSlabCell(target.getLocation());
        VerticalHalf half = new VerticalHalf(slabMaterial, face);

        // addHalf 内部已做 canAdd 校验，成功则执行放置，失败则直接结束
        if (cell.addHalf(half)) {
            return finalizePlacement(player, hand, target, cell, half);
        }

        return true;
    }

    /**
     * 提取公共的放置收尾逻辑：持久化、消耗物品、刷新世界状态、播放音效
     */
    private boolean finalizePlacement(Player player, ItemStack hand, Block target,
                                      VerticalSlabCell cell, VerticalHalf half) {
        plugin.getSlabStorage().put(cell);

        if (player.getGameMode() != GameMode.CREATIVE) {
            hand.setAmount(hand.getAmount() - 1);
        }

        applyWorldState(cell);
        player.playSound(target.getLocation(), Sound.BLOCK_STONE_PLACE, 1f, 1.05f);
        plugin.debug("Placed " + half + " at " + cell.key() + " halves=" + cell.size()
                + " full=" + cell.isFull() + " same=" + cell.isSameMaterialPair());
        return true;
    }

    private boolean tryHorizontalStack(Player player, ItemStack hand, Block clicked, BlockFace clickedFace, Material slabMaterial) {
        // 点在原版半砖上
        if (clicked.getBlockData() instanceof Slab existingSlab) {
            if (existingSlab.getType() == Slab.Type.DOUBLE) {
                return false;
            }
            boolean topClick = clickedFace == BlockFace.UP && existingSlab.getType() == Slab.Type.BOTTOM;
            boolean bottomClick = clickedFace == BlockFace.DOWN && existingSlab.getType() == Slab.Type.TOP;
            if (!topClick && !bottomClick) {
                return false;
            }
            Material existingMat = clicked.getType();
            if (existingMat == slabMaterial) {
                return false;
            }
            SlabFace existingFace = existingSlab.getType() == Slab.Type.TOP ? SlabFace.TOP : SlabFace.BOTTOM;
            SlabFace newFace = existingFace.opposite();

            VerticalSlabCell cell = new VerticalSlabCell(clicked.getLocation());
            cell.forceAddHalf(new VerticalHalf(existingMat, existingFace));
            VerticalHalf half = new VerticalHalf(slabMaterial, newFace);
            if (cell.addHalf(half)) {
                return finalizePlacement(player, hand, clicked, cell, half);
            }
            return false;
        }

        String key = VerticalSlabCell.keyOf(clicked.getLocation());
        VerticalSlabCell existing = plugin.getSlabStorage().get(key);
        if (existing != null && existing.isHorizontalCell() && !existing.isFull()) {
            SlabFace newFace = null;
            if (clickedFace == BlockFace.UP && existing.hasFace(SlabFace.BOTTOM)) {
                newFace = SlabFace.TOP;
            } else if (clickedFace == BlockFace.DOWN && existing.hasFace(SlabFace.TOP)) {
                newFace = SlabFace.BOTTOM;
            }
            if (newFace != null) {
                return placeAt(player, hand, clicked, newFace, slabMaterial);
            }
        }
        return false;
    }

    public void applyWorldState(VerticalSlabCell cell) {
        if (cell == null) {
            return;
        }
        plugin.getCollisionManager().apply(cell);
        VerticalSlabCell stored = plugin.getSlabStorage().get(cell.key());
        if (stored == null) {
            plugin.getDisplayManager().despawn(cell.key());
            return;
        }
        plugin.getDisplayManager().refresh(stored);
    }

    public void tryBreak(Player player, Block block) {
        if (block == null) {
            return;
        }
        String key = VerticalSlabCell.keyOf(block.getLocation());
        String fromHead = plugin.getCollisionManager().cellKeyFromHead(block);
        if (fromHead != null) {
            key = fromHead;
            VerticalSlabCell c = plugin.getSlabStorage().get(key);
            if (c != null) {
                Block b = c.toBlock();
                if (b != null) {
                    block = b;
                }
            }
        }
        VerticalSlabCell cell = plugin.getSlabStorage().get(key);
        if (cell == null || cell.isEmpty()) {
            return;
        }
        List<ItemStack> drops = new ArrayList<>(2);
        boolean wasVertical = cell.isVerticalCell();
        for (VerticalHalf half : cell.getHalves()) {
            if (wasVertical) {
                ItemStack v = plugin.getSlabRegistry().createVerticalSlabItem(half.getSlabMaterial(), 1);
                drops.add(v != null ? v : new ItemStack(half.getSlabMaterial(), 1));
            } else {
                drops.add(new ItemStack(half.getSlabMaterial(), 1));
            }
        }

        plugin.getDisplayManager().despawn(key);
        plugin.getCollisionManager().removeHost(key, block);
        cell.clear();
        plugin.getSlabStorage().remove(key);

        if (block.getType() != Material.AIR) {
            block.setType(Material.AIR, false);
        }

        if (player == null || player.getGameMode() != GameMode.CREATIVE) {
            Location dropAt = block.getLocation().add(0.5, 0.5, 0.5);
            for (ItemStack drop : drops) {
                block.getWorld().dropItemNaturally(dropAt, drop);
            }
        }

        if (player != null) {
            player.playSound(block.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 1f);
        }
    }
}
