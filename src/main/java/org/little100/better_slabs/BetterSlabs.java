package org.little100.better_slabs;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.little100.better_slabs.command.BetterSlabsCommand;
import org.little100.better_slabs.listener.ChunkListener;
import org.little100.better_slabs.listener.EntityProtectListener;
import org.little100.better_slabs.listener.NeighborUpdateListener;
import org.little100.better_slabs.listener.SlabInteractListener;
import org.little100.better_slabs.listener.WorldProtectListener;
import org.little100.better_slabs.manager.CollisionManager;
import org.little100.better_slabs.manager.DisplayManager;
import org.little100.better_slabs.manager.SlabManager;
import org.little100.better_slabs.registry.SlabRegistry;
import org.little100.better_slabs.storage.SlabStorage;
import org.little100.better_slabs.util.FoliaScheduler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class BetterSlabs extends JavaPlugin {

    private final String version = getClass().getPackage().getImplementationVersion();
    private SlabRegistry slabRegistry;
    private SlabStorage slabStorage;
    private DisplayManager displayManager;
    private CollisionManager collisionManager;
    private SlabManager slabManager;
    private FoliaScheduler scheduler;
    private FileConfiguration langConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLangConfig();

        this.scheduler = new FoliaScheduler(this);
        this.slabRegistry = new SlabRegistry(this);
        this.slabRegistry.scan();

        this.slabStorage = new SlabStorage(this);
        this.slabStorage.load();

        this.displayManager = new DisplayManager(this);
        this.collisionManager = new CollisionManager(this);
        this.slabManager = new SlabManager(this);

        Bukkit.getPluginManager().registerEvents(new SlabInteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChunkListener(this), this);
        Bukkit.getPluginManager().registerEvents(new WorldProtectListener(this), this);
        Bukkit.getPluginManager().registerEvents(new NeighborUpdateListener(this), this);
        Bukkit.getPluginManager().registerEvents(new EntityProtectListener(), this);

        var command = getCommand("betterslabs");
        if (command != null) {
            BetterSlabsCommand executor = new BetterSlabsCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        scheduler.runGlobalLater(() -> {
            try {
                displayManager.respawnAllLoaded();
            } catch (Throwable t) {
                getLogger().warning("respawn on enable: " + t.getMessage());
            }
            getLogger().info("BetterSlab enabled — " + slabRegistry.getSupportedCount()
                    + " slabs, Folia=" + scheduler.isFolia());
        }, 40L);
    }

    private void loadLangConfig() {
        File langFile = new File(getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            saveResource("lang.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);

        try (InputStream defLangStream = getResource("lang.yml")) {
            if (defLangStream != null) {
                YamlConfiguration defLang = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defLangStream, StandardCharsets.UTF_8));
                langConfig.setDefaults(defLang);
            }
        } catch (IOException e) {
            getLogger().warning("Failed to load default lang.yml: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (displayManager != null) {
            displayManager.despawnAll();
        }
        if (slabStorage != null) {
            slabStorage.close();
        }
        if (collisionManager != null) {
            collisionManager.clearBarriers();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        loadLangConfig();
        slabRegistry.scan();
        displayManager.despawnAll();
        displayManager.respawnAllLoaded();
    }

    public SlabRegistry getSlabRegistry() {
        return slabRegistry;
    }

    public SlabStorage getSlabStorage() {
        return slabStorage;
    }

    public DisplayManager getDisplayManager() {
        return displayManager;
    }

    public CollisionManager getCollisionManager() {
        return collisionManager;
    }

    public SlabManager getSlabManager() {
        return slabManager;
    }

    public String getPluginVersion() {
        return version != null ? version : "unknown";
    }

    public FoliaScheduler getScheduler() {
        return scheduler;
    }

    public FileConfiguration getLangConfig() {
        return langConfig;
    }

    public String getLang(String path) {
        return langConfig.getString(path, path);
    }

    public String getLang(String path, String... replacements) {
        String text = getLang(path);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }
        return text;
    }

    public void debug(String message) {
        if (getConfig().getBoolean("debug")) {
            getLogger().info("[Debug] " + message);
        }
    }
}
