package net.skykings.combat.map.zone;

import net.skykings.combat.map.MapLandmarkService;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Persistente Map-Mastery fuer Kampfzonen und die vier Gameplay-Landmarks.
 * Landmark-Zeit wird bewusst nur im Speicher hochgezaehlt und periodisch vom
 * IslandGameplayService gespeichert, damit nicht jede Spielsekunde Disk-I/O erzeugt.
 */
public final class MapMasteryService {
    private static volatile MapMasteryService liveInstance;

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private boolean dirty;

    public MapMasteryService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "map-mastery.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        liveInstance = this;
    }

    public static MapMasteryService liveInstance() {
        return liveInstance;
    }

    public void addHotZoneKill(UUID uuid) { addAndSave(uuid, "hot-zone-kills", 1L); }
    public void addKingCapture(UUID uuid) { addAndSave(uuid, "king-captures", 1L); }
    public void addEndKill(UUID uuid) { addAndSave(uuid, "end-kills", 1L); }
    public void addSecret(UUID uuid) { addAndSave(uuid, "secrets", 1L); }

    public int getHotZoneKills(UUID uuid) { return getInt(uuid, "hot-zone-kills"); }
    public int getKingCaptures(UUID uuid) { return getInt(uuid, "king-captures"); }
    public int getEndKills(UUID uuid) { return getInt(uuid, "end-kills"); }
    public int getSecrets(UUID uuid) { return getInt(uuid, "secrets"); }

    public void addLandmarkSecond(UUID uuid, MapLandmarkService.Type type) {
        addBuffered(uuid, landmarkKey(type, "seconds"), 1L);
    }

    public void addLandmarkVisit(UUID uuid, MapLandmarkService.Type type) {
        addAndSave(uuid, landmarkKey(type, "visits"), 1L);
    }

    public void addLandmarkActivity(UUID uuid, MapLandmarkService.Type type) {
        addAndSave(uuid, landmarkKey(type, "activities"), 1L);
    }

    public long getLandmarkSeconds(UUID uuid, MapLandmarkService.Type type) {
        return data.getLong(path(uuid, landmarkKey(type, "seconds")), 0L);
    }

    public int getLandmarkVisits(UUID uuid, MapLandmarkService.Type type) {
        return getInt(uuid, landmarkKey(type, "visits"));
    }

    public int getLandmarkActivities(UUID uuid, MapLandmarkService.Type type) {
        return getInt(uuid, landmarkKey(type, "activities"));
    }

    public long getTotalLandmarkSeconds(UUID uuid) {
        long total = 0L;
        for (MapLandmarkService.Type type : MapLandmarkService.Type.values()) total += getLandmarkSeconds(uuid, type);
        return total;
    }

    public int getTotalLandmarkVisits(UUID uuid) {
        int total = 0;
        for (MapLandmarkService.Type type : MapLandmarkService.Type.values()) total += getLandmarkVisits(uuid, type);
        return total;
    }

    public int getTotalLandmarkActivities(UUID uuid) {
        int total = 0;
        for (MapLandmarkService.Type type : MapLandmarkService.Type.values()) total += getLandmarkActivities(uuid, type);
        return total;
    }

    public String getTitle(UUID uuid) {
        int king = getKingCaptures(uuid);
        int hot = getHotZoneKills(uuid);
        int end = getEndKills(uuid);
        int secrets = getSecrets(uuid);
        long landmarkMinutes = getTotalLandmarkSeconds(uuid) / 60L;
        int activities = getTotalLandmarkActivities(uuid);

        if (king >= 25) return "King Slayer";
        if (end >= 75) return "End Hunter";
        if (activities >= 50 && landmarkMinutes >= 300) return "Island Legend";
        if (king >= 10) return "Altar Conqueror";
        if (hot >= 100) return "Hot Zone Reaper";
        if (landmarkMinutes >= 180) return "Island Veteran";
        if (end >= 25) return "End Raider";
        if (hot >= 50) return "Hot Zone Hunter";
        if (activities >= 15) return "Island Regular";
        if (secrets >= 10) return "Secret Seeker";
        if (hot >= 15) return "Zone Fighter";
        if (landmarkMinutes >= 30) return "Explorer";
        return "Rookie";
    }

    private int getInt(UUID uuid, String key) {
        return data.getInt(path(uuid, key), 0);
    }

    private void addAndSave(UUID uuid, String key, long amount) {
        String path = path(uuid, key);
        long previous = data.getLong(path, 0L);
        addBuffered(uuid, key, amount);
        if (!save()) {
            data.set(path, previous);
            dirty = true;
            plugin.getLogger().severe("Map-Mastery-Fortschritt wurde wegen eines Speicherfehlers verworfen: " + uuid + " / " + key);
        }
    }

    private void addBuffered(UUID uuid, String key, long amount) {
        String path = path(uuid, key);
        long current = data.getLong(path, 0L);
        if (amount > 0L && current > Long.MAX_VALUE - amount) {
            plugin.getLogger().severe("Map-Mastery-Fortschritt wuerde ueberlaufen: " + uuid + " / " + key);
            return;
        }
        data.set(path, current + amount);
        dirty = true;
    }

    private String landmarkKey(MapLandmarkService.Type type, String metric) {
        return "landmarks." + type.name().toLowerCase(Locale.ROOT) + "." + metric;
    }

    private String path(UUID uuid, String key) { return "players." + uuid + "." + key; }

    public boolean save() {
        if (!dirty && file.exists()) return true;
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs() && !plugin.getDataFolder().exists()) {
                plugin.getLogger().severe("Map-Mastery-Datenordner konnte nicht erstellt werden.");
                return false;
            }

            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
                plugin.getLogger().severe("Map-Mastery-Elternordner konnte nicht erstellt werden.");
                return false;
            }

            File temp = new File(parent, file.getName() + ".tmp");
            if (temp.exists() && !temp.delete()) {
                plugin.getLogger().severe("Alte temporaere Map-Mastery-Datei konnte nicht entfernt werden.");
                return false;
            }

            data.save(temp);
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
            return true;
        } catch (IOException ex) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "map-mastery.yml konnte nicht atomar gespeichert werden.", ex);
            return false;
        }
    }
}
