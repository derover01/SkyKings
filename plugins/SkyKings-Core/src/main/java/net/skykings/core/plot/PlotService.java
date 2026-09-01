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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * PlotSquared-inspiriertes Claim-System.
 *
 * <p>Das Welt-Raster ist die einzige Wahrheit: 65x65 Plotflaeche, danach 7 Block neutrale Strasse.
 * Strassen gehoeren keinem Plot und werden erst beim expliziten Merge Teil der zusammengefuehrten Flaeche.</p>
 */
public final class PlotService implements PlotAccessService {
    public static final String WORLD_NAME = "SkyPlots";
    public static final int PLOT_SIZE = 65;
    public static final int ROAD_WIDTH = 7;
    public static final int SPACING = PLOT_SIZE + ROAD_WIDTH;
    public static final int GRID_WIDTH = 100;
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
            refreshLoadedBorders();
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

        while (isCellClaimed(nextIndex)) nextIndex++;
        int index = nextIndex++;
        int gx = gridX(index);
        int gz = gridZ(index);
        int cx = gx * SPACING + RADIUS;
        int cz = gz * SPACING + RADIUS;
        Location home = new Location(world, cx + 0.5D, Y + 0.1D, cz + 0.5D);
        Set<Integer> cells = new LinkedHashSet<Integer>();
        cells.add(index);
        PlotData data = new PlotData(uuid, index, cx, cz, home,
                new HashSet<UUID>(), new HashSet<UUID>(), new HashSet<UUID>(), cells,
                false, false, false, false);
        plots.put(uuid, data);
        world.getChunkAt(home).load(true);
        applyBorder(data, Material.STEP);
        save();
        player.teleport(home);
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, 1.25F);
        return true;
    }

    public synchronized PlotData get(UUID owner) { return plots.get(owner); }

    /**
     * Generator und Claim-System benutzen exakt dasselbe 72er Raster.
     * Normale Strassen liefern null; nur eine physisch entfernte Merge-Strasse gehoert zum Plot.
     */
    public synchronized PlotData findAt(Location location) {
        if (!isPlotWorld(location)) return null;
        int gx = Math.floorDiv(location.getBlockX(), SPACING);
        int gz = Math.floorDiv(location.getBlockZ(), SPACING);
        int localX = Math.floorMod(location.getBlockX(), SPACING);
        int localZ = Math.floorMod(location.getBlockZ(), SPACING);

        if (localX < PLOT_SIZE && localZ < PLOT_SIZE) {
            return findByCell(cellIndex(gx, gz));
        }
        return findMergedRoad(gx, gz, localX, localZ);
    }

    /** Geometrische Road-Zone des Generators, unabhaengig davon ob sie durch Merge entfernt wurde. */
    public boolean isRoad(Location location) {
        if (!isPlotWorld(location)) return false;
        int localX = Math.floorMod(location.getBlockX(), SPACING);
        int localZ = Math.floorMod(location.getBlockZ(), SPACING);
        return localX >= PLOT_SIZE || localZ >= PLOT_SIZE;
    }

    /** Echte, geschuetzte Strasse. Entfernte Merge-Roads zaehlen nicht mehr als neutral. */
    public synchronized boolean isNeutralRoad(Location location) {
        return isRoad(location) && findAt(location) == null;
    }

    /** Noch freie 65x65 Plotflaeche. */
    public synchronized boolean isUnclaimedPlotCell(Location location) {
        return isPlotWorld(location) && !isRoad(location) && findAt(location) == null;
    }

    /** Der sichtbare, vom System verwaltete Aussenrand eines geclaimten/merged Plots. */
    public synchronized boolean isManagedBorder(Location location) {
        if (!isPlotWorld(location) || location.getBlockY() != Y) return false;
        PlotData plot = findAt(location);
        if (plot == null || isRoad(location)) return false;
        int gx = Math.floorDiv(location.getBlockX(), SPACING);
        int gz = Math.floorDiv(location.getBlockZ(), SPACING);
        int localX = Math.floorMod(location.getBlockX(), SPACING);
        int localZ = Math.floorMod(location.getBlockZ(), SPACING);
        if (!plot.cells.contains(cellIndex(gx, gz))) return false;
        return (localX == 0 && !plot.cells.contains(cellIndex(gx - 1, gz)))
                || (localX == PLOT_SIZE - 1 && !plot.cells.contains(cellIndex(gx + 1, gz)))
                || (localZ == 0 && !plot.cells.contains(cellIndex(gx, gz - 1)))
                || (localZ == PLOT_SIZE - 1 && !plot.cells.contains(cellIndex(gx, gz + 1)));
    }

    public synchronized MergeResult merge(UUID owner, Location source, MergeDirection direction) {
        PlotData plot = plots.get(owner);
        if (plot == null) return MergeResult.NO_PLOT;
        if (direction == null) return MergeResult.INVALID_DIRECTION;
        PlotData standing = findAt(source);
        if (standing == null || !standing.owner.equals(owner) || isRoad(source)) return MergeResult.NOT_ON_OWN_PLOT;

        int sourceGx = Math.floorDiv(source.getBlockX(), SPACING);
        int sourceGz = Math.floorDiv(source.getBlockZ(), SPACING);
        int sourceIndex = cellIndex(sourceGx, sourceGz);
        if (!plot.cells.contains(sourceIndex)) return MergeResult.NOT_ON_OWN_PLOT;

        int targetGx = sourceGx + direction.dx;
        int targetGz = sourceGz + direction.dz;
        if (targetGx < 0 || targetGx >= GRID_WIDTH || targetGz < 0) return MergeResult.WORLD_EDGE;
        int targetIndex = cellIndex(targetGx, targetGz);
        if (plot.cells.contains(targetIndex)) return MergeResult.ALREADY_MERGED;
        if (isCellClaimed(targetIndex)) return MergeResult.CLAIMED_BY_OTHER;

        plot.cells.add(targetIndex);
        refreshMergedTerrain(plot);
        Material border = borderService == null ? Material.STEP : borderService.selected(owner).getMaterial();
        applyBorder(plot, border);
        save();
        return MergeResult.SUCCESS;
    }

    /**
     * Entfernt nur die Strassen, die zwischen Zellen desselben Plots liegen. Kreuzungen werden erst
     * entfernt, wenn alle vier angrenzenden Zellen zum selben Merge gehoeren.
     */
    @SuppressWarnings("deprecation")
    private void refreshMergedTerrain(PlotData plot) {
        World world = ensureWorld();
        if (world == null) return;
        for (Integer index : plot.cells) {
            int gx = gridX(index);
            int gz = gridZ(index);
            if (plot.cells.contains(cellIndex(gx + 1, gz))) fillEastRoad(world, gx, gz);
            if (plot.cells.contains(cellIndex(gx, gz + 1))) fillSouthRoad(world, gx, gz);
            if (plot.cells.contains(cellIndex(gx + 1, gz))
                    && plot.cells.contains(cellIndex(gx, gz + 1))
                    && plot.cells.contains(cellIndex(gx + 1, gz + 1))) {
                fillIntersection(world, gx, gz);
            }
        }
    }

    private void fillEastRoad(World world, int gx, int gz) {
        int startX = gx * SPACING + PLOT_SIZE;
        int minZ = gz * SPACING;
        for (int x = startX; x < startX + ROAD_WIDTH; x++) {
            for (int z = minZ; z < minZ + PLOT_SIZE; z++) setPlotGround(world, x, z);
        }
        int leftEdge = gx * SPACING + PLOT_SIZE - 1;
        int rightEdge = (gx + 1) * SPACING;
        for (int z = minZ; z < minZ + PLOT_SIZE; z++) {
            world.getBlockAt(leftEdge, Y - 1, z).setType(Material.GRASS, false);
            world.getBlockAt(rightEdge, Y - 1, z).setType(Material.GRASS, false);
            world.getBlockAt(leftEdge, Y, z).setType(Material.AIR, false);
            world.getBlockAt(rightEdge, Y, z).setType(Material.AIR, false);
        }
    }

    private void fillSouthRoad(World world, int gx, int gz) {
        int startZ = gz * SPACING + PLOT_SIZE;
        int minX = gx * SPACING;
        for (int z = startZ; z < startZ + ROAD_WIDTH; z++) {
            for (int x = minX; x < minX + PLOT_SIZE; x++) setPlotGround(world, x, z);
        }
        int northEdge = gz * SPACING + PLOT_SIZE - 1;
        int southEdge = (gz + 1) * SPACING;
        for (int x = minX; x < minX + PLOT_SIZE; x++) {
            world.getBlockAt(x, Y - 1, northEdge).setType(Material.GRASS, false);
            world.getBlockAt(x, Y - 1, southEdge).setType(Material.GRASS, false);
            world.getBlockAt(x, Y, northEdge).setType(Material.AIR, false);
            world.getBlockAt(x, Y, southEdge).setType(Material.AIR, false);
        }
    }

    private void fillIntersection(World world, int gx, int gz) {
        int startX = gx * SPACING + PLOT_SIZE;
        int startZ = gz * SPACING + PLOT_SIZE;
        for (int x = startX; x < startX + ROAD_WIDTH; x++) {
            for (int z = startZ; z < startZ + ROAD_WIDTH; z++) setPlotGround(world, x, z);
        }
    }

    private void setPlotGround(World world, int x, int z) {
        world.getBlockAt(x, 61, z).setType(Material.DIRT, false);
        world.getBlockAt(x, 62, z).setType(Material.DIRT, false);
        world.getBlockAt(x, 63, z).setType(Material.DIRT, false);
        world.getBlockAt(x, Y - 1, z).setType(Material.GRASS, false);
        world.getBlockAt(x, Y, z).setType(Material.AIR, false);
    }

    /** Wendet einen Cosmetic-Rand nur an der aeusseren Kontur an; interne Merge-Kanten werden Gras. */
    public synchronized void applyBorder(PlotData plot, Material material) {
        World world = ensureWorld();
        if (world == null || plot == null || material == null) return;
        for (Integer index : plot.cells) {
            int gx = gridX(index);
            int gz = gridZ(index);
            int minX = gx * SPACING;
            int minZ = gz * SPACING;
            int maxX = minX + PLOT_SIZE - 1;
            int maxZ = minZ + PLOT_SIZE - 1;
            boolean westOpen = !plot.cells.contains(cellIndex(gx - 1, gz));
            boolean eastOpen = !plot.cells.contains(cellIndex(gx + 1, gz));
            boolean northOpen = !plot.cells.contains(cellIndex(gx, gz - 1));
            boolean southOpen = !plot.cells.contains(cellIndex(gx, gz + 1));

            for (int x = minX; x <= maxX; x++) {
                setBorderBlock(world, x, minZ, material, northOpen || (x == minX && westOpen) || (x == maxX && eastOpen));
                setBorderBlock(world, x, maxZ, material, southOpen || (x == minX && westOpen) || (x == maxX && eastOpen));
            }
            for (int z = minZ + 1; z < maxZ; z++) {
                setBorderBlock(world, minX, z, material, westOpen);
                setBorderBlock(world, maxX, z, material, eastOpen);
            }
        }
    }

    private void setBorderBlock(World world, int x, int z, Material material, boolean exposed) {
        world.getBlockAt(x, Y - 1, z).setType(Material.GRASS, false);
        world.getBlockAt(x, Y, z).setType(exposed ? material : Material.AIR, false);
    }

    /** Migriert bestehende Claims beim Start vom alten bodenbuendigen auf den erhoehten Rand. */
    private synchronized void refreshLoadedBorders() {
        if (borderService == null) return;
        for (PlotData plot : plots.values()) {
            applyBorder(plot, borderService.selected(plot.owner).getMaterial());
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
        PlotData plot = plots.get(owner);
        if (plot == null || findAt(location) != plot) return false;
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

    private PlotData findByCell(int index) {
        for (PlotData plot : plots.values()) if (plot.cells.contains(index)) return plot;
        return null;
    }

    private PlotData findMergedRoad(int gx, int gz, int localX, int localZ) {
        if (localX >= PLOT_SIZE && localZ < PLOT_SIZE) {
            return samePlot(cellIndex(gx, gz), cellIndex(gx + 1, gz));
        }
        if (localZ >= PLOT_SIZE && localX < PLOT_SIZE) {
            return samePlot(cellIndex(gx, gz), cellIndex(gx, gz + 1));
        }
        if (localX >= PLOT_SIZE && localZ >= PLOT_SIZE) {
            PlotData a = findByCell(cellIndex(gx, gz));
            if (a == null) return null;
            return a.cells.contains(cellIndex(gx + 1, gz))
                    && a.cells.contains(cellIndex(gx, gz + 1))
                    && a.cells.contains(cellIndex(gx + 1, gz + 1)) ? a : null;
        }
        return null;
    }

    private PlotData samePlot(int aIndex, int bIndex) {
        PlotData a = findByCell(aIndex);
        return a != null && a.cells.contains(bIndex) ? a : null;
    }

    private boolean isCellClaimed(int index) { return findByCell(index) != null; }
    private int cellIndex(int gx, int gz) { return gz * GRID_WIDTH + gx; }
    private int gridX(int index) { return Math.floorMod(index, GRID_WIDTH); }
    private int gridZ(int index) { return Math.floorDiv(index, GRID_WIDTH); }

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
                int gx = gridX(index);
                int gz = gridZ(index);
                int cx = gx * SPACING + RADIUS;
                int cz = gz * SPACING + RADIUS;
                Location home = new Location(world, yaml.getDouble(base + ".home.x", cx + 0.5D), yaml.getDouble(base + ".home.y", Y + 0.1D),
                        yaml.getDouble(base + ".home.z", cz + 0.5D), (float) yaml.getDouble(base + ".home.yaw", 0D), (float) yaml.getDouble(base + ".home.pitch", 0D));
                Set<UUID> members = readSet(yaml, base + ".members");
                Set<UUID> trusted = readSet(yaml, base + ".trusted");
                Set<UUID> denied = readSet(yaml, base + ".denied");
                Set<Integer> cells = new LinkedHashSet<Integer>();
                for (Integer cell : yaml.getIntegerList(base + ".cells")) if (cell != null && cell >= 0) cells.add(cell);
                if (cells.isEmpty()) cells.add(index);
                plots.put(owner, new PlotData(owner, index, cx, cz, home, members, trusted, denied, cells,
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
            yaml.set(base + ".cells", new ArrayList<Integer>(plot.cells));
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

    public enum MergeDirection {
        NORTH(0, -1), EAST(1, 0), SOUTH(0, 1), WEST(-1, 0);
        final int dx; final int dz;
        MergeDirection(int dx, int dz) { this.dx = dx; this.dz = dz; }

        public static MergeDirection parse(String raw) {
            if (raw == null) return null;
            String value = raw.toLowerCase(java.util.Locale.ROOT);
            if ("n".equals(value) || "north".equals(value) || "nord".equals(value)) return NORTH;
            if ("e".equals(value) || "east".equals(value) || "ost".equals(value)) return EAST;
            if ("s".equals(value) || "south".equals(value) || "sued".equals(value) || "süd".equals(value)) return SOUTH;
            if ("w".equals(value) || "west".equals(value)) return WEST;
            return null;
        }
    }

    public enum MergeResult {
        SUCCESS, NO_PLOT, NOT_ON_OWN_PLOT, INVALID_DIRECTION, WORLD_EDGE, ALREADY_MERGED, CLAIMED_BY_OTHER
    }

    public static final class PlotData {
        public final UUID owner; public final int index; public final int centerX; public final int centerZ;
        private Location home; private final Set<UUID> members; private final Set<UUID> trusted; private final Set<UUID> denied;
        private final Set<Integer> cells;
        private boolean pvp; private boolean explosions; private boolean fire; private boolean mobSpawning;

        PlotData(UUID owner, int index, int centerX, int centerZ, Location home, Set<UUID> members,
                 Set<UUID> trusted, Set<UUID> denied, Set<Integer> cells,
                 boolean pvp, boolean explosions, boolean fire, boolean mobSpawning) {
            this.owner = owner; this.index = index; this.centerX = centerX; this.centerZ = centerZ; this.home = home;
            this.members = members; this.trusted = trusted; this.denied = denied; this.cells = cells;
            this.pvp = pvp; this.explosions = explosions; this.fire = fire; this.mobSpawning = mobSpawning;
        }
        public Location getHome() { return home.clone(); }
        public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }
        public Set<UUID> getTrusted() { return Collections.unmodifiableSet(trusted); }
        public Set<UUID> getDenied() { return Collections.unmodifiableSet(denied); }
        public Set<Integer> getCells() { return Collections.unmodifiableSet(cells); }
        public int getCellCount() { return cells.size(); }
        public boolean isPvp() { return pvp; }
        public boolean isExplosions() { return explosions; }
        public boolean isFire() { return fire; }
        public boolean isMobSpawning() { return mobSpawning; }
        public int getMinX() {
            int min = Integer.MAX_VALUE;
            for (Integer cell : cells) min = Math.min(min, Math.floorMod(cell, GRID_WIDTH) * SPACING);
            return min == Integer.MAX_VALUE ? 0 : min;
        }
        public int getMaxX() {
            int max = Integer.MIN_VALUE;
            for (Integer cell : cells) max = Math.max(max, Math.floorMod(cell, GRID_WIDTH) * SPACING + PLOT_SIZE - 1);
            return max == Integer.MIN_VALUE ? 0 : max;
        }
        public int getMinZ() {
            int min = Integer.MAX_VALUE;
            for (Integer cell : cells) min = Math.min(min, Math.floorDiv(cell, GRID_WIDTH) * SPACING);
            return min == Integer.MAX_VALUE ? 0 : min;
        }
        public int getMaxZ() {
            int max = Integer.MIN_VALUE;
            for (Integer cell : cells) max = Math.max(max, Math.floorDiv(cell, GRID_WIDTH) * SPACING + PLOT_SIZE - 1);
            return max == Integer.MIN_VALUE ? 0 : max;
        }
        public boolean contains(Location l) {
            if (l == null || l.getWorld() == null || !WORLD_NAME.equals(l.getWorld().getName())) return false;
            int gx = Math.floorDiv(l.getBlockX(), SPACING);
            int gz = Math.floorDiv(l.getBlockZ(), SPACING);
            int localX = Math.floorMod(l.getBlockX(), SPACING);
            int localZ = Math.floorMod(l.getBlockZ(), SPACING);
            return localX < PLOT_SIZE && localZ < PLOT_SIZE && cells.contains(gz * GRID_WIDTH + gx);
        }
    }
}
