package net.skykings.combat.map.zone;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/** Persistente Phase-6-Mastery fuer Hot-Zones, King-Altar, End-Zone und Secrets. */
public final class MapMasteryService {
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public MapMasteryService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "map-mastery.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public void addHotZoneKill(UUID uuid) { add(uuid, "hot-zone-kills", 1); }
    public void addKingCapture(UUID uuid) { add(uuid, "king-captures", 1); }
    public void addEndKill(UUID uuid) { add(uuid, "end-kills", 1); }
    public void addSecret(UUID uuid) { add(uuid, "secrets", 1); }

    public int getHotZoneKills(UUID uuid) { return data.getInt(path(uuid, "hot-zone-kills"), 0); }
    public int getKingCaptures(UUID uuid) { return data.getInt(path(uuid, "king-captures"), 0); }
    public int getEndKills(UUID uuid) { return data.getInt(path(uuid, "end-kills"), 0); }
    public int getSecrets(UUID uuid) { return data.getInt(path(uuid, "secrets"), 0); }

    public String getTitle(UUID uuid) {
        int king = getKingCaptures(uuid);
        int hot = getHotZoneKills(uuid);
        int end = getEndKills(uuid);
        int secrets = getSecrets(uuid);
        if (king >= 25) return "King Slayer";
        if (end >= 75) return "End Hunter";
        if (king >= 10) return "Altar Conqueror";
        if (hot >= 100) return "Hot Zone Reaper";
        if (end >= 25) return "End Raider";
        if (hot >= 50) return "Hot Zone Hunter";
        if (secrets >= 10) return "Secret Seeker";
        if (hot >= 15) return "Zone Fighter";
        return "Rookie";
    }

    private void add(UUID uuid, String key, int amount) {
        String path = path(uuid, key);
        data.set(path, data.getInt(path, 0) + amount);
        save();
    }

    private String path(UUID uuid, String key) { return "players." + uuid + "." + key; }

    public void save() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            data.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("map-mastery.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
