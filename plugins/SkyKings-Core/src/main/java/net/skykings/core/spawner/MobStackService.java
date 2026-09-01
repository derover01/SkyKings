package net.skykings.core.spawner;

import net.skykings.core.island.IslandAccessService;
import net.skykings.core.plot.PlotAccessService;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reduziert Spawner-Farmen auf SkyIslands/SkyPlots durch sichtbare Mob-Stacks.
 * Nur SPAWNER-Spawns werden zusammengefuehrt; normale Map-Mobs bleiben unangetastet.
 */
public final class MobStackService implements Listener {
    private static final int MAX_STACK = 250;
    private static final double MERGE_RADIUS = 6.0D;
    private static final String MARKER = ChatColor.DARK_GRAY + "[SK]";

    private final JavaPlugin plugin;
    private final IslandAccessService islands;
    private final PlotAccessService plots;

    public MobStackService(JavaPlugin plugin, IslandAccessService islands, PlotAccessService plots) {
        this.plugin = plugin;
        this.islands = islands;
        this.plots = plots;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) return;
        final LivingEntity spawned = event.getEntity();
        if (!inStackWorld(spawned.getLocation()) || excluded(spawned.getType())) return;

        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() {
                if (spawned.isDead() || !spawned.isValid()) return;
                mergeNearby(spawned);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        final LivingEntity dead = event.getEntity();
        if (!inStackWorld(dead.getLocation())) return;
        final int count = readCount(dead);
        if (count <= 1) return;

        final Location location = dead.getLocation().clone();
        final EntityType type = dead.getType();
        final int remaining = count - 1;

        // Der aktuelle Tod behaelt Vanilla-Drops und XP fuer genau einen Mob.
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() {
                if (location.getWorld() == null || excluded(type)) return;
                Entity entity = location.getWorld().spawnEntity(location, type);
                if (!(entity instanceof LivingEntity)) {
                    entity.remove();
                    return;
                }
                applyCount((LivingEntity) entity, remaining);
            }
        });
    }

    private void mergeNearby(LivingEntity target) {
        int total = readCount(target);
        List<LivingEntity> merge = new ArrayList<LivingEntity>();
        for (Entity entity : target.getNearbyEntities(MERGE_RADIUS, MERGE_RADIUS, MERGE_RADIUS)) {
            if (!(entity instanceof LivingEntity) || entity == target || entity.isDead()) continue;
            LivingEntity other = (LivingEntity) entity;
            if (other.getType() != target.getType() || !eligibleName(other)) continue;
            int otherCount = readCount(other);
            if (total + otherCount > MAX_STACK) continue;
            total += otherCount;
            merge.add(other);
        }
        if (merge.isEmpty()) return;
        for (LivingEntity other : merge) other.remove();
        applyCount(target, total);
    }

    private boolean eligibleName(LivingEntity entity) {
        String name = entity.getCustomName();
        return name == null || name.isEmpty() || name.startsWith(MARKER);
    }

    private int readCount(LivingEntity entity) {
        String name = entity.getCustomName();
        if (name == null || !name.startsWith(MARKER)) return 1;
        int marker = name.lastIndexOf(" x");
        if (marker < 0) return 1;
        String raw = ChatColor.stripColor(name.substring(marker + 2));
        try {
            return Math.max(1, Math.min(MAX_STACK, Integer.parseInt(raw)));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private void applyCount(LivingEntity entity, int count) {
        if (count <= 1) {
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
            return;
        }
        entity.setCustomName(MARKER + " " + ChatColor.GOLD + display(entity.getType())
                + ChatColor.GRAY + " x" + ChatColor.WHITE + count);
        entity.setCustomNameVisible(true);
    }

    private String display(EntityType type) {
        String raw = type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder out = new StringBuilder();
        boolean upper = true;
        for (char c : raw.toCharArray()) {
            if (upper && Character.isLetter(c)) {
                out.append(Character.toUpperCase(c));
                upper = false;
            } else {
                out.append(c);
            }
            if (c == ' ') upper = true;
        }
        return out.toString();
    }

    private boolean inStackWorld(Location location) {
        return location != null && (islands.isIslandWorld(location) || plots.isPlotWorld(location));
    }

    private boolean excluded(EntityType type) {
        return type == EntityType.ENDER_DRAGON || type == EntityType.WITHER || type == EntityType.ARMOR_STAND;
    }
}
