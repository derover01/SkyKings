package net.skykings.combat.event;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Persistente Arena-Punkte fuer Duel/LMS/Tournament/Juggernaut ohne feste Map-Koordinaten. */
public final class EventArenaService {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Map<String, StoredLocation>> arenas = new LinkedHashMap<String, Map<String, StoredLocation>>();

    public EventArenaService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "event-arenas.yml");
        load();
    }

    public void set(String arenaRaw, String pointRaw, Location location) {
        if (location == null || location.getWorld() == null) return;
        String arena = normalize(arenaRaw);
        String point = normalize(pointRaw);
        if (arena.isEmpty() || point.isEmpty()) return;
        Map<String, StoredLocation> points = arenas.get(arena);
        if (points == null) {
            points = new LinkedHashMap<String, StoredLocation>();
            arenas.put(arena, points);
        }
        points.put(point, StoredLocation.from(location));
        save();
    }

    public boolean removePoint(String arenaRaw, String pointRaw) {
        String arena = normalize(arenaRaw);
        String point = normalize(pointRaw);
        Map<String, StoredLocation> points = arenas.get(arena);
        if (points == null) return false;
        boolean removed = points.remove(point) != null;
        if (points.isEmpty()) arenas.remove(arena);
        if (removed) save();
        return removed;
    }

    public boolean removeArena(String arenaRaw) {
        boolean removed = arenas.remove(normalize(arenaRaw)) != null;
        if (removed) save();
        return removed;
    }

    public Location get(String arenaRaw, String pointRaw) {
        Map<String, StoredLocation> points = arenas.get(normalize(arenaRaw));
        if (points == null) return null;
        StoredLocation stored = points.get(normalize(pointRaw));
        return stored == null ? null : stored.toLocation();
    }

    public Map<String, String> points(String arenaRaw) {
        Map<String, StoredLocation> points = arenas.get(normalize(arenaRaw));
        if (points == null) return Collections.emptyMap();
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (Map.Entry<String, StoredLocation> entry : points.entrySet()) {
            StoredLocation l = entry.getValue();
            out.put(entry.getKey(), l.world + " @ " + round(l.x) + ", " + round(l.y) + ", " + round(l.z));
        }
        return out;
    }

    public Map<String, Integer> list() {
        Map<String, Integer> out = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Map<String, StoredLocation>> entry : arenas.entrySet()) out.put(entry.getKey(), entry.getValue().size());
        return out;
    }

    public boolean isReadyForDuel(String arena) {
        return get(arena, "a") != null && get(arena, "b") != null;
    }

    public boolean isReadyForLms(String arena) {
        return get(arena, "lobby") != null && countPrefix(arena, "spawn") >= 4;
    }

    public int countPrefix(String arenaRaw, String prefixRaw) {
        Map<String, StoredLocation> points = arenas.get(normalize(arenaRaw));
        if (points == null) return 0;
        String prefix = normalize(prefixRaw);
        int count = 0;
        for (String key : points.keySet()) if (key.startsWith(prefix)) count++;
        return count;
    }

    private void load() {
        arenas.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("arenas");
        if (root == null) return;
        for (String arena : root.getKeys(false)) {
            ConfigurationSection pointsSection = root.getConfigurationSection(arena + ".points");
            if (pointsSection == null) continue;
            Map<String, StoredLocation> points = new LinkedHashMap<String, StoredLocation>();
            for (String point : pointsSection.getKeys(false)) {
                String base = "arenas." + arena + ".points." + point + ".";
                String world = yaml.getString(base + "world");
                if (world == null || world.trim().isEmpty()) continue;
                points.put(normalize(point), new StoredLocation(world, yaml.getDouble(base + "x"), yaml.getDouble(base + "y"),
                        yaml.getDouble(base + "z"), (float) yaml.getDouble(base + "yaw"), (float) yaml.getDouble(base + "pitch")));
            }
            if (!points.isEmpty()) arenas.put(normalize(arena), points);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Map<String, StoredLocation>> arena : arenas.entrySet()) {
            for (Map.Entry<String, StoredLocation> point : arena.getValue().entrySet()) {
                String base = "arenas." + arena.getKey() + ".points." + point.getKey() + ".";
                StoredLocation l = point.getValue();
                yaml.set(base + "world", l.world);
                yaml.set(base + "x", l.x);
                yaml.set(base + "y", l.y);
                yaml.set(base + "z", l.z);
                yaml.set(base + "yaw", l.yaw);
                yaml.set(base + "pitch", l.pitch);
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("event-arenas.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    private String round(double value) { return String.format(Locale.US, "%.1f", value); }

    private static final class StoredLocation {
        final String world; final double x, y, z; final float yaw, pitch;
        StoredLocation(String world, double x, double y, double z, float yaw, float pitch) {
            this.world = world; this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch;
        }
        static StoredLocation from(Location l) {
            return new StoredLocation(l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
        }
        Location toLocation() {
            World worldObject = org.bukkit.Bukkit.getWorld(world);
            return worldObject == null ? null : new Location(worldObject, x, y, z, yaw, pitch);
        }
    }
}
