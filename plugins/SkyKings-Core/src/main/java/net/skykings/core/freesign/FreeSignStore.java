package net.skykings.core.freesign;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/** Persistente Source of Truth fuer echte SkyKings-Free-Signs. */
public final class FreeSignStore {

    public static final class FreeItem {
        private final Material material;
        private final short data;

        public FreeItem(Material material, short data) {
            this.material = material;
            this.data = data;
        }

        public Material getMaterial() { return material; }
        public short getData() { return data; }
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, FreeItem> signs = new HashMap<String, FreeItem>();

    public FreeSignStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "free-signs.yml");
        load();
    }

    public synchronized FreeItem get(Location location) {
        return signs.get(key(location));
    }

    public synchronized boolean contains(Location location) {
        return signs.containsKey(key(location));
    }

    public void put(Location location, FreeItem item) {
        synchronized (this) {
            signs.put(key(location), item);
        }
        saveAsync();
    }

    public void remove(Location location) {
        boolean changed;
        synchronized (this) {
            changed = signs.remove(key(location)) != null;
        }
        if (changed) saveAsync();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("signs");
        if (root == null) return;
        for (String encodedKey : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(encodedKey);
            if (section == null) continue;
            Material material = Material.matchMaterial(section.getString("material", ""));
            if (material == null || material == Material.AIR) continue;
            signs.put(decodeKey(encodedKey), new FreeItem(material, (short) section.getInt("data", 0)));
        }
        plugin.getLogger().info("Free Signs geladen: " + signs.size());
    }

    private void saveAsync() {
        final Map<String, FreeItem> snapshot;
        synchronized (this) {
            snapshot = new HashMap<String, FreeItem>(signs);
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> save(snapshot));
    }

    private void save(Map<String, FreeItem> snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, FreeItem> entry : snapshot.entrySet()) {
            String path = "signs." + encodeKey(entry.getKey());
            yaml.set(path + ".material", entry.getValue().getMaterial().name());
            yaml.set(path + ".data", entry.getValue().getData());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Free Signs konnten nicht gespeichert werden.", ex);
        }
    }

    private String key(Location location) {
        if (location == null || location.getWorld() == null) return "";
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    // YAML-Pfade duerfen keine Punkte enthalten; Welt-/Koordinaten-Key daher simpel maskieren.
    private String encodeKey(String key) {
        return key.replace(".", "%2E");
    }

    private String decodeKey(String key) {
        return key.replace("%2E", ".");
    }
}
