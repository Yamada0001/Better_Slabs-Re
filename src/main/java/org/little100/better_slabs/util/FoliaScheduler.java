package org.little100.better_slabs.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class FoliaScheduler {

    private final Plugin plugin;
    private final boolean folia;

    public FoliaScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.folia = detectFolia();
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isFolia() {
        return folia;
    }

    public void runGlobal(Runnable task) {
        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class).invoke(scheduler, plugin, task);
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runGlobalLater(Runnable task, long delayTicks) {
        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class)
                    .invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), Math.max(1L, delayTicks));
            } catch (Exception e) {
                Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public void runAt(Location location, Runnable task) {
        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
                scheduler.getClass().getMethod("execute", Plugin.class, Location.class, Runnable.class)
                    .invoke(scheduler, plugin, location, task);
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runAtLater(Location location, Runnable task, long delayTicks) {
        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
                scheduler.getClass().getMethod("runDelayed", Plugin.class, Location.class, Consumer.class, long.class)
                    .invoke(scheduler, plugin, location, (Consumer<Object>) t -> task.run(), Math.max(1L, delayTicks));
            } catch (Exception e) {
                Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public void runAtFixedRate(Location location, Consumer<Object> task, long delayTicks, long periodTicks) {
        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
                scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Location.class, Consumer.class, long.class, long.class)
                    .invoke(scheduler, plugin, location, task, Math.max(1L, delayTicks), Math.max(1L, periodTicks));
            } catch (Exception e) {
                Bukkit.getScheduler().runTaskTimer(plugin, () -> task.accept(null), delayTicks, periodTicks);
            }
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, () -> task.accept(null), delayTicks, periodTicks);
        }
    }

    public void runForEntity(Entity entity, Runnable task) {
        if (folia) {
            try {
                Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
                scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class, Runnable.class, long.class)
                    .invoke(scheduler, plugin, task, null, 1L);
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void runAsync(Runnable task) {
        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                scheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class)
                    .invoke(scheduler, plugin, (Consumer<Object>) t -> task.run());
            } catch (Exception e) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public void runAsyncLater(Runnable task, long delayMs) {
        if (folia) {
            try {
                Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class)
                    .invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), delayMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                long ticks = Math.max(1L, delayMs / 50L);
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, ticks);
            }
        } else {
            long ticks = Math.max(1L, delayMs / 50L);
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, ticks);
        }
    }
}
