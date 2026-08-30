package net.skykings.core.perk;

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
import java.util.logging.Level;

/** Persistiert platzierte kostenlose /blöcke-Blöcke, damit die No-Sell-Herkunft erhalten bleibt. */
public final class BuildBlockStore {

    public static final class Entry {
        private final Material material;
        private final short data;

        public Entry(Material material, short data) {
            this.material = material;
            this.data = data;
        }

        public Material getMaterial() { return material; }
        public short getData() { return data; }
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Entry> blocks = new HashMap<String, Entry>();
    private final ExecutorService writer;

    public BuildBlockStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "free-build-blocks.yml");
        this.writer = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SkyKings-BuildBlocks-Store");
            t.setDaemon(true);
            return t;
        });
        load();
    }

    public synchronized Entry get(Location location) { return blocks.get(key(location)); }
    public synchronized boolean contains(Location location) { return blocks.containsKey(key(location)); }

    public void put(Location location, Material material, short data) {
        synchronized (this) { blocks.put(key(location), new Entry(material, data)); }
        saveAsync();
    }

    public void remove(Location location) {
        boolean changed;
        synchronized (this) { changed = blocks.remove(key(location)) != null; }
        if (changed) saveAsync();
    }

    public void shutdown() { writer.shutdown(); }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("blocks");
        if (root == null) return;
        for (String encoded : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(encoded);
            if (section == null) continue;
            Material material = Material.matchMaterial(section.getString("material", ""));
            if (material == null || material == Material.AIR) continue;
            blocks.put(decode(encoded), new Entry(material, (short) section.getInt("data", 0)));
        }
        plugin.getLogger().info("Platzierte Gratis-Baublöcke geladen: " + blocks.size());
    }

    private void saveAsync() {
        final Map<String, Entry> snapshot;
        synchronized (this) { snapshot = new HashMap<String, Entry>(blocks); }
        writer.submit(() -> save(snapshot));
    }

    private void save(Map<String, Entry> snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Entry> entry : snapshot.entrySet()) {
            String path = "blocks." + encode(entry.getKey());
            yaml.set(path + ".material", entry.getValue().getMaterial().name());
            yaml.set(path + ".data", entry.getValue().getData());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Gratis-Baublöcke konnten nicht gespeichert werden.", ex);
        }
    }

    private String key(Location location) {
        if (location == null || location.getWorld() == null) return "";
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private String encode(String raw) { return raw.replace(".", "%2E"); }
    private String decode(String raw) { return raw.replace("%2E", "."); }
}
