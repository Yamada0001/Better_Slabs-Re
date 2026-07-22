package org.little100.better_slabs.listener;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.little100.better_slabs.util.Keys;

public final class EntityProtectListener implements Listener {

    public EntityProtectListener() {
    }

    private boolean isOurs(@NotNull Entity entity) {
        var pdc = entity.getPersistentDataContainer();
        return pdc.has(Keys.DISPLAY_MARKER, PersistentDataType.BYTE)
                || pdc.has(Keys.HEAD_MARKER, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(@NotNull EntityDamageEvent event) {
        if (isOurs(event.getEntity())) {
            event.setCancelled(true);
            event.setDamage(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(@NotNull EntityDeathEvent event) {
        if (!isOurs(event.getEntity())) {
            return;
        }
        event.setCancelled(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
    }
}
