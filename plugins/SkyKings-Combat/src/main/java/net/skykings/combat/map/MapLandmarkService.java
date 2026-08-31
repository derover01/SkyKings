package net.skykings.combat.map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
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
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Persistente Marker fuer Gold-, Level-, Blacksmith- und Merchant-Island. */
public final class MapLandmarkService implements Listener {
    public enum Type { GOLD, LEVEL, BLACKSMITH, MERCHANT }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<Type, Entry> entries = new LinkedHashMap<Type, Entry>();
    private final Map<UUID, Type> current = new ConcurrentHashMap<UUID, Type>();

    public MapLandmarkService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "map-landmarks.yml");
        load();
    }

    public void set(Type type, Player player, double radius) {
        entries.put(type, new Entry(player.getWorld().getName(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), radius));
        save();
    }

    public boolean remove(Type type) { boolean removed = entries.remove(type) != null; if (removed) save(); return removed; }
    public Map<Type, Entry> list() { return new LinkedHashMap<Type, Entry>(entries); }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        Player player = event.getPlayer();
        Type now = find(player.getLocation());
        Type before = current.get(player.getUniqueId());
        if (before == now) return;
        if (now == null) { current.remove(player.getUniqueId()); return; }
        current.put(player.getUniqueId(), now);
        player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + display(now));
        player.playSound(player.getLocation(), Sound.NOTE_PLING, 0.45F, 1.2F);
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { current.remove(event.getPlayer().getUniqueId()); }

    private Type find(Location location) {
        for (Map.Entry<Type, Entry> e : entries.entrySet()) if (e.getValue().contains(location)) return e.getKey();
        return null;
    }

    public static Type parse(String raw) {
        if (raw == null) return null;
        String n = raw.trim().toLowerCase(Locale.ROOT);
        if ("gold".equals(n)) return Type.GOLD;
        if ("level".equals(n) || "xp".equals(n)) return Type.LEVEL;
        if ("blacksmith".equals(n) || "smith".equals(n)) return Type.BLACKSMITH;
        if ("merchant".equals(n) || "trader".equals(n)) return Type.MERCHANT;
        return null;
    }

    private String display(Type type) {
        if (type == Type.GOLD) return "GOLD ISLAND";
        if (type == Type.LEVEL) return "LEVEL ISLAND";
        if (type == Type.BLACKSMITH) return "BLACKSMITH ISLAND";
        return "MERCHANT ISLAND";
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (Type type : Type.values()) {
            String base = "landmarks." + type.name().toLowerCase(Locale.ROOT);
            String world = yaml.getString(base + ".world");
            if (world == null) continue;
            entries.put(type, new Entry(world, yaml.getDouble(base + ".x"), yaml.getDouble(base + ".y"), yaml.getDouble(base + ".z"), yaml.getDouble(base + ".radius", 8D)));
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<Type, Entry> e : entries.entrySet()) {
            String base = "landmarks." + e.getKey().name().toLowerCase(Locale.ROOT);
            Entry v = e.getValue();
            yaml.set(base + ".world", v.world); yaml.set(base + ".x", v.x); yaml.set(base + ".y", v.y); yaml.set(base + ".z", v.z); yaml.set(base + ".radius", v.radius);
        }
        try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); yaml.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("map-landmarks.yml konnte nicht gespeichert werden: " + ex.getMessage()); }
    }

    public static final class Entry {
        public final String world; public final double x, y, z, radius;
        Entry(String world, double x, double y, double z, double radius) { this.world = world; this.x = x; this.y = y; this.z = z; this.radius = radius; }
        boolean contains(Location l) { if (l == null || l.getWorld() == null || !world.equals(l.getWorld().getName())) return false; double dx=l.getX()-x, dy=l.getY()-y, dz=l.getZ()-z; return dx*dx+dy*dy+dz*dz <= radius*radius; }
    }
}
