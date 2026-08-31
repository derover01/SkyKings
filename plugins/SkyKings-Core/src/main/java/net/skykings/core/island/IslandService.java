package net.skykings.core.island;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

/** Persistente private Islands mit Owner, Trust-Liste, Home, Welcome-Punkt und fester Schutzregion. */
public final class IslandService implements IslandAccessService {
    public static final String WORLD_NAME = "SkyIslands";
    public static final int SPACING = 256;
    public static final int RADIUS = 64;
    public static final int Y = 100;

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, IslandData> islands = new HashMap<UUID, IslandData>();
    private int nextIndex;

    public IslandService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "islands.yml");
        ensureWorld();
        load();
        Bukkit.getServicesManager().register(IslandAccessService.class, this, plugin, ServicePriority.Normal);
    }

    public World ensureWorld() {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world != null) return world;
        WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.generator(new IslandVoidGenerator());
        creator.generateStructures(false);
        world = creator.createWorld();
        if (world != null) {
            world.setSpawnLocation(0, Y, 0);
            world.setPVP(false);
        }
        return world;
    }

    public synchronized boolean create(Player player) {
        UUID uuid = player.getUniqueId();
        if (islands.containsKey(uuid)) return false;
        World world = ensureWorld();
        if (world == null) return false;
        int index = nextIndex++;
        int gridX = index % 100;
        int gridZ = index / 100;
        int centerX = gridX * SPACING;
        int centerZ = gridZ * SPACING;
        Location home = new Location(world, centerX + 0.5D, Y + 1D, centerZ + 0.5D);
        IslandData data = new IslandData(uuid, index, centerX, centerZ, home, null, new HashSet<UUID>());
        islands.put(uuid, data);
        generateStarterIsland(world, centerX, centerZ);
        save();
        player.teleport(home);
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, 1.25F);
        return true;
    }

    private void generateStarterIsland(World world, int cx, int cz) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                world.getBlockAt(cx + x, Y - 2, cz + z).setType(Material.DIRT);
                world.getBlockAt(cx + x, Y - 1, cz + z).setType(Material.GRASS);
            }
        }
        world.getBlockAt(cx, Y - 3, cz).setType(Material.BEDROCK);
        Block chestBlock = world.getBlockAt(cx + 2, Y, cz);
        chestBlock.setType(Material.CHEST);
        if (chestBlock.getState() instanceof Chest) {
            Chest chest = (Chest) chestBlock.getState();
            chest.getBlockInventory().clear();
            chest.getBlockInventory().setItem(0, new ItemStack(Material.COBBLESTONE, 32));
            chest.getBlockInventory().setItem(2, new ItemStack(Material.LOG, 16));
            chest.getBlockInventory().setItem(4, new ItemStack(Material.SAPLING, 4));
            chest.getBlockInventory().setItem(6, new ItemStack(Material.BREAD, 16));
            chest.getBlockInventory().setItem(9, new ItemStack(Material.WATER_BUCKET, 1));
            chest.getBlockInventory().setItem(11, new ItemStack(Material.LAVA_BUCKET, 1));
            chest.getBlockInventory().setItem(13, new ItemStack(Material.TORCH, 16));
            chest.getBlockInventory().setItem(15, new ItemStack(Material.BONE_MEAL, 8));
            chest.update(true);
        }
    }

    public synchronized IslandData get(UUID owner) { return islands.get(owner); }

    public synchronized IslandData findAt(Location location) {
        if (!isIslandWorld(location)) return null;
        for (IslandData island : islands.values()) if (island.contains(location)) return island;
        return null;
    }

    public synchronized boolean trust(UUID owner, UUID target) {
        IslandData island = islands.get(owner);
        if (island == null || owner.equals(target)) return false;
        boolean changed = island.trusted.add(target);
        if (changed) save();
        return changed;
    }

    public synchronized boolean untrust(UUID owner, UUID target) {
        IslandData island = islands.get(owner);
        if (island == null) return false;
        boolean changed = island.trusted.remove(target);
        if (changed) save();
        return changed;
    }

    public synchronized boolean setHome(UUID owner, Location location) {
        IslandData island = islands.get(owner);
        if (island == null || !island.contains(location)) return false;
        island.home = location.clone();
        save();
        return true;
    }

    public synchronized boolean setWelcome(UUID owner, Location signLocation, float yaw, float pitch) {
        IslandData island = islands.get(owner);
        if (island == null || signLocation == null || !island.contains(signLocation)) return false;
        Location welcome = signLocation.clone().add(0.5D, 0.2D, 0.5D);
        welcome.setYaw(yaw);
        welcome.setPitch(pitch);
        island.welcome = welcome;
        save();
        return true;
    }

    public synchronized boolean clearWelcomeAt(UUID owner, Location blockLocation) {
        IslandData island = islands.get(owner);
        if (island == null || island.welcome == null || blockLocation == null) return false;
        if (island.welcome.getWorld() == null || blockLocation.getWorld() == null
                || !island.welcome.getWorld().getName().equals(blockLocation.getWorld().getName())) return false;
        int wx = island.welcome.getBlockX();
        int wy = island.welcome.getBlockY();
        int wz = island.welcome.getBlockZ();
        if (wx != blockLocation.getBlockX() || wy != blockLocation.getBlockY() || wz != blockLocation.getBlockZ()) return false;
        island.welcome = null;
        save();
        return true;
    }

    public synchronized boolean hasWelcome(UUID owner) {
        IslandData island = islands.get(owner);
        return island != null && validateWelcome(island);
    }

    public synchronized Location getWelcome(UUID owner) {
        IslandData island = islands.get(owner);
        if (island == null || !validateWelcome(island)) return null;
        return island.welcome.clone();
    }

    private boolean validateWelcome(IslandData island) {
        if (island.welcome == null || island.welcome.getWorld() == null) return false;
        Block block = island.welcome.getBlock();
        Material type = block.getType();
        if (type != Material.SIGN_POST && type != Material.WALL_SIGN) {
            island.welcome = null;
            save();
            return false;
        }
        return true;
    }

    public synchronized List<UUID> trusted(UUID owner) {
        IslandData island = islands.get(owner);
        if (island == null) return Collections.emptyList();
        return new ArrayList<UUID>(island.trusted);
    }

    @Override public boolean isIslandWorld(Location location) {
        return location != null && location.getWorld() != null && WORLD_NAME.equals(location.getWorld().getName());
    }

    @Override public synchronized boolean hasIsland(UUID owner) { return islands.containsKey(owner); }

    @Override public synchronized boolean canBuild(UUID player, Location location) {
        IslandData island = findAt(location);
        return island != null && (island.owner.equals(player) || island.trusted.contains(player));
    }

    @Override public synchronized boolean ownsLocation(UUID player, Location location) {
        IslandData island = findAt(location);
        return island != null && island.owner.equals(player);
    }

    public void teleportHome(Player player, UUID owner) {
        IslandData island = get(owner);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "Diese Insel existiert nicht.");
            return;
        }
        player.teleport(island.home.clone());
        player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 0.7F, 1.2F);
    }

    public boolean visit(Player player, UUID owner) {
        IslandData island = get(owner);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "Dieser Spieler besitzt keine Insel.");
            return false;
        }
        Location welcome = getWelcome(owner);
        if (welcome == null) {
            player.sendMessage(ChatColor.RED + "Diese Insel ist privat. " + ChatColor.GRAY + "Der Owner hat kein [Welcome]-Schild gesetzt.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1.0F);
            return false;
        }
        player.teleport(welcome);
        player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 0.8F, 1.35F);
        player.sendMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "SKYKINGS ISLANDS " + ChatColor.GRAY + "Du besuchst eine öffentliche Insel.");
        return true;
    }

    private void load() {
        islands.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        nextIndex = Math.max(0, yaml.getInt("next-index", 0));
        ConfigurationSection root = yaml.getConfigurationSection("islands");
        if (root == null) return;
        World world = ensureWorld();
        for (String raw : root.getKeys(false)) {
            try {
                UUID owner = UUID.fromString(raw);
                String base = "islands." + raw;
                int index = yaml.getInt(base + ".index");
                int cx = yaml.getInt(base + ".center-x");
                int cz = yaml.getInt(base + ".center-z");
                double hx = yaml.getDouble(base + ".home.x", cx + 0.5D);
                double hy = yaml.getDouble(base + ".home.y", Y + 1D);
                double hz = yaml.getDouble(base + ".home.z", cz + 0.5D);
                float yaw = (float) yaml.getDouble(base + ".home.yaw", 0D);
                float pitch = (float) yaml.getDouble(base + ".home.pitch", 0D);
                Location welcome = null;
                if (yaml.contains(base + ".welcome.x")) {
                    welcome = new Location(world,
                            yaml.getDouble(base + ".welcome.x"), yaml.getDouble(base + ".welcome.y"), yaml.getDouble(base + ".welcome.z"),
                            (float) yaml.getDouble(base + ".welcome.yaw", 0D), (float) yaml.getDouble(base + ".welcome.pitch", 0D));
                }
                Set<UUID> trusted = new HashSet<UUID>();
                for (String id : yaml.getStringList(base + ".trusted")) {
                    try { trusted.add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) { }
                }
                islands.put(owner, new IslandData(owner, index, cx, cz, new Location(world, hx, hy, hz, yaw, pitch), welcome, trusted));
                if (index >= nextIndex) nextIndex = index + 1;
            } catch (IllegalArgumentException ignored) { }
        }
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("next-index", nextIndex);
        for (IslandData island : islands.values()) {
            String base = "islands." + island.owner;
            yaml.set(base + ".index", island.index);
            yaml.set(base + ".center-x", island.centerX);
            yaml.set(base + ".center-z", island.centerZ);
            yaml.set(base + ".home.x", island.home.getX());
            yaml.set(base + ".home.y", island.home.getY());
            yaml.set(base + ".home.z", island.home.getZ());
            yaml.set(base + ".home.yaw", island.home.getYaw());
            yaml.set(base + ".home.pitch", island.home.getPitch());
            if (island.welcome != null) {
                yaml.set(base + ".welcome.x", island.welcome.getX());
                yaml.set(base + ".welcome.y", island.welcome.getY());
                yaml.set(base + ".welcome.z", island.welcome.getZ());
                yaml.set(base + ".welcome.yaw", island.welcome.getYaw());
                yaml.set(base + ".welcome.pitch", island.welcome.getPitch());
            }
            List<String> trust = new ArrayList<String>();
            for (UUID uuid : island.trusted) trust.add(uuid.toString());
            yaml.set(base + ".trusted", trust);
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("islands.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    public static final class IslandData {
        public final UUID owner;
        public final int index;
        public final int centerX;
        public final int centerZ;
        private Location home;
        private Location welcome;
        private final Set<UUID> trusted;

        IslandData(UUID owner, int index, int centerX, int centerZ, Location home, Location welcome, Set<UUID> trusted) {
            this.owner = owner;
            this.index = index;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.home = home;
            this.welcome = welcome;
            this.trusted = trusted;
        }

        public Location getHome() { return home.clone(); }
        public Location getWelcome() { return welcome == null ? null : welcome.clone(); }
        public Set<UUID> getTrusted() { return Collections.unmodifiableSet(trusted); }
        public boolean contains(Location location) {
            if (location == null || location.getWorld() == null || !WORLD_NAME.equals(location.getWorld().getName())) return false;
            return Math.abs(location.getX() - centerX) <= RADIUS && Math.abs(location.getZ() - centerZ) <= RADIUS;
        }
    }
}
