package net.skykings.combat.map.route;

import net.skykings.core.economy.EconomyService;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Persistente Jump-/Pearl-Routen mit geordneten Checkpoints und Abschlussreward. */
public final class MapRouteService implements Listener {
    private static final double CHECKPOINT_RADIUS_SQUARED = 2.5D * 2.5D;
    private static final long COOLDOWN_MILLIS = 30L * 60L * 1000L;
    private static final long COIN_REWARD = 100000L;
    private static final int STAR_REWARD = 2;

    private final JavaPlugin plugin;
    private final EconomyService economy;
    private final File file;
    private final Map<String, List<Location>> routes = new LinkedHashMap<String, List<Location>>();
    private final Map<UUID, Progress> progress = new ConcurrentHashMap<UUID, Progress>();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<String, Long>();

    public MapRouteService(JavaPlugin plugin, EconomyService economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.file = new File(plugin.getDataFolder(), "map-routes.yml");
        load();
    }

    public boolean create(String raw) {
        String id = normalize(raw);
        if (id.isEmpty() || routes.containsKey(id)) return false;
        routes.put(id, new ArrayList<Location>());
        save();
        return true;
    }

    public boolean remove(String raw) {
        boolean removed = routes.remove(normalize(raw)) != null;
        if (removed) save();
        return removed;
    }

    public boolean addPoint(String raw, Player player) {
        List<Location> points = routes.get(normalize(raw));
        if (points == null) return false;
        points.add(player.getLocation().clone());
        save();
        return true;
    }

    public Map<String, Integer> list() {
        Map<String, Integer> out = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, List<Location>> e : routes.entrySet()) out.put(e.getKey(), e.getValue().size());
        return out;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        Player player = event.getPlayer();
        Progress active = progress.get(player.getUniqueId());
        if (active != null) {
            List<Location> points = routes.get(active.route);
            if (points == null || active.index >= points.size()) { progress.remove(player.getUniqueId()); return; }
            if (near(player.getLocation(), points.get(active.index))) {
                active.index++;
                player.playSound(player.getLocation(), Sound.ORB_PICKUP, 0.55F, 1.45F);
                if (active.index >= points.size()) complete(player, active.route);
                else player.sendMessage(ChatColor.AQUA + "Route " + ChatColor.YELLOW + active.route + ChatColor.GRAY
                        + " • Checkpoint " + active.index + "/" + points.size());
            }
            return;
        }
        for (Map.Entry<String, List<Location>> entry : routes.entrySet()) {
            List<Location> points = entry.getValue();
            if (points.size() < 2) continue;
            if (!near(player.getLocation(), points.get(0))) continue;
            progress.put(player.getUniqueId(), new Progress(entry.getKey(), 1));
            player.sendMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "ROUTE " + ChatColor.YELLOW + entry.getKey()
                    + ChatColor.GRAY + " gestartet • 1/" + points.size());
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 0.65F, 1.25F);
            break;
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { progress.remove(event.getPlayer().getUniqueId()); }

    private void complete(Player player, String route) {
        progress.remove(player.getUniqueId());
        long now = System.currentTimeMillis();
        String key = player.getUniqueId() + ":" + route;
        Long until = cooldowns.get(key);
        if (until != null && until > now) {
            player.sendMessage(ChatColor.GREEN + "Route abgeschlossen! " + ChatColor.GRAY + "Reward ist noch im Cooldown.");
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.45F, 1.3F);
            return;
        }
        cooldowns.put(key, now + COOLDOWN_MILLIS);
        economy.deposit(player.getUniqueId(), COIN_REWARD, "MAP_ROUTE", "Route " + route);
        Map<Integer, ItemStack> left = player.getInventory().addItem(new ItemStack(Material.NETHER_STAR, STAR_REWARD));
        for (ItemStack item : left.values()) player.getWorld().dropItemNaturally(player.getLocation(), item);
        player.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "ROUTE GESCHAFFT! " + ChatColor.YELLOW
                + "+100.000 Coins +2 Nethersterne");
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.75F, 1.55F);
    }

    private boolean near(Location a, Location b) {
        return a != null && b != null && a.getWorld() != null && b.getWorld() != null
                && a.getWorld().getName().equals(b.getWorld().getName()) && a.distanceSquared(b) <= CHECKPOINT_RADIUS_SQUARED;
    }

    private String normalize(String raw) { return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace(' ', '-'); }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.getConfigurationSection("routes") == null) return;
        for (String id : yaml.getConfigurationSection("routes").getKeys(false)) {
            List<Location> points = new ArrayList<Location>();
            int count = yaml.getInt("routes." + id + ".count", 0);
            for (int i = 0; i < count; i++) {
                Object raw = yaml.get("routes." + id + ".points." + i);
                if (raw instanceof Location) points.add((Location) raw);
            }
            routes.put(id, points);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, List<Location>> entry : routes.entrySet()) {
            String base = "routes." + entry.getKey();
            yaml.set(base + ".count", entry.getValue().size());
            for (int i = 0; i < entry.getValue().size(); i++) yaml.set(base + ".points." + i, entry.getValue().get(i));
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("map-routes.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private static final class Progress {
        final String route;
        int index;
        Progress(String route, int index) { this.route = route; this.index = index; }
    }
}
