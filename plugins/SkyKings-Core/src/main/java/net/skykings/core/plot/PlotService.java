package net.skykings.core.plot;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.display.ServerListMotdListener;
import net.skykings.core.listener.InventoryDropSyncListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** PlotSquared-inspiriertes Claim-System mit 65x65 Plots, neutralen Strassen und klaren Zellgrenzen. */
public final class PlotService implements PlotAccessService {
    public static final String WORLD_NAME = "SkyPlots";
    public static final int PLOT_SIZE = 65;
    public static final int ROAD_WIDTH = 7;
    public static final int SPACING = PLOT_SIZE + ROAD_WIDTH;
    public static final int RADIUS = 32;
    public static final int Y = 65;

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, PlotData> plots = new HashMap<UUID, PlotData>();
    private final PlotBorderService borderService;
    private int nextIndex;

    public PlotService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "plots.yml");
        ensureWorld();
        load();
        if (plugin instanceof SkyKingsCoreAPI) {
            SkyKingsCoreAPI core = (SkyKingsCoreAPI) plugin;
            this.borderService = new PlotBorderService(plugin, this, core.getEconomyService());
            plugin.getServer().getPluginManager().registerEvents(new InventoryDropSyncListener(plugin), plugin);
            plugin.getServer().getPluginManager().registerEvents(new ServerListMotdListener(), plugin);
        } else {
            this.borderService = null;
        }
        Bukkit.getServicesManager().register(PlotAccessService.class, this, plugin, ServicePriority.Normal);
    }

    public PlotBorderService getBorderService() {
        if (borderService == null) throw new IllegalStateException("PlotBorderService ist nur im laufenden SkyKings-Core verfuegbar.");
        return borderService;
    }

    public World ensureWorld() {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world != null) return world;
        WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.generator(new PlotGridGenerator());
        creator.generateStructures(false);
        world = creator.createWorld();
        if (world != null) {
            world.setSpawnLocation(RADIUS, Y, RADIUS);
            world.setPVP(false);
        }
        return world;
    }

    public synchronized boolean create(Player player) {
        UUID uuid = player.getUniqueId();
        if (plots.containsKey(uuid)) return false;
        World world = ensureWorld();
        if (world == null) return false;
        int index = nextIndex++;
        int gx = index % 100;
        int gz = index / 100;
        int cx = gx * SPACING + RADIUS;
        int cz = gz * SPACING + RADIUS;
        Location home = new Location(world, cx + 0.5D, Y + 0.1D, cz + 0.5D);
        PlotData data = new PlotData(uuid, index, cx, cz, home,
                new HashSet<UUID>(), new HashSet<UUID>(), new HashSet<UUID>(), false, false, false, false);
        plots.put(uuid, data);
        world.getChunkAt(home).load(true);
        applyBorder(data, Material.STEP);
        save();
        player.teleport(home);
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, 1.25F);
        return true;
    }

    public synchronized PlotData get(UUID owner) { return plots.get(owner); }

    /** Generator und Claim-System benutzen exakt dasselbe 72er Raster: 0..64 Plot, 65..71 neutrale Strasse. */
    public synchronized PlotData findAt(Location location) {
        if (!isPlotWorld(location) || isRoad(location)) return null;
        int cellX = Math.floorDiv(location.getBlockX(), SPACING);
        int cellZ = Math.floorDiv(location.getBlockZ(), SPACING);
        int index = cellZ * 100 + cellX;
        for (PlotData plot : plots.values()) if (plot.index == index) return plot;
        return null;
    }

    public boolean isRoad(Location location) {
        if (!isPlotWorld(location)) return false;
        int localX = Math.floorMod(location.getBlockX(), SPACING);
        int localZ = Math.floorMod(location.getBlockZ(), SPACING);
        return localX >= PLOT_SIZE || localZ >= PLOT_SIZE;
    }

    @SuppressWarnings("deprecation")
    public void applyBorder(PlotData plot, Material material) {
        World world = ensureWorld();
        if (world == null || plot == null) return;
        int minX = plot.getMinX();
        int maxX = plot.getMaxX();
        int minZ = plot.getMinZ();
        int maxZ = plot.getMaxZ();
        for (int x = minX; x <= maxX; x++) {
            world.getBlockAt(x, Y - 1, minZ).setType(material, false);
            world.getBlockAt(x, Y - 1, maxZ).setType(material, false);
        }
        for (int z = minZ + 1; z < maxZ; z++) {
            world.getBlockAt(minX, Y - 1, z).setType(material, false);
            world.getBlockAt(maxX, Y - 1, z).setType(material, false);
        }
    }

    public synchronized boolean add(UUID owner, UUID target) {
        PlotData plot = plots.get(owner); if (plot == null || owner.equals(target)) return false;
        plot.denied.remove(target); plot.trusted.remove(target);
        boolean changed = plot.members.add(target); if (changed) save(); return changed;
    }

    public synchronized boolean trust(UUID owner, UUID target) {
        PlotData plot = plots.get(owner); if (plot == null || owner.equals(target)) return false;
        plot.denied.remove(target); plot.members.remove(target);
        boolean changed = plot.trusted.add(target); if (changed) save(); return changed;
    }

    public synchronized boolean remove(UUID owner, UUID target) {
        PlotData plot = plots.get(owner); if (plot == null) return false;
        boolean changed = plot.members.remove(target) | plot.trusted.remove(target);
        if (changed) save(); return changed;
    }

    public synchronized boolean deny(UUID owner, UUID target) {
        PlotData plot = plots.get(owner); if (plot == null || owner.equals(target)) return false;
        plot.members.remove(target); plot.trusted.remove(target);
        boolean changed = plot.denied.add(target); if (changed) save(); return changed;
    }

    public synchronized boolean undeny(UUID owner, UUID target) {
        PlotData plot = plots.get(owner); if (plot == null) return false;
        boolean changed = plot.denied.remove(target); if (changed) save(); return changed;
    }

    public synchronized boolean setHome(UUID owner, Location location) {
        PlotData plot = plots.get(owner); if (plot == null || !plot.contains(location)) return false;
        plot.home = location.clone(); save(); return true;
    }

    public synchronized boolean setFlag(UUID owner, String flag, boolean value) {
        PlotData plot = plots.get(owner); if (plot == null || flag == null) return false;
        String id = flag.toLowerCase(java.util.Locale.ROOT);
        if ("pvp".equals(id)) plot.pvp = value;
        else if ("explosion".equals(id) || "explosions".equals(id)) plot.explosions = value;
        else if ("fire".equals(id) || "feuer".equals(id)) plot.fire = value;
        else if ("mobspawn".equals(id) || "mob-spawn".equals(id) || "mobs".equals(id)) plot.mobSpawning = value;
        else return false;
        save(); return true;
    }

    public boolean canEnter(UUID player, Location location) {
        PlotData plot = findAt(location); return plot == null || !plot.denied.contains(player);
    }
    public boolean isPvpAllowed(Location location) { PlotData plot = findAt(location); return plot != null && plot.pvp; }
    public boolean areExplosionsAllowed(Location location) { PlotData plot = findAt(location); return plot != null && plot.explosions; }
    public boolean isFireAllowed(Location location) { PlotData plot = findAt(location); return plot != null && plot.fire; }

    public void teleportHome(Player player, UUID owner) {
        PlotData plot = get(owner);
        if (plot == null) { player.sendMessage(ChatColor.RED + "Dieser Plot existiert nicht."); return; }
        if (plot.denied.contains(player.getUniqueId()) && !owner.equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Du bist von diesem Plot ausgeschlossen.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1F); return;
        }
        player.teleport(plot.home.clone());
        player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 0.7F, 1.2F);
    }

    @Override public boolean isPlotWorld(Location location) { return location != null && location.getWorld() != null && WORLD_NAME.equals(location.getWorld().getName()); }
    @Override public synchronized boolean hasPlot(UUID owner) { return plots.containsKey(owner); }
    @Override public synchronized boolean canBuild(UUID player, Location location) {
        PlotData plot = findAt(location);
        if (plot == null || plot.denied.contains(player)) return false;
        if (plot.owner.equals(player) || plot.trusted.contains(player)) return true;
        if (!plot.members.contains(player)) return false;
        Player owner = Bukkit.getPlayer(plot.owner);
        return owner != null && owner.isOnline();
    }
    @Override public synchronized boolean ownsLocation(UUID player, Location location) {
        PlotData plot = findAt(location); return plot != null && plot.owner.equals(player);
    }

    private void load() {
        plots.clear(); if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        nextIndex = Math.max(0, yaml.getInt("next-index", 0));
        ConfigurationSection root = yaml.getConfigurationSection("plots"); if (root == null) return;
        World world = ensureWorld();
        for (String raw : root.getKeys(false)) {
            try {
                UUID owner = UUID.fromString(raw); String base = "plots." + raw;
                int index = yaml.getInt(base + ".index");
                int gx = index % 100;
                int gz = index / 100;
                int cx = gx * SPACING + RADIUS;
                int cz = gz * SPACING + RADIUS;
                Location home = new Location(world, yaml.getDouble(base + ".home.x", cx + 0.5D), yaml.getDouble(base + ".home.y", Y + 0.1D),
                        yaml.getDouble(base + ".home.z", cz + 0.5D), (float) yaml.getDouble(base + ".home.yaw", 0D), (float) yaml.getDouble(base + ".home.pitch", 0D));
                Set<UUID> members = readSet(yaml, base + ".members");
                Set<UUID> trusted = readSet(yaml, base + ".trusted");
                Set<UUID> denied = readSet(yaml, base + ".denied");
                plots.put(owner, new PlotData(owner, index, cx, cz, home, members, trusted, denied,
                        yaml.getBoolean(base + ".flags.pvp", false), yaml.getBoolean(base + ".flags.explosions", false),
                        yaml.getBoolean(base + ".flags.fire", false), yaml.getBoolean(base + ".flags.mob-spawn", false)));
                if (index >= nextIndex) nextIndex = index + 1;
            } catch (IllegalArgumentException ignored) { }
        }
    }

    private Set<UUID> readSet(YamlConfiguration yaml, String path) {
        Set<UUID> out = new HashSet<UUID>();
        for (String id : yaml.getStringList(path)) try { out.add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) { }
        return out;
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration(); yaml.set("next-index", nextIndex);
        for (PlotData plot : plots.values()) {
            String base = "plots." + plot.owner;
            yaml.set(base + ".index", plot.index); yaml.set(base + ".center-x", plot.centerX); yaml.set(base + ".center-z", plot.centerZ);
            yaml.set(base + ".home.x", plot.home.getX()); yaml.set(base + ".home.y", plot.home.getY()); yaml.set(base + ".home.z", plot.home.getZ());
            yaml.set(base + ".home.yaw", plot.home.getYaw()); yaml.set(base + ".home.pitch", plot.home.getPitch());
            yaml.set(base + ".members", strings(plot.members)); yaml.set(base + ".trusted", strings(plot.trusted)); yaml.set(base + ".denied", strings(plot.denied));
            yaml.set(base + ".flags.pvp", plot.pvp); yaml.set(base + ".flags.explosions", plot.explosions);
            yaml.set(base + ".flags.fire", plot.fire); yaml.set(base + ".flags.mob-spawn", plot.mobSpawning);
        }
        try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); yaml.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("plots.yml konnte nicht gespeichert werden: " + ex.getMessage()); }
    }

    private List<String> strings(Set<UUID> values) {
        List<String> out = new ArrayList<String>(); for (UUID uuid : values) out.add(uuid.toString()); return out;
    }

    public static final class PlotData {
        public final UUID owner; public final int index; public final int centerX; public final int centerZ;
        private Location home; private final Set<UUID> members; private final Set<UUID> trusted; private final Set<UUID> denied;
        private boolean pvp; private boolean explosions; private boolean fire; private boolean mobSpawning;

        PlotData(UUID owner, int index, int centerX, int centerZ, Location home, Set<UUID> members,
                 Set<UUID> trusted, Set<UUID> denied, boolean pvp, boolean explosions, boolean fire, boolean mobSpawning) {
            this.owner = owner; this.index = index; this.centerX = centerX; this.centerZ = centerZ; this.home = home;
            this.members = members; this.trusted = trusted; this.denied = denied;
            this.pvp = pvp; this.explosions = explosions; this.fire = fire; this.mobSpawning = mobSpawning;
        }
        public Location getHome() { return home.clone(); }
        public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }
        public Set<UUID> getTrusted() { return Collections.unmodifiableSet(trusted); }
        public Set<UUID> getDenied() { return Collections.unmodifiableSet(denied); }
        public boolean isPvp() { return pvp; }
        public boolean isExplosions() { return explosions; }
        public boolean isFire() { return fire; }
        public boolean isMobSpawning() { return mobSpawning; }
        public int getMinX() { return (index % 100) * SPACING; }
        public int getMaxX() { return getMinX() + PLOT_SIZE - 1; }
        public int getMinZ() { return (index / 100) * SPACING; }
        public int getMaxZ() { return getMinZ() + PLOT_SIZE - 1; }
        public boolean contains(Location l) {
            if (l == null || l.getWorld() == null || !WORLD_NAME.equals(l.getWorld().getName())) return false;
            int x = l.getBlockX(); int z = l.getBlockZ();
            return x >= getMinX() && x <= getMaxX() && z >= getMinZ() && z <= getMaxZ();
        }
    }
}
