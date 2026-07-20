package org.little100.better_slabs.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.little100.better_slabs.BetterSlabs;
import org.little100.better_slabs.model.VerticalHalf;
import org.little100.better_slabs.model.VerticalSlabCell;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SlabInteractListener implements Listener {

    private static final long DIG_MS = 2000L;
    private static final ThreadLocal<Boolean> INTERNAL_BREAK = ThreadLocal.withInitial(() -> false);

    private final BetterSlabs plugin;
    private final Map<UUID, DigSession> digSessions = new ConcurrentHashMap<>();

    public SlabInteractListener(BetterSlabs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        EquipmentSlot hand = event.getHand();
        if (hand != null && hand != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Action action = event.getAction();

        // 检测空气+潜行右键在切换
        if (player.isSneaking() && action == Action.RIGHT_CLICK_AIR) {
            if (tryToggle(player, item, EquipmentSlot.HAND)) {
                event.setCancelled(true);
                return;
            }
        }

        // 左键挖掘
        if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) {
            Block target = findTargetCell(player, event.getClickedBlock());
            if (target != null) {
                event.setCancelled(true);
                if (player.getGameMode() == GameMode.CREATIVE) {
                    clearTitle(player);
                    doBreak(player, target);
                } else {
                    advanceDig(player, target);
                }
            }
            return;
        }

        // 右键放置
        if (action == Action.RIGHT_CLICK_BLOCK) {
            if (item == null || item.getType().isAir()) {
                return;
            }
            Block clicked = event.getClickedBlock();
            if (clicked == null) {
                return;
            }

            Block resolved = resolveManagedBlock(clicked);
            if (resolved != null) {
                clicked = resolved;
            }

            boolean isVertical = plugin.getSlabRegistry().isVerticalSlabItem(item);
            boolean isNormalSlab = plugin.getSlabRegistry().isSupported(item.getType());

            if (isVertical) {
                boolean handled = plugin.getSlabManager().tryPlace(
                        player, item, clicked, event.getBlockFace(), EquipmentSlot.HAND);
                event.setCancelled(true);
                if (!handled) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.5f);
                }
                return;
            }

            if (isNormalSlab) {
                boolean handled = plugin.getSlabManager().tryPlace(
                        player, item, clicked, event.getBlockFace(), EquipmentSlot.HAND);
                if (handled) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        Block target = findTargetCell(player, null);
        if (target != null) {
            advanceDig(player, target);
        } else {
            DigSession s = digSessions.get(player.getUniqueId());
            if (s != null && System.currentTimeMillis() - s.lastMs > 400L) {
                digSessions.remove(player.getUniqueId());
                clearTitle(player);
            }
        }
    }

    private Block findTargetCell(Player player, Block clicked) {
        double reach = player.getGameMode() == GameMode.CREATIVE ? 5.0 : 4.5;
        Vector direction = player.getEyeLocation().getDirection().normalize();
        var eye = player.getEyeLocation();

        for (double d = 0.1; d <= reach; d += 0.025) {
            var point = eye.clone().add(direction.clone().multiply(d));
            Block at = player.getWorld().getBlockAt(
                    point.getBlockX(), point.getBlockY(), point.getBlockZ());
            Block resolved = resolveManagedBlock(at);

            if (resolved != null) {
                String key = VerticalSlabCell.keyOf(resolved.getLocation());
                VerticalSlabCell cell = plugin.getSlabStorage().get(key);
                if (cell != null && !cell.isEmpty()) {
                    if (cell.isFull()) {
                        return resolved;
                    }
                    for (VerticalHalf half : cell.getHalves()) {
                        BoundingBox box = VerticalSlabCell.halfBox(
                                resolved.getX(), resolved.getY(), resolved.getZ(), half.getFace());
                        if (box.clone().expand(0.03).contains(point.toVector())) {
                            return resolved;
                        }
                    }
                }
            }

            if (!at.getType().isAir() && !at.isPassable()
                    && resolveManagedBlock(at) == null) {
                return null;
            }

            String key = player.getWorld().getUID() + ":" + at.getX() + ":" + at.getY() + ":" + at.getZ();
            VerticalSlabCell cell = plugin.getSlabStorage().get(key);
            if (cell == null || cell.isEmpty()) {
                continue;
            }
            for (VerticalHalf half : cell.getHalves()) {
                BoundingBox box = VerticalSlabCell.halfBox(at.getX(), at.getY(), at.getZ(), half.getFace());
                if (box.clone().expand(0.03).contains(point.toVector())) {
                    return at;
                }
            }
        }

        if (clicked != null
                && player.getEyeLocation().distance(clicked.getLocation().add(0.5, 0.5, 0.5)) <= reach) {
            return resolveManagedBlock(clicked);
        }
        return null;
    }

    private Block resolveManagedBlock(Block block) {
        if (block == null) {
            return null;
        }
        String key = VerticalSlabCell.keyOf(block.getLocation());
        VerticalSlabCell cell = plugin.getSlabStorage().get(key);
        if (cell != null && !cell.isEmpty()) {
            return block;
        }
        String cellKey = plugin.getCollisionManager().cellKeyFromHead(block);
        if (cellKey != null) {
            VerticalSlabCell c = plugin.getSlabStorage().get(cellKey);
            if (c != null && !c.isEmpty()) {
                Block b = c.toBlock();
                return b != null ? b : block;
            }
        }
        if (plugin.getCollisionManager().isManagedHost(block)) {
            return block;
        }
        return null;
    }

    private void advanceDig(Player player, Block block) {
        UUID id = player.getUniqueId();
        String key = VerticalSlabCell.keyOf(block.getLocation());
        long now = System.currentTimeMillis();
        DigSession session = digSessions.get(id);

        if (session == null || !session.key.equals(key)) {
            digSessions.put(id, new DigSession(key, now));
            showProgress(player, 0f);
            player.playSound(block.getLocation(), Sound.BLOCK_STONE_HIT, 0.5f, 1.0f);
            return;
        }

        if (now - session.lastMs > 600L) {
            session.startMs = now;
            session.lastMs = now;
            showProgress(player, 0f);
            player.playSound(block.getLocation(), Sound.BLOCK_STONE_HIT, 0.5f, 1.0f);
            return;
        }

        session.lastMs = now;
        float progress = Math.min(1f, (now - session.startMs) / (float) DIG_MS);
        showProgress(player, progress);

        long elapsed = now - session.startMs;
        if (elapsed / 400L != session.lastBeat) {
            session.lastBeat = elapsed / 400L;
            player.playSound(block.getLocation(), Sound.BLOCK_STONE_HIT, 0.35f, 0.85f);
        }

        if (elapsed >= DIG_MS) {
            digSessions.remove(id);
            clearTitle(player);
            doBreak(player, block);
        }
    }

    private void showProgress(Player player, float progress) {
        int pct = Math.round(progress * 100);
        int bars = 10;
        int filled = Math.round(progress * bars);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            bar.append(i < filled ? "■" : "□");
        }
        String sec = String.format("%.1f", progress * (DIG_MS / 1000.0));
        String total = String.format("%.1f", DIG_MS / 1000.0);

        String subtitle = plugin.getLang("dig.subtitle",
                "{bar}", bar.toString(),
                "{current}", sec,
                "{total}", total,
                "{percent}", String.valueOf(pct));

        try {
            Title title = Title.title(
                    Component.text(plugin.getLang("dig.title")),
                    Component.text(subtitle),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(400), Duration.ofMillis(100)));
            player.showTitle(title);
        } catch (Throwable t) {
            try {
                player.sendTitle(plugin.getLang("dig.title"), subtitle, 0, 8, 2);
            } catch (Throwable ignored) {
                player.sendActionBar(Component.text(plugin.getLang("dig.actionbar", "{percent}", String.valueOf(pct))));
            }
        }
    }

    private void clearTitle(Player player) {
        try {
            player.clearTitle();
        } catch (Throwable t) {
            try {
                player.resetTitle();
            } catch (Throwable ignored) {
            }
        }
    }

    private void doBreak(Player player, Block block) {
        Location loc = block.getLocation();
        plugin.getScheduler().runAt(loc, () -> {
            String key = VerticalSlabCell.keyOf(loc);
            VerticalSlabCell cell = plugin.getSlabStorage().get(key);
            if (cell == null || cell.isEmpty()) {
                return;
            }
            INTERNAL_BREAK.set(true);
            try {
                BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
                plugin.getServer().getPluginManager().callEvent(breakEvent);
                if (breakEvent.isCancelled()) {
                    return;
                }
            } finally {
                INTERNAL_BREAK.set(false);
            }
            plugin.getSlabManager().tryBreak(player, block);
        });
    }

    private boolean tryToggle(Player player, ItemStack item, EquipmentSlot hand) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        boolean isVertical = plugin.getSlabRegistry().isVerticalSlabItem(item);
        boolean isNormalSlab = plugin.getSlabRegistry().isSupported(item.getType());

        if (isVertical) {
            ItemStack normal = plugin.getSlabRegistry().convertToNormalSlab(item);
            if (normal != null) {
                player.getInventory().setItem(hand, normal);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                return true;
            }
        } else if (isNormalSlab) {
            ItemStack vertical = plugin.getSlabRegistry().createVerticalSlabItem(item.getType(), item.getAmount());
            if (vertical != null) {
                player.getInventory().setItem(hand, vertical);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (INTERNAL_BREAK.get()) {
            return;
        }
        Block block = event.getBlock();
        Player player = event.getPlayer();
        String key = VerticalSlabCell.keyOf(block.getLocation());
        VerticalSlabCell cell = plugin.getSlabStorage().get(key);
        boolean managed = (cell != null && !cell.isEmpty())
                || plugin.getCollisionManager().isManagedHost(block);
        if (!managed) {
            return;
        }
        event.setCancelled(true);
        event.setDropItems(false);
        event.setExpToDrop(0);
        if (player.getGameMode() == GameMode.CREATIVE) {
            clearTitle(player);
            doBreak(player, block);
        } else {
            advanceDig(player, block);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        digSessions.remove(event.getPlayer().getUniqueId());
        clearTitle(event.getPlayer());
    }

    private static final class DigSession {
        final String key;
        long startMs;
        long lastMs;
        long lastBeat;

        DigSession(String key, long now) {
            this.key = key;
            this.startMs = now;
            this.lastMs = now;
            this.lastBeat = 0;
        }
    }
}
