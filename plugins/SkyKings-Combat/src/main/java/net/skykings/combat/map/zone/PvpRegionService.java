package net.skykings.combat.map.zone;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Mehrere persistente Cuboids fuer echte Open-World-PvP-Bereiche. */
public final class PvpRegionService {
    public static final class Region {
        String id;
        String world;
        int minX, minY, minZ, maxX, maxY, maxZ;

        public boolean contains(Location location) {
            if (location == null || location.getWorld() == null || !world.equals(location.getWorld().getName())) return false;
            double x = location.getX(), y = location.getY(), z = location.getZ();
            return x >= minX && x <= maxX + 1 && y >= minY && y <= maxY + 1 && z >= minZ && z <= maxZ + 1;
        }
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Region> regions = new LinkedHashMap<String, Region>();
    private Location pos1;
    private Location pos2;

    public PvpRegionService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pvp-regions.yml");
        load();
    }

    public void setPos1(Location location) { pos1 = location == null ? null : location.clone(); }
    public void setPos2(Location location) { pos2 = location == null ? null : location.clone(); }

    public boolean create(String rawId) {
        if (pos1 == null || pos2 == null || pos1.getWorld() == null || pos2.getWorld() == null
                || !pos1.getWorld().getName().equals(pos2.getWorld().getName())) return false;
        String id = normalize(rawId);
        Region region = new Region();
        region.id = id;
        region.world = pos1.getWorld().getName();
        region.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        region.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        region.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        region.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        region.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        region.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        regions.put(id, region);
        save();
        return true;
    }

    public boolean remove(String rawId) {
        boolean removed = regions.remove(normalize(rawId)) != null;
        if (removed) save();
        return removed;
    }

    public boolean isInPvpArea(Player player) { return player != null && find(player.getLocation()) != null; }
    public boolean isInPvpArea(Location location) { return find(location) != null; }

    public Region find(Location location) {
        for (Region region : regions.values()) if (region.contains(location)) return region;
        return null;
    }

    public Map<String, Region> getRegions() { return new LinkedHashMap<String, Region>(regions); }

    private String normalize(String raw) {
        return raw == null ? "pvp" : raw.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("regions");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            String base = "regions." + id + ".";
            String world = yaml.getString(base + "world");
            if (world == null) continue;
            Region region = new Region();
            region.id = id; region.world = world;
            region.minX = yaml.getInt(base + "min-x"); region.minY = yaml.getInt(base + "min-y"); region.minZ = yaml.getInt(base + "min-z");
            region.maxX = yaml.getInt(base + "max-x"); region.maxY = yaml.getInt(base + "max-y"); region.maxZ = yaml.getInt(base + "max-z");
            regions.put(id, region);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Region region : regions.values()) {
            String base = "regions." + region.id + ".";
            yaml.set(base + "world", region.world);
            yaml.set(base + "min-x", region.minX); yaml.set(base + "min-y", region.minY); yaml.set(base + "min-z", region.minZ);
            yaml.set(base + "max-x", region.maxX); yaml.set(base + "max-y", region.maxY); yaml.set(base + "max-z", region.maxZ);
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("pvp-regions.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
