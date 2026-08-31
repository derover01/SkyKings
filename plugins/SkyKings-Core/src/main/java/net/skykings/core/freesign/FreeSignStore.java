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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** Persistente Source of Truth fuer echte SkyKings-Free-Signs. */
public final class FreeSignStore {

    public static final class FreeItem {
        private final Material material;
        private final short data;
        private final int amount;

        public FreeItem(Material material, short data, int amount) {
            this.material = material;
            this.data = data;
            this.amount = amount;
        }

        public Material getMaterial() { return material; }
        public short getData() { return data; }
        public int getAmount() { return amount; }
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, FreeItem> signs = new HashMap<String, FreeItem>();
    private final ExecutorService writer;

    public FreeSignStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "free-signs.yml");
        this.writer = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "SkyKings-FreeSigns-Store");
            thread.setDaemon(true);
            return thread;
        });
        load();
    }

    public synchronized FreeItem get(Location location) { return signs.get(key(location)); }
    public synchronized boolean contains(Location location) { return signs.containsKey(key(location)); }

    public void put(Location location, FreeItem item) {
        synchronized (this) { signs.put(key(location), item); }
        saveAsync();
    }

    public void remove(Location location) {
        boolean changed;
        synchronized (this) { changed = signs.remove(key(location)) != null; }
        if (changed) saveAsync();
    }

    public void shutdown() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(5, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("FreeSign-Store hatte beim Shutdown noch ausstehende Writes.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            plugin.getLogger().warning("Warten auf FreeSign-Store wurde unterbrochen.");
        }
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
            int amount = Math.max(1, Math.min(material.getMaxStackSize(), section.getInt("amount", 1)));
            signs.put(decodeKey(encodedKey), new FreeItem(material, (short) section.getInt("data", 0), amount));
        }
        plugin.getLogger().info("Free Signs geladen: " + signs.size());
    }

    private void saveAsync() {
        final Map<String, FreeItem> snapshot;
        synchronized (this) { snapshot = new HashMap<String, FreeItem>(signs); }
        writer.submit(() -> save(snapshot));
    }

    private void save(Map<String, FreeItem> snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, FreeItem> entry : snapshot.entrySet()) {
            String path = "signs." + encodeKey(entry.getKey());
            yaml.set(path + ".material", entry.getValue().getMaterial().name());
            yaml.set(path + ".data", entry.getValue().getData());
            yaml.set(path + ".amount", entry.getValue().getAmount());
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

    private String encodeKey(String key) { return key.replace(".", "%2E"); }
    private String decodeKey(String key) { return key.replace("%2E", "."); }
}
