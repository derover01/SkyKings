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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistente private Islands mit Owner, Trust, Home, Welcome, Level und fester Schutzregion. */
public final class IslandService implements IslandAccessService {
    public static final String WORLD_NAME = "SkyIslands";
    public static final int SPACING = 256;
    public static final int RADIUS = 64;
    public static final int Y = 100;
    private static final long LEVEL_COST = 100L;

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
        Location home = new Location(world, centerX + 0.5D, Y + 0.1D, centerZ + 0.5D);
        IslandData data = new IslandData(uuid, index, centerX, centerZ, home, null, new HashSet<UUID>(), 0L);
        islands.put(uuid, data);
        data.levelPoints = Math.max(300L, generateStarterIsland(world, centerX, centerZ));
        save();
        player.teleport(home);
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.8F, 1.25F);
        return true;
    }

    /** Klassische aSkyBlock-inspirierte Starterinsel: kompakt, Baum, Chest und Void rundherum. */
    private long generateStarterIsland(World world, int cx, int cz) {
        long points = 0L;
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (Math.abs(x) + Math.abs(z) > 5) continue;
                points += set(world, cx + x, Y - 1, cz + z, Material.GRASS);
                points += set(world, cx + x, Y - 2, cz + z, Material.DIRT);
                if (Math.abs(x) <= 2 && Math.abs(z) <= 2) points += set(world, cx + x, Y - 3, cz + z, Material.DIRT);
                if (Math.abs(x) <= 1 && Math.abs(z) <= 1) points += set(world, cx + x, Y - 4, cz + z, Material.DIRT);
            }
        }
        points += set(world, cx, Y - 5, cz, Material.BEDROCK);

        // Klassischer Eichenbaum links vom Spawn.
        for (int y = 0; y < 4; y++) points += set(world, cx - 2, Y + y, cz, Material.LOG);
        for (int y = 3; y <= 4; y++) {
            int radius = y == 3 ? 2 : 1;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && z == 0 && y == 3) continue;
                    points += set(world, cx - 2 + x, Y + y, cz + z, Material.LEAVES);
                }
            }
        }
        points += set(world, cx - 2, Y + 5, cz, Material.LEAVES);

        Block chestBlock = world.getBlockAt(cx + 2, Y, cz);
        chestBlock.setType(Material.CHEST);
        points += blockPoints(Material.CHEST);
        if (chestBlock.getState() instanceof Chest) {
            Chest chest = (Chest) chestBlock.getState();
            chest.getBlockInventory().clear();
            chest.getBlockInventory().setItem(0, new ItemStack(Material.LAVA_BUCKET, 1));
            chest.getBlockInventory().setItem(2, new ItemStack(Material.ICE, 2));
            chest.getBlockInventory().setItem(4, new ItemStack(Material.SAPLING, 2));
            chest.getBlockInventory().setItem(6, new ItemStack(Material.CACTUS, 1));
            chest.getBlockInventory().setItem(8, new ItemStack(Material.SUGAR_CANE, 2));
            chest.getBlockInventory().setItem(10, new ItemStack(Material.MELON, 1));
            chest.getBlockInventory().setItem(12, new ItemStack(Material.PUMPKIN_SEEDS, 2));
            chest.getBlockInventory().setItem(14, new ItemStack(Material.RED_MUSHROOM, 1));
            chest.getBlockInventory().setItem(16, new ItemStack(Material.BROWN_MUSHROOM, 1));
            chest.getBlockInventory().setItem(22, new ItemStack(Material.INK_SACK, 8, (short) 15));
            chest.update(true);
        }
        return points;
    }

    private long set(World world, int x, int y, int z, Material material) {
        world.getBlockAt(x, y, z).setType(material);
        return blockPoints(material);
    }

    /** Einfache, nachvollziehbare Blockwerte; Place/Break ist symmetrisch und damit nicht farmbar. */
    public static long blockPoints(Material material) {
        if (material == null || material == Material.AIR) return 0L;
        switch (material) {
            case DIAMOND_BLOCK: return 500L;
            case EMERALD_BLOCK: return 450L;
            case GOLD_BLOCK: return 250L;
            case IRON_BLOCK: return 100L;
            case LAPIS_BLOCK: return 80L;
            case REDSTONE_BLOCK: return 60L;
            case COAL_BLOCK: return 40L;
            case OBSIDIAN: return 15L;
            case ENCHANTMENT_TABLE: return 25L;
            case BEACON: return 1000L;
            case CHEST: return 5L;
            case LOG: return 2L;
            case LEAVES: return 1L;
            default: return 1L;
        }
    }

    public synchronized void adjustLevel(Location location, long delta) {
        IslandData island = findAt(location);
        if (island == null || delta == 0L) return;
        island.levelPoints = Math.max(0L, island.levelPoints + delta);
        save();
    }

    public synchronized List<IslandData> top(int limit) {
        List<IslandData> list = new ArrayList<IslandData>(islands.values());
        Collections.sort(list, new Comparator<IslandData>() {
            @Override public int compare(IslandData a, IslandData b) {
                return Long.compare(b.levelPoints, a.levelPoints);
            }
        });
        if (list.size() > limit) return new ArrayList<IslandData>(list.subList(0, limit));
        return list;
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
        if (island.welcome.getBlockX() != blockLocation.getBlockX()
                || island.welcome.getBlockY() != blockLocation.getBlockY()
                || island.welcome.getBlockZ() != blockLocation.getBlockZ()) return false;
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
        Material type = island.welcome.getBlock().getType();
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
            player.sendMessage(ChatColor.RED + "Diese Insel ist privat. " + ChatColor.GRAY + "Kein [Welcome]-Schild gesetzt.");
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 0.7F, 1.0F);
            return false;
        }
        player.teleport(welcome);
        player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 0.8F, 1.35F);
        player.sendMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "SKYKINGS ISLANDS " + ChatColor.GRAY + "Du besuchst eine oeffentliche Insel.");
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
                double hy = yaml.getDouble(base + ".home.y", Y + 0.1D);
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
                long levelPoints = Math.max(0L, yaml.getLong(base + ".level-points", 300L));
                islands.put(owner, new IslandData(owner, index, cx, cz,
                        new Location(world, hx, hy, hz, yaw, pitch), welcome, trusted, levelPoints));
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
            yaml.set(base + ".level-points", island.levelPoints);
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
        private long levelPoints;

        IslandData(UUID owner, int index, int centerX, int centerZ, Location home,
                   Location welcome, Set<UUID> trusted, long levelPoints) {
            this.owner = owner;
            this.index = index;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.home = home;
            this.welcome = welcome;
            this.trusted = trusted;
            this.levelPoints = levelPoints;
        }

        public Location getHome() { return home.clone(); }
        public Location getWelcome() { return welcome == null ? null : welcome.clone(); }
        public Set<UUID> getTrusted() { return Collections.unmodifiableSet(trusted); }
        public long getLevelPoints() { return levelPoints; }
        public long getLevel() { return levelPoints / LEVEL_COST; }
        public int getMinX() { return centerX - RADIUS; }
        public int getMaxX() { return centerX + RADIUS; }
        public int getMinZ() { return centerZ - RADIUS; }
        public int getMaxZ() { return centerZ + RADIUS; }

        public boolean contains(Location location) {
            if (location == null || location.getWorld() == null || !WORLD_NAME.equals(location.getWorld().getName())) return false;
            return Math.abs(location.getX() - centerX) <= RADIUS && Math.abs(location.getZ() - centerZ) <= RADIUS;
        }
    }
}
