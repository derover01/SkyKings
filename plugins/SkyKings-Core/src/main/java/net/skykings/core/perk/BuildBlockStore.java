package net.skykings.core.perk;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

    /**
     * Kritischer Platzierungs-Pfad: Der No-Sell-Marker muss durable sein, bevor ein kostenloser
     * Block in der Welt als erfolgreich akzeptiert wird. Sonst koennte ein Crash den Marker
     * verlieren und den Gratisblock in einen normal verkaufbaren Block verwandeln.
     */
    public synchronized boolean putNow(Location location, Material material, short data) {
        String locationKey = key(location);
        Entry previous = blocks.put(locationKey, new Entry(material, data));
        if (saveSnapshot(new HashMap<String, Entry>(blocks))) return true;
        if (previous == null) blocks.remove(locationKey); else blocks.put(locationKey, previous);
        return false;
    }

    public void remove(Location location) {
        boolean changed;
        synchronized (this) { changed = blocks.remove(key(location)) != null; }
        if (changed) saveAsync();
    }

    /** Synchroner Remove fuer Break-Pfade, damit alte Marker nicht nach einem Crash wieder auftauchen. */
    public synchronized boolean removeNow(Location location) {
        String locationKey = key(location);
        Entry previous = blocks.remove(locationKey);
        if (previous == null) return true;
        if (saveSnapshot(new HashMap<String, Entry>(blocks))) return true;
        blocks.put(locationKey, previous);
        return false;
    }

    public void shutdown() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(5, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("BuildBlock-Store hatte beim Shutdown noch ausstehende Writes.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            plugin.getLogger().warning("Warten auf BuildBlock-Store wurde unterbrochen.");
        }
    }

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
        writer.submit(() -> saveSnapshot(snapshot));
    }

    private boolean saveSnapshot(Map<String, Entry> snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Entry> entry : snapshot.entrySet()) {
            String path = "blocks." + encode(entry.getKey());
            yaml.set(path + ".material", entry.getValue().getMaterial().name());
            yaml.set(path + ".data", entry.getValue().getData());
        }
        File parent = file.getParentFile();
        File temp = parent == null ? new File(file.getPath() + ".tmp") : new File(parent, file.getName() + ".tmp");
        try {
            if (parent != null && !parent.exists() && !parent.mkdirs()) return false;
            yaml.save(temp);
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "Gratis-Baublöcke konnten nicht atomar gespeichert werden.", ex);
            if (temp.exists() && !temp.delete()) temp.deleteOnExit();
            return false;
        }
    }

    private String key(Location location) {
        if (location == null || location.getWorld() == null) return "";
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private String encode(String raw) { return raw.replace(".", "%2E"); }
    private String decode(String raw) { return raw.replace("%2E", "."); }
}
