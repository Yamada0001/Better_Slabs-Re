package org.little100.better_slabs.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class FoliaScheduler {

    private final Plugin plugin;
    private final boolean folia;

    // 缓存反射 Method，避免每次调度都做 getMethod 查找
    private final Method getGlobalRegionScheduler;
    private final Method getRegionScheduler;
    private final Method getAsyncScheduler;
    private final Method globalRunDelayed;
    private final Method regionExecute;
    private final Method asyncRunNow;

    public FoliaScheduler(Plugin plugin) {
        this.plugin = plugin;
        boolean detected;
        Method tmpGlobalGet, tmpRegionGet, tmpAsyncGet;
        Method tmpGlobalDelayed;
        Method tmpRegionExec;
        Method tmpAsyncNow;

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            detected = true;
        } catch (ClassNotFoundException e) {
            detected = false;
        }

        if (detected) {
            tmpGlobalGet = findMethod(Bukkit.class, "getGlobalRegionScheduler");
            tmpRegionGet = findMethod(Bukkit.class, "getRegionScheduler");
            tmpAsyncGet = findMethod(Bukkit.class, "getAsyncScheduler");
            Class<?> globalRegionSchedulerClass = tmpGlobalGet != null ? tmpGlobalGet.getReturnType() : null;
            Class<?> regionSchedulerClass = tmpRegionGet != null ? tmpRegionGet.getReturnType() : null;
            Class<?> asyncSchedulerClass = tmpAsyncGet != null ? tmpAsyncGet.getReturnType() : null;
            tmpGlobalDelayed = globalRegionSchedulerClass != null
                    ? findMethod(globalRegionSchedulerClass, "runDelayed", Plugin.class, Consumer.class, long.class) : null;
            tmpRegionExec = regionSchedulerClass != null
                    ? findMethod(regionSchedulerClass, "execute", Plugin.class, Location.class, Runnable.class) : null;
            tmpAsyncNow = asyncSchedulerClass != null
                    ? findMethod(asyncSchedulerClass, "runNow", Plugin.class, Consumer.class) : null;
        } else {
            tmpGlobalGet = tmpRegionGet = tmpAsyncGet = null;
            tmpGlobalDelayed = null;
            tmpRegionExec = null;
            tmpAsyncNow = null;
        }

        this.folia = detected;
        this.getGlobalRegionScheduler = tmpGlobalGet;
        this.getRegionScheduler = tmpRegionGet;
        this.getAsyncScheduler = tmpAsyncGet;
        this.globalRunDelayed = tmpGlobalDelayed;
        this.regionExecute = tmpRegionExec;
        this.asyncRunNow = tmpAsyncNow;
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... params) {
        try {
            return clazz.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public boolean isFolia() {
        return folia;
    }

    public void runGlobalLater(Runnable task, long delayTicks) {
        if (folia && globalRunDelayed != null) {
            try {
                Object scheduler = getGlobalRegionScheduler.invoke(null);
                globalRunDelayed.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), Math.max(1L, delayTicks));
                return;
            } catch (Exception ignored) {
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    public void runAt(Location location, Runnable task) {
        if (folia && regionExecute != null) {
            try {
                Object scheduler = getRegionScheduler.invoke(null);
                regionExecute.invoke(scheduler, plugin, location, task);
                return;
            } catch (Exception ignored) {
            }
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public void runAsync(Runnable task) {
        if (folia && asyncRunNow != null) {
            try {
                Object scheduler = getAsyncScheduler.invoke(null);
                asyncRunNow.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run());
                return;
            } catch (Exception ignored) {
            }
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }
}
