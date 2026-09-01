package net.skykings.admin.warp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/** Kleine persistente Warp-Verwaltung ohne externe Abhängigkeit. */
public final class WarpService {
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public WarpService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "warps.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public void set(String name, Location location) {
        String key = normalize(name);
        String path = "warps." + key;
        yaml.set(path + ".display", name);
        yaml.set(path + ".world", location.getWorld().getName());
        yaml.set(path + ".x", location.getX());
        yaml.set(path + ".y", location.getY());
        yaml.set(path + ".z", location.getZ());
        yaml.set(path + ".yaw", location.getYaw());
        yaml.set(path + ".pitch", location.getPitch());
        save();
    }

    public boolean delete(String name) {
        String path = "warps." + normalize(name);
        if (!yaml.contains(path)) return false;
        yaml.set(path, null);
        save();
        return true;
    }

    public Location get(String name) {
        String path = "warps." + normalize(name);
        String worldName = yaml.getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
                yaml.getDouble(path + ".x"), yaml.getDouble(path + ".y"), yaml.getDouble(path + ".z"),
                (float) yaml.getDouble(path + ".yaw"), (float) yaml.getDouble(path + ".pitch"));
    }

    public boolean exists(String name) {
        return yaml.contains("warps." + normalize(name));
    }

    public List<String> names() {
        ConfigurationSection section = yaml.getConfigurationSection("warps");
        if (section == null) return Collections.emptyList();
        List<String> names = new ArrayList<String>();
        for (String key : section.getKeys(false)) {
            names.add(yaml.getString("warps." + key + ".display", key));
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "warps.yml konnte nicht gespeichert werden.", ex);
        }
    }
}
