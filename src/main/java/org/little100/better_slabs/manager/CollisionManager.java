package org.little100.better_slabs.manager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.little100.better_slabs.BetterSlabs;
import org.little100.better_slabs.model.SlabFace;
import org.little100.better_slabs.model.VerticalHalf;
import org.little100.better_slabs.model.VerticalSlabCell;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CollisionManager {

    private final BetterSlabs plugin;
    private final Set<String> hostKeys = ConcurrentHashMap.newKeySet();
    private final Map<String, String> headBlockKeys = new ConcurrentHashMap<>();
    private final Map<String, String> headToCell = new ConcurrentHashMap<>();

    public CollisionManager(BetterSlabs plugin) {
        this.plugin = plugin;
    }

    public void apply(VerticalSlabCell cell) {
        if (cell == null) {
            return;
        }
        try {
            applyOnRegion(cell);
        } catch (Throwable t) {
            plugin.debug("collision apply: " + t.getMessage());
        }
    }

    private void applyOnRegion(VerticalSlabCell cell) {
        if (cell.isEmpty()) {
            clearHeadForCell(cell.key());
            clearHostLocal(cell.key(), cell.toBlock());
            return;
        }

        Block block = cell.toBlock();
        if (block == null) {
            return;
        }

        // 完整方块
        if (cell.isFull() && cell.isSameMaterialPair()) {
            clearHeadSync(cell.key(), block);
            Material full = plugin.getSlabRegistry().resolveFullBlock(cell.firstSlabMaterial());
            if (block.getType() != full) {
                block.setType(full, false);
            }
            hostKeys.add(cell.key());
            plugin.getDisplayManager().despawn(cell.key());
            return;
        }

        // 不同材质用屏障
        if (cell.isFull() && !cell.isSameMaterialPair()) {
            clearHeadSync(cell.key(), block);
            if (block.getType() != Material.BARRIER) {
                block.setType(Material.BARRIER, false);
            }
            hostKeys.add(cell.key());
            return;
        }

        clearHeadSync(cell.key(), block);
        hostKeys.remove(cell.key());
        Material type = block.getType();
        // 单半砖场景：清除原占位方块（屏障/头颅/原版半砖/其它非空气方块）
        if (type != Material.AIR) {
            block.setType(Material.AIR, false);
        }
        placeHeadForSingle(cell);
    }

    private void placeHeadForSingle(VerticalSlabCell cell) {
        clearHeadForCell(cell.key());
        if (cell.size() != 1) {
            return;
        }
        VerticalHalf half = cell.getHalves().get(0);
        Block cellBlock = cell.toBlock();
        if (cellBlock == null) {
            return;
        }

        SlabFace face = half.getFace();
        Material headMat;
        BlockFace facing;

        if (face.isVertical()) {
            headMat = Material.PLAYER_WALL_HEAD;
            facing = toBlockFace(face.opposite());
        } else {
            headMat = Material.PLAYER_HEAD;
            facing = BlockFace.NORTH;
        }

        try {
            cellBlock.setType(headMat, false);
            var data = cellBlock.getBlockData();
            if (data instanceof Directional directional && face.isVertical()) {
                directional.setFacing(facing);
                cellBlock.setBlockData(directional, false);
            } else if (data instanceof Rotatable rotatable) {
                rotatable.setRotation(BlockFace.NORTH);
                cellBlock.setBlockData(rotatable, false);
            }
        } catch (Exception e) {
            try {
                cellBlock.setType(Material.PLAYER_HEAD, false);
            } catch (Exception ignored) {
                return;
            }
        }

        String headKey = VerticalSlabCell.keyOf(cellBlock.getLocation());
        headBlockKeys.put(cell.key(), headKey);
        headToCell.put(headKey, cell.key());
        hostKeys.add(cell.key());
    }

    private BlockFace toBlockFace(SlabFace face) {
        return switch (face) {
            case NORTH -> BlockFace.NORTH;
            case SOUTH -> BlockFace.SOUTH;
            case WEST -> BlockFace.WEST;
            case EAST -> BlockFace.EAST;
            case TOP -> BlockFace.UP;
            case BOTTOM -> BlockFace.DOWN;
        };
    }

    private void clearHeadSync(String cellKey, Block cellBlock) {
        String headKey = headBlockKeys.remove(cellKey);
        if (headKey != null) {
            headToCell.remove(headKey);
        }
        
        if (cellBlock != null) {
            try {
                Material t = cellBlock.getType();
                if (t == Material.PLAYER_HEAD || t == Material.PLAYER_WALL_HEAD) {
                    cellBlock.setType(Material.AIR, false);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void clearHeadForCell(String cellKey) {
        String headKey = headBlockKeys.remove(cellKey);
        if (headKey == null) {
            return;
        }
        headToCell.remove(headKey);
        try {
            String[] p = headKey.split(":");
            if (p.length < 4) {
                return;
            }
            java.util.UUID worldId = java.util.UUID.fromString(p[0]);
            int x = Integer.parseInt(p[1]);
            int y = Integer.parseInt(p[2]);
            int z = Integer.parseInt(p[3]);
            org.bukkit.World world = plugin.getServer().getWorld(worldId);
            if (world == null) {
                return;
            }
            Block b = world.getBlockAt(x, y, z);
            Material t = b.getType();
            if (t == Material.PLAYER_HEAD || t == Material.PLAYER_WALL_HEAD) {
                b.setType(Material.AIR, false);
            }
        } catch (Exception ignored) {
        }
    }

    private void clearHostLocal(String key, Block block) {
        if (key != null) {
            hostKeys.remove(key);
            clearHeadSync(key, block);
        }
        if (block == null) {
            return;
        }
        try {
            Material t = block.getType();
            if (t == Material.BARRIER || t == Material.PLAYER_HEAD || t == Material.PLAYER_WALL_HEAD) {
                block.setType(Material.AIR, false);
            }
        } catch (Exception ignored) {
        }
    }

    public void removeHost(String key, Block block) {
        if (key != null) {
            hostKeys.remove(key);
            clearHeadForCell(key);
        }
        if (block != null) {
            Location loc = block.getLocation();
            plugin.getScheduler().runAt(loc, () -> {
                try {
                    if (!block.getType().isAir()) {
                        block.setType(Material.AIR, false);
                    }
                } catch (Exception ignored) {
                }
            });
        }
    }

    public boolean isManagedHost(Block block) {
        if (block == null) {
            return false;
        }
        try {
            String key = VerticalSlabCell.keyOf(block.getLocation());
            if (hostKeys.contains(key) || plugin.getSlabStorage().get(key) != null) {
                return true;
            }
            return headToCell.containsKey(key);
        } catch (Exception e) {
            return false;
        }
    }

    public String cellKeyFromHead(Block block) {
        if (block == null) {
            return null;
        }
        try {
            return headToCell.get(VerticalSlabCell.keyOf(block.getLocation()));
        } catch (Exception e) {
            return null;
        }
    }

    public void clearBarriers() {
        Set<String> cellKeys = new HashSet<>(headBlockKeys.keySet());
        for (String cellKey : cellKeys) {
            clearHeadForCell(cellKey);
        }
        headBlockKeys.clear();
        headToCell.clear();
        hostKeys.clear();
    }
}
