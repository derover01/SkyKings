package net.skykings.combat.stats;

import net.skykings.core.pvp.PvpStatsSnapshot;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** Persistente PvP-Stats: Kills, Tode, aktuelle Streak und Beststreak. */
public final class PvpStatsService implements PvpStatsTracker {

    private static final class MutableStats {
        long kills;
        long deaths;
        int currentStreak;
        int bestStreak;
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, MutableStats> stats = new ConcurrentHashMap<UUID, MutableStats>();
    private final ExecutorService writer;

    public PvpStatsService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pvp-stats.yml");
        this.writer = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SkyKings-PvP-Stats");
            t.setDaemon(true);
            return t;
        });
        load();
    }

    @Override
    public PvpStatsSnapshot getStats(UUID uuid) {
        MutableStats value = stats.get(uuid);
        if (value == null) return new PvpStatsSnapshot(0L, 0L, 0, 0);
        synchronized (value) {
            return new PvpStatsSnapshot(value.kills, value.deaths, value.currentStreak, value.bestStreak);
        }
    }

    public Map<UUID, PvpStatsSnapshot> getAllStats() {
        Map<UUID, PvpStatsSnapshot> snapshot = new HashMap<UUID, PvpStatsSnapshot>();
        for (UUID uuid : stats.keySet()) snapshot.put(uuid, getStats(uuid));
        return Collections.unmodifiableMap(snapshot);
    }

    @Override
    public void recordDeath(UUID victimUuid) {
        MutableStats value = stats.computeIfAbsent(victimUuid, ignored -> new MutableStats());
        synchronized (value) {
            value.deaths++;
            value.currentStreak = 0;
        }
        saveAsync();
    }

    @Override
    public void recordKill(UUID killerUuid, int newStreak) {
        MutableStats value = stats.computeIfAbsent(killerUuid, ignored -> new MutableStats());
        synchronized (value) {
            value.kills++;
            value.currentStreak = Math.max(0, newStreak);
            if (value.currentStreak > value.bestStreak) value.bestStreak = value.currentStreak;
        }
        saveAsync();
    }

    public void shutdown() {
        final Map<UUID, PvpStatsSnapshot> snapshot = new HashMap<UUID, PvpStatsSnapshot>();
        for (UUID uuid : stats.keySet()) snapshot.put(uuid, getStats(uuid));
        writer.submit(() -> save(snapshot));
        writer.shutdown();
        try {
            if (!writer.awaitTermination(3, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("PvP-Stats-Writer wurde nicht innerhalb von 3 Sekunden beendet.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("players");
        if (root == null) return;
        for (String rawUuid : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(rawUuid);
                ConfigurationSection section = root.getConfigurationSection(rawUuid);
                if (section == null) continue;
                MutableStats value = new MutableStats();
                value.kills = Math.max(0L, section.getLong("kills", 0L));
                value.deaths = Math.max(0L, section.getLong("deaths", 0L));
                value.currentStreak = Math.max(0, section.getInt("current-streak", 0));
                value.bestStreak = Math.max(value.currentStreak, section.getInt("best-streak", 0));
                stats.put(uuid, value);
            } catch (IllegalArgumentException ignored) { }
        }
        plugin.getLogger().info("PvP-Stats geladen: " + stats.size());
    }

    private void saveAsync() {
        final Map<UUID, PvpStatsSnapshot> snapshot = new HashMap<UUID, PvpStatsSnapshot>();
        for (UUID uuid : stats.keySet()) snapshot.put(uuid, getStats(uuid));
        writer.submit(() -> save(snapshot));
    }

    private void save(Map<UUID, PvpStatsSnapshot> snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PvpStatsSnapshot> entry : snapshot.entrySet()) {
            String path = "players." + entry.getKey().toString();
            PvpStatsSnapshot value = entry.getValue();
            yaml.set(path + ".kills", value.getKills());
            yaml.set(path + ".deaths", value.getDeaths());
            yaml.set(path + ".current-streak", value.getCurrentStreak());
            yaml.set(path + ".best-streak", value.getBestStreak());
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "PvP-Stats konnten nicht gespeichert werden.", ex);
        }
    }
}
