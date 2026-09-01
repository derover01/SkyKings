package net.skykings.combat.retention;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Permanente Top-3-Historie abgeschlossener Seasons mit optionalen Head/Hologramm-Displays. */
public final class LegacyHallService {
    public static final class Entry {
        final int season; final int rank; final UUID uuid; final String name; final int xp;
        Entry(int season, int rank, UUID uuid, String name, int xp) {
            this.season = season; this.rank = rank; this.uuid = uuid; this.name = name; this.xp = xp;
        }
    }

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<String, Entry> entries = new LinkedHashMap<String, Entry>();

    public LegacyHallService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "legacy-hall.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        load();
        Bukkit.getScheduler().runTaskLater(plugin, this::respawnDisplays, 40L);
    }

    public void archive(int season, List<Map.Entry<UUID, Integer>> ranking) {
        for (int i = 0; i < Math.min(3, ranking.size()); i++) {
            Map.Entry<UUID, Integer> value = ranking.get(i);
            String name = Bukkit.getOfflinePlayer(value.getKey()).getName();
            if (name == null) name = value.getKey().toString().substring(0, 8);
            entries.put(key(season, i + 1), new Entry(season, i + 1, value.getKey(), name, value.getValue()));
        }
        save(); respawnDisplays();
    }

    public Entry get(int season, int rank) { return entries.get(key(season, rank)); }

    public List<Entry> all() {
        List<Entry> list = new ArrayList<Entry>(entries.values());
        Collections.sort(list, Comparator.comparingInt((Entry e) -> e.season).reversed().thenComparingInt(e -> e.rank));
        return list;
    }

    public void open(Player player, int page) {
        List<Entry> list = all();
        int pageSize = 45;
        int pages = Math.max(1, (list.size() + pageSize - 1) / pageSize);
        int current = Math.max(1, Math.min(page, pages));
        GuiSession gui = GuiSession.create(player, UiTheme.title("Legacy Hall"), 54);
        int from = (current - 1) * pageSize;
        int to = Math.min(list.size(), from + pageSize);
        for (int i = from; i < to; i++) {
            Entry entry = list.get(i);
            gui.setItem(i - from, UiItems.head(entry.name,
                    medalColor(entry.rank) + "#" + entry.rank + " " + UiTheme.TEXT + entry.name,
                    UiTheme.MUTED + "Season " + UiTheme.TEXT + entry.season,
                    UiTheme.MUTED + "Season XP " + UiTheme.TEXT + UiFormat.number(entry.xp),
                    entry.rank == 1 ? UiTheme.LEGENDARY + "CHAMPION" : UiTheme.DISABLED + "LEGACY"));
        }
        if (list.isEmpty()) gui.setItem(22, UiItems.empty("Noch keine Legacy", "Die erste abgeschlossene Season erscheint hier."));
        if (current > 1) gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> open(p, current - 1));
        else gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> { SoundFeedback.back(p); Bukkit.dispatchCommand(p, "profile"); });
        gui.setItem(UiTheme.NAV_HOME, UiItems.item(Material.BOOK, UiTheme.PRIMARY + "Legacy Hall",
                UiTheme.MUTED + "Permanente Season-Historie", UiTheme.TEXT.toString() + list.size() + UiTheme.MUTED + " Eintraege"));
        if (current < pages) gui.setItem(UiTheme.NAV_NEXT, UiItems.next(), (p,e,s) -> open(p, current + 1));
        GuiManager.active().open(gui); SoundFeedback.menuOpen(player);
    }

    public boolean setDisplay(Player staff, int season, int rank) {
        Entry entry = get(season, rank); if (entry == null) return false;
        Location location = staff.getLocation();
        String path = "displays." + season + "." + rank + ".";
        data.set(path + "world", location.getWorld().getName());
        data.set(path + "x", location.getX()); data.set(path + "y", location.getY()); data.set(path + "z", location.getZ());
        data.set(path + "yaw", location.getYaw()); data.set(path + "pitch", location.getPitch());
        saveFile(); spawnDisplay(entry, location); return true;
    }

    public boolean removeDisplay(int season, int rank) {
        String base = "displays." + season + "." + rank;
        if (!data.contains(base)) return false;
        Location location = displayLocation(season, rank);
        if (location != null) removeExisting(entryName(season, rank), location);
        data.set(base, null); saveFile(); return true;
    }

    private void respawnDisplays() {
        ConfigurationSection seasons = data.getConfigurationSection("displays");
        if (seasons == null) return;
        for (String seasonRaw : seasons.getKeys(false)) {
            ConfigurationSection ranks = seasons.getConfigurationSection(seasonRaw);
            if (ranks == null) continue;
            for (String rankRaw : ranks.getKeys(false)) {
                try {
                    int season = Integer.parseInt(seasonRaw), rank = Integer.parseInt(rankRaw);
                    Entry entry = get(season, rank); Location loc = displayLocation(season, rank);
                    if (entry != null && loc != null) spawnDisplay(entry, loc);
                } catch (NumberFormatException ignored) { }
            }
        }
    }

    private void spawnDisplay(Entry entry, Location location) {
        if (location.getWorld() == null) return;
        location.getChunk().load(); removeExisting(entryName(entry.season, entry.rank), location);
        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class);
        stand.setVisible(false); stand.setGravity(false); stand.setSmall(false);
        stand.setCustomName(entryName(entry.season, entry.rank)); stand.setCustomNameVisible(true);
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) head.getItemMeta(); meta.setOwner(entry.name); head.setItemMeta(meta); stand.setHelmet(head);
    }

    private void removeExisting(String name, Location location) {
        for (Entity entity : location.getChunk().getEntities()) {
            if (!(entity instanceof ArmorStand)) continue;
            ArmorStand stand = (ArmorStand) entity;
            if (name.equals(stand.getCustomName())) stand.remove();
        }
    }

    private String entryName(int season, int rank) {
        Entry entry = get(season, rank);
        if (entry == null) return UiTheme.DISABLED + "Legacy Hall";
        return medalColor(rank) + "#" + rank + " " + UiTheme.TEXT + entry.name + UiTheme.DISABLED + " • Season " + season;
    }

    private Location displayLocation(int season, int rank) {
        String path = "displays." + season + "." + rank + ".";
        String worldName = data.getString(path + "world");
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, data.getDouble(path + "x"), data.getDouble(path + "y"), data.getDouble(path + "z"),
                (float) data.getDouble(path + "yaw"), (float) data.getDouble(path + "pitch"));
    }

    private String medalColor(int rank) { return rank == 1 ? UiTheme.LEGENDARY.toString() : rank == 2 ? UiTheme.PRIMARY.toString() : UiTheme.WARNING.toString(); }
    private String key(int season, int rank) { return season + ":" + rank; }

    private void load() {
        ConfigurationSection seasons = data.getConfigurationSection("seasons");
        if (seasons == null) return;
        for (String seasonRaw : seasons.getKeys(false)) {
            ConfigurationSection ranks = seasons.getConfigurationSection(seasonRaw); if (ranks == null) continue;
            for (String rankRaw : ranks.getKeys(false)) {
                try {
                    int season = Integer.parseInt(seasonRaw), rank = Integer.parseInt(rankRaw);
                    String base = "seasons." + season + "." + rank + ".";
                    UUID uuid = UUID.fromString(data.getString(base + "uuid"));
                    entries.put(key(season, rank), new Entry(season, rank, uuid,
                            data.getString(base + "name", uuid.toString().substring(0, 8)), data.getInt(base + "xp", 0)));
                } catch (RuntimeException ignored) { }
            }
        }
    }

    public void save() {
        data.set("seasons", null);
        for (Entry entry : entries.values()) {
            String base = "seasons." + entry.season + "." + entry.rank + ".";
            data.set(base + "uuid", entry.uuid.toString()); data.set(base + "name", entry.name); data.set(base + "xp", entry.xp);
        }
        saveFile();
    }

    private void saveFile() {
        try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); data.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("legacy-hall.yml konnte nicht gespeichert werden: " + ex.getMessage()); }
    }
}
