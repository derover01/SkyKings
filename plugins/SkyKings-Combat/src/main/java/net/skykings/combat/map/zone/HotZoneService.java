package net.skykings.combat.map.zone;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Persistente PvP-Hot-Zones mit Enter/Leave-Feedback. */
public final class HotZoneService implements Listener {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, MapZone> zones = new LinkedHashMap<String, MapZone>();
    private final Map<UUID, String> current = new ConcurrentHashMap<UUID, String>();

    public HotZoneService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "hot-zones.yml");
        load();
    }

    public Map<String, MapZone> getZones() { return new LinkedHashMap<String, MapZone>(zones); }

    public boolean add(String id, Player player, double radius) {
        if (id == null || id.trim().isEmpty()) return false;
        String key = normalize(id);
        zones.put(key, new MapZone(key, player.getWorld().getName(), player.getLocation().getX(),
                player.getLocation().getY(), player.getLocation().getZ(), radius));
        save();
        return true;
    }

    public boolean remove(String id) {
        if (id == null) return false;
        boolean removed = zones.remove(normalize(id)) != null;
        if (removed) save();
        return removed;
    }

    public String findZone(Player player) {
        for (Map.Entry<String, MapZone> entry : zones.entrySet()) {
            if (entry.getValue().contains(player.getLocation())) return entry.getKey();
        }
        return null;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        String before = current.get(player.getUniqueId());
        String now = findZone(player);
        if (before == null ? now == null : before.equals(now)) return;

        if (now == null) {
            current.remove(player.getUniqueId());
            if (before != null) {
                player.sendMessage(ChatColor.GRAY + "Du hast die Hot Zone " + ChatColor.RED + before + ChatColor.GRAY + " verlassen.");
                player.playSound(player.getLocation(), Sound.CLICK, 0.35F, 0.8F);
            }
            return;
        }

        current.put(player.getUniqueId(), now);
        player.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "HOT ZONE " + ChatColor.YELLOW + now
                + ChatColor.GRAY + " - erhöhtes Risiko, besondere Rewards folgen.");
        player.playSound(player.getLocation(), Sound.NOTE_PLING, 0.7F, 1.35F);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        current.remove(event.getPlayer().getUniqueId());
    }

    private String normalize(String raw) {
        return raw.trim().toLowerCase(java.util.Locale.ROOT).replace(' ', '-');
    }

    private void load() {
        zones.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.getConfigurationSection("zones") == null) return;
        for (String key : yaml.getConfigurationSection("zones").getKeys(false)) {
            String base = "zones." + key;
            String world = yaml.getString(base + ".world");
            if (world == null) continue;
            zones.put(key, new MapZone(key, world, yaml.getDouble(base + ".x"), yaml.getDouble(base + ".y"),
                    yaml.getDouble(base + ".z"), yaml.getDouble(base + ".radius", 8D)));
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, MapZone> entry : zones.entrySet()) {
            MapZone zone = entry.getValue();
            String base = "zones." + entry.getKey();
            yaml.set(base + ".world", zone.getWorld());
            yaml.set(base + ".x", zone.getX());
            yaml.set(base + ".y", zone.getY());
            yaml.set(base + ".z", zone.getZ());
            yaml.set(base + ".radius", zone.getRadius());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("hot-zones.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
