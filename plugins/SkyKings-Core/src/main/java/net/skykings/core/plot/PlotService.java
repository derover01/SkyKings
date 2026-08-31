package net.skykings.core.plot;

import net.skykings.core.island.IslandVoidGenerator;
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

/** Eigene PlotSquared-inspirierte Claim-Welt mit 65x65 Plots. */
public final class PlotService implements PlotAccessService {
    public static final String WORLD_NAME = "SkyPlots";
    public static final int SPACING = 128;
    public static final int RADIUS = 32;
    public static final int Y = 65;

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, PlotData> plots = new HashMap<UUID, PlotData>();
    private int nextIndex;

    public PlotService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "plots.yml");
        ensureWorld();
        load();
        Bukkit.getServicesManager().register(PlotAccessService.class, this, plugin, ServicePriority.Normal);
    }

    public World ensureWorld() {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world != null) return world;
        WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.generator(new IslandVoidGenerator());
        creator.generateStructures(false);
        world = creator.createWorld();
        if (world != null) {
            world.setSpawnLocation(0, Y + 1, 0);
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
        int cx = gx * SPACING;
        int cz = gz * SPACING;
        Location home = new Location(world, cx + 0.5D, Y + 1D, cz + 0.5D);
        PlotData data = new PlotData(uuid, index, cx, cz, home, new HashSet<UUID>());
        plots.put(uuid, data);
        generate(world, cx, cz);
        save();
        player.teleport(home);
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, 1.25F);
        return true;
    }

    /** 65x65 Plot mit Grass-Flaeche, sauberem Rand und kleinem Eingangsweg. */
    private void generate(World world, int cx, int cz) {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                boolean border = Math.abs(x) == RADIUS || Math.abs(z) == RADIUS;
                world.getBlockAt(cx + x, Y - 2, cz + z).setType(Material.DIRT);
                world.getBlockAt(cx + x, Y - 1, cz + z).setType(border ? Material.SMOOTH_BRICK : Material.GRASS);
            }
        }
        world.getBlockAt(cx, Y - 3, cz).setType(Material.BEDROCK);
        for (int z = RADIUS - 3; z <= RADIUS; z++) {
            world.getBlockAt(cx, Y - 1, cz + z).setType(Material.STONE);
            world.getBlockAt(cx - 1, Y - 1, cz + z).setType(Material.STONE);
            world.getBlockAt(cx + 1, Y - 1, cz + z).setType(Material.STONE);
        }
    }

    public synchronized PlotData get(UUID owner) { return plots.get(owner); }
    public synchronized PlotData findAt(Location location) {
        if (!isPlotWorld(location)) return null;
        for (PlotData plot : plots.values()) if (plot.contains(location)) return plot;
        return null;
    }
    public synchronized boolean trust(UUID owner, UUID target) {
        PlotData plot = plots.get(owner); if (plot == null || owner.equals(target)) return false;
        boolean changed = plot.trusted.add(target); if (changed) save(); return changed;
    }
    public synchronized boolean untrust(UUID owner, UUID target) {
        PlotData plot = plots.get(owner); if (plot == null) return false;
        boolean changed = plot.trusted.remove(target); if (changed) save(); return changed;
    }
    public synchronized boolean setHome(UUID owner, Location location) {
        PlotData plot = plots.get(owner); if (plot == null || !plot.contains(location)) return false;
        plot.home = location.clone(); save(); return true;
    }
    public void teleportHome(Player player, UUID owner) {
        PlotData plot = get(owner);
        if (plot == null) { player.sendMessage(ChatColor.RED + "Dieser Plot existiert nicht."); return; }
        player.teleport(plot.home.clone());
        player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 0.7F, 1.2F);
    }

    @Override public boolean isPlotWorld(Location location) { return location != null && location.getWorld() != null && WORLD_NAME.equals(location.getWorld().getName()); }
    @Override public synchronized boolean hasPlot(UUID owner) { return plots.containsKey(owner); }
    @Override public synchronized boolean canBuild(UUID player, Location location) {
        PlotData plot = findAt(location); return plot != null && (plot.owner.equals(player) || plot.trusted.contains(player));
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
                int index = yaml.getInt(base + ".index"); int cx = yaml.getInt(base + ".center-x"); int cz = yaml.getInt(base + ".center-z");
                Location home = new Location(world, yaml.getDouble(base + ".home.x", cx + 0.5D), yaml.getDouble(base + ".home.y", Y + 1D),
                        yaml.getDouble(base + ".home.z", cz + 0.5D), (float) yaml.getDouble(base + ".home.yaw", 0D), (float) yaml.getDouble(base + ".home.pitch", 0D));
                Set<UUID> trusted = new HashSet<UUID>();
                for (String id : yaml.getStringList(base + ".trusted")) try { trusted.add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) { }
                plots.put(owner, new PlotData(owner, index, cx, cz, home, trusted)); if (index >= nextIndex) nextIndex = index + 1;
            } catch (IllegalArgumentException ignored) { }
        }
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration(); yaml.set("next-index", nextIndex);
        for (PlotData plot : plots.values()) {
            String base = "plots." + plot.owner; yaml.set(base + ".index", plot.index); yaml.set(base + ".center-x", plot.centerX); yaml.set(base + ".center-z", plot.centerZ);
            yaml.set(base + ".home.x", plot.home.getX()); yaml.set(base + ".home.y", plot.home.getY()); yaml.set(base + ".home.z", plot.home.getZ());
            yaml.set(base + ".home.yaw", plot.home.getYaw()); yaml.set(base + ".home.pitch", plot.home.getPitch());
            List<String> trust = new ArrayList<String>(); for (UUID uuid : plot.trusted) trust.add(uuid.toString()); yaml.set(base + ".trusted", trust);
        }
        try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); yaml.save(file); }
        catch (IOException ex) { plugin.getLogger().warning("plots.yml konnte nicht gespeichert werden: " + ex.getMessage()); }
    }

    public static final class PlotData {
        public final UUID owner; public final int index; public final int centerX; public final int centerZ;
        private Location home; private final Set<UUID> trusted;
        PlotData(UUID owner, int index, int centerX, int centerZ, Location home, Set<UUID> trusted) {
            this.owner = owner; this.index = index; this.centerX = centerX; this.centerZ = centerZ; this.home = home; this.trusted = trusted;
        }
        public Location getHome() { return home.clone(); }
        public Set<UUID> getTrusted() { return Collections.unmodifiableSet(trusted); }
        public boolean contains(Location l) { return l != null && l.getWorld() != null && WORLD_NAME.equals(l.getWorld().getName())
                && Math.abs(l.getX() - centerX) <= RADIUS && Math.abs(l.getZ() - centerZ) <= RADIUS; }
    }
}
