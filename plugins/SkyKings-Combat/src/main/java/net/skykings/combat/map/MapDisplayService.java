package net.skykings.combat.map;

import net.skykings.combat.map.zone.HotZoneService;
import net.skykings.combat.map.zone.KingAltarService;
import net.skykings.combat.stats.PvpStatsService;
import net.skykings.core.pvp.PvpStatsSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Dynamische 1.8-Map-Hologramme fuer Top-Kills, King Altar und Hot-Zones. */
public final class MapDisplayService {
    public enum Type { TOP_KILLS, KING, HOT_ZONES }

    private final JavaPlugin plugin;
    private final PvpStatsService stats;
    private final KingAltarService king;
    private final HotZoneService hotZones;
    private final File file;
    private final Map<Type, StoredLocation> locations = new LinkedHashMap<Type, StoredLocation>();
    private final Map<Type, List<UUID>> spawned = new LinkedHashMap<Type, List<UUID>>();
    private int taskId = -1;

    public MapDisplayService(JavaPlugin plugin, PvpStatsService stats, KingAltarService king, HotZoneService hotZones) {
        this.plugin = plugin;
        this.stats = stats;
        this.king = king;
        this.hotZones = hotZones;
        this.file = new File(plugin.getDataFolder(), "map-displays.yml");
        load();
        this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::refreshAll, 40L, 20L * 10L);
    }

    public void set(Type type, Player player) {
        Location l = player.getLocation();
        locations.put(type, new StoredLocation(l.getWorld().getName(), l.getX(), l.getY(), l.getZ()));
        save();
        refresh(type);
    }

    public boolean remove(Type type) {
        boolean removed = locations.remove(type) != null;
        removeSpawned(type);
        if (removed) save();
        return removed;
    }

    public Map<Type, String> list() {
        Map<Type, String> out = new LinkedHashMap<Type, String>();
        for (Map.Entry<Type, StoredLocation> e : locations.entrySet()) out.put(e.getKey(), e.getValue().world);
        return out;
    }

    public void shutdown() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        for (Type type : Type.values()) removeSpawned(type);
        save();
    }

    private void refreshAll() {
        for (Type type : locations.keySet().toArray(new Type[0])) refresh(type);
    }

    private void refresh(Type type) {
        StoredLocation stored = locations.get(type);
        if (stored == null) return;
        World world = Bukkit.getWorld(stored.world);
        if (world == null) return;
        removeSpawned(type);
        List<String> lines = lines(type);
        List<UUID> ids = new ArrayList<UUID>();
        double y = stored.y + ((lines.size() - 1) * 0.27D);
        for (String line : lines) {
            Location loc = new Location(world, stored.x, y, stored.z);
            ArmorStand stand = world.spawn(loc, ArmorStand.class);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setSmall(true);
            stand.setBasePlate(false);
            stand.setCustomName(line);
            stand.setCustomNameVisible(true);
            ids.add(stand.getUniqueId());
            y -= 0.27D;
        }
        spawned.put(type, ids);
    }

    private List<String> lines(Type type) {
        if (type == Type.TOP_KILLS) return topKillLines();
        if (type == Type.KING) {
            List<String> out = new ArrayList<String>();
            out.add(ChatColor.GOLD.toString() + ChatColor.BOLD + "KING ALTAR");
            if (king.getZone() == null) out.add(ChatColor.GRAY + "Noch nicht eingerichtet");
            else if (king.getCooldown() > 0) out.add(ChatColor.RED + "Cooldown: " + king.getCooldown() + "s");
            else if (king.getCapturing() != null) {
                Player p = Bukkit.getPlayer(king.getCapturing());
                out.add(ChatColor.YELLOW + (p == null ? "Unbekannt" : p.getName()) + ChatColor.GRAY + " captured");
                out.add(ChatColor.WHITE + String.valueOf(king.getProgress()) + "/60s");
            } else out.add(ChatColor.GREEN + "Bereit zur Eroberung");
            return out;
        }
        List<String> out = new ArrayList<String>();
        out.add(ChatColor.RED.toString() + ChatColor.BOLD + "HOT ZONES");
        out.add(ChatColor.YELLOW + String.valueOf(hotZones.getZones().size()) + ChatColor.GRAY + " aktive Zonen");
        out.add(ChatColor.GRAY + "+25.000 Coins +1 Netherstern/Kill");
        return out;
    }

    private List<String> topKillLines() {
        List<Map.Entry<UUID, PvpStatsSnapshot>> entries = new ArrayList<Map.Entry<UUID, PvpStatsSnapshot>>(stats.getAllStats().entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<UUID, PvpStatsSnapshot>>() {
            @Override public int compare(Map.Entry<UUID, PvpStatsSnapshot> a, Map.Entry<UUID, PvpStatsSnapshot> b) {
                return Long.compare(b.getValue().getKills(), a.getValue().getKills());
            }
        });
        List<String> out = new ArrayList<String>();
        out.add(ChatColor.GOLD.toString() + ChatColor.BOLD + "TOP KILLS");
        for (int i = 0; i < Math.min(3, entries.size()); i++) {
            Map.Entry<UUID, PvpStatsSnapshot> entry = entries.get(i);
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = entry.getKey().toString().substring(0, 8);
            out.add(ChatColor.YELLOW + "#" + (i + 1) + " " + ChatColor.WHITE + name + ChatColor.GRAY + " - " + entry.getValue().getKills());
        }
        if (entries.isEmpty()) out.add(ChatColor.GRAY + "Noch keine Kills");
        return out;
    }

    private void removeSpawned(Type type) {
        List<UUID> ids = spawned.remove(type);
        if (ids == null) return;
        for (World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (ids.contains(entity.getUniqueId())) entity.remove();
            }
        }
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (Type type : Type.values()) {
            String base = "displays." + type.name().toLowerCase(Locale.ROOT);
            String world = yaml.getString(base + ".world");
            if (world == null) continue;
            locations.put(type, new StoredLocation(world, yaml.getDouble(base + ".x"), yaml.getDouble(base + ".y"), yaml.getDouble(base + ".z")));
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<Type, StoredLocation> e : locations.entrySet()) {
            String base = "displays." + e.getKey().name().toLowerCase(Locale.ROOT);
            StoredLocation l = e.getValue();
            yaml.set(base + ".world", l.world);
            yaml.set(base + ".x", l.x);
            yaml.set(base + ".y", l.y);
            yaml.set(base + ".z", l.z);
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("map-displays.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    public static Type parse(String raw) {
        if (raw == null) return null;
        String n = raw.trim().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        if ("topkills".equals(n) || "kills".equals(n) || "top".equals(n)) return Type.TOP_KILLS;
        if ("king".equals(n) || "koth".equals(n) || "kingaltar".equals(n)) return Type.KING;
        if ("hotzones".equals(n) || "hotzone".equals(n)) return Type.HOT_ZONES;
        return null;
    }

    private static final class StoredLocation {
        final String world; final double x, y, z;
        StoredLocation(String world, double x, double y, double z) { this.world = world; this.x = x; this.y = y; this.z = z; }
    }
}
