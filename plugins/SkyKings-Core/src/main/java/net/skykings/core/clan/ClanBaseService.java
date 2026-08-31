package net.skykings.core.clan;

import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Kleine geschuetzte Clan-Hubs in einer separaten offenen Normalwelt, kein Factions-Ersatz. */
public final class ClanBaseService implements Listener {
    public static final String WORLD_NAME = "SkyClanWorld";
    private static final int CLAIM_RADIUS = 16;
    private static final int SPACING = 256;
    private static final int GRID_WIDTH = 8;

    public static final class BaseData {
        final UUID clanId;
        final int index;
        final int centerX;
        final int centerZ;
        double homeX, homeY, homeZ;
        float yaw, pitch;

        BaseData(UUID clanId, int index, int centerX, int centerZ) {
            this.clanId = clanId;
            this.index = index;
            this.centerX = centerX;
            this.centerZ = centerZ;
        }

        public int getIndex() { return index; }
        public int getCenterX() { return centerX; }
        public int getCenterZ() { return centerZ; }
    }

    private final JavaPlugin plugin;
    private final ClanService clans;
    private final File file;
    private final Map<UUID, BaseData> bases = new LinkedHashMap<UUID, BaseData>();
    private int nextIndex;

    public ClanBaseService(JavaPlugin plugin, ClanService clans) {
        this.plugin = plugin;
        this.clans = clans;
        this.file = new File(plugin.getDataFolder(), "clan-bases.yml");
        load();
        ensureWorld();
    }

    public BaseData get(UUID clanId) { return clanId == null ? null : bases.get(clanId); }

    public boolean create(Player owner) {
        ClanService.Clan clan = clans.getClan(owner.getUniqueId());
        if (clan == null || !clan.isOwner(owner.getUniqueId()) || bases.containsKey(clan.getId())) return false;
        World world = ensureWorld();
        if (world == null) return false;

        int index = nextIndex++;
        int gridX = index % GRID_WIDTH;
        int gridZ = index / GRID_WIDTH;
        int x = gridX * SPACING;
        int z = gridZ * SPACING;
        int surfaceY = Math.max(65, Math.min(180, world.getHighestBlockYAt(x, z) + 1));

        BaseData base = new BaseData(clan.getId(), index, x, z);
        buildStarterHub(world, x, surfaceY, z, clan);
        base.homeX = x + 0.5D;
        base.homeY = surfaceY + 1D;
        base.homeZ = z + 4.5D;
        base.yaw = 180F;
        base.pitch = 0F;
        bases.put(clan.getId(), base);
        save();
        owner.teleport(home(base));
        SoundFeedback.reward(owner);
        return true;
    }

    public boolean teleport(Player player) {
        ClanService.Clan clan = clans.getClan(player.getUniqueId());
        BaseData base = clan == null ? null : bases.get(clan.getId());
        if (base == null) return false;
        player.teleport(home(base));
        SoundFeedback.success(player);
        return true;
    }

    public boolean setHome(Player player) {
        ClanService.Clan clan = clans.getClan(player.getUniqueId());
        if (clan == null || !clan.isOwner(player.getUniqueId())) return false;
        BaseData base = bases.get(clan.getId());
        Location location = player.getLocation();
        if (base == null || !contains(base, location)) return false;
        base.homeX = location.getX(); base.homeY = location.getY(); base.homeZ = location.getZ();
        base.yaw = location.getYaw(); base.pitch = location.getPitch();
        save();
        return true;
    }

    public void remove(UUID clanId) {
        if (clanId != null && bases.remove(clanId) != null) save();
    }

    public boolean contains(BaseData base, Location location) {
        if (base == null || location == null || location.getWorld() == null || !WORLD_NAME.equals(location.getWorld().getName())) return false;
        return Math.abs(location.getX() - base.centerX) <= CLAIM_RADIUS
                && Math.abs(location.getZ() - base.centerZ) <= CLAIM_RADIUS;
    }

    private BaseData find(Location location) {
        if (location == null || location.getWorld() == null || !WORLD_NAME.equals(location.getWorld().getName())) return null;
        for (BaseData base : bases.values()) if (contains(base, location)) return base;
        return null;
    }

    private boolean canModify(Player player, BaseData base) {
        if (player.hasPermission("skykings.admin.clanbase.bypass")) return true;
        ClanService.Clan clan = clans.getClan(player.getUniqueId());
        return clan != null && clan.getId().equals(base.clanId);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        BaseData base = find(event.getBlock().getLocation());
        if (base != null && !canModify(event.getPlayer(), base)) deny(event.getPlayer(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        BaseData base = find(event.getBlock().getLocation());
        if (base != null && !canModify(event.getPlayer(), base)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(UiTheme.DANGER + "Diese Clan Base ist geschuetzt.");
            SoundFeedback.error(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        BaseData base = find(event.getClickedBlock().getLocation());
        if (base == null || canModify(event.getPlayer(), base)) return;
        switch (event.getClickedBlock().getType()) {
            case CHEST:
            case TRAPPED_CHEST:
            case FURNACE:
            case BURNING_FURNACE:
            case ANVIL:
            case ENCHANTMENT_TABLE:
            case WORKBENCH:
                event.setCancelled(true);
                event.getPlayer().sendMessage(UiTheme.DANGER + "Clan Vault nur fuer Mitglieder.");
                SoundFeedback.error(event.getPlayer());
                break;
            default: break;
        }
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) if (find(iterator.next().getLocation()) != null) iterator.remove();
    }

    private void deny(Player player, BlockBreakEvent event) {
        event.setCancelled(true);
        player.sendMessage(UiTheme.DANGER + "Diese Clan Base ist geschuetzt.");
        SoundFeedback.error(player);
    }

    private World ensureWorld() {
        World existing = Bukkit.getWorld(WORLD_NAME);
        if (existing != null) return existing;
        WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.type(WorldType.NORMAL);
        creator.generateStructures(true);
        return Bukkit.createWorld(creator);
    }

    private void buildStarterHub(World world, int cx, int y, int cz, ClanService.Clan clan) {
        // Kleine 9x9 Lodge auf normalem Terrain; bewusst kosmetisch und spaeter frei umbaubar.
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            world.getBlockAt(cx + x, y, cz + z).setType(Material.SMOOTH_BRICK);
        }
        for (int level = 1; level <= 4; level++) {
            for (int x = -4; x <= 4; x++) {
                setWall(world, cx + x, y + level, cz - 4, level, x, -4);
                setWall(world, cx + x, y + level, cz + 4, level, x, 4);
            }
            for (int z = -3; z <= 3; z++) {
                setWall(world, cx - 4, y + level, cz + z, level, -4, z);
                setWall(world, cx + 4, y + level, cz + z, level, 4, z);
            }
        }
        // Eingang Suedseite.
        world.getBlockAt(cx, y + 1, cz + 4).setType(Material.AIR);
        world.getBlockAt(cx, y + 2, cz + 4).setType(Material.AIR);
        // Flaches Dach; Spieler duerfen die Basis danach selbst gestalten.
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) world.getBlockAt(cx + x, y + 5, cz + z).setType(Material.STEP);
        world.getBlockAt(cx, y + 1, cz).setType(Material.CHEST);
        world.getBlockAt(cx - 2, y + 1, cz).setType(Material.WORKBENCH);
        world.getBlockAt(cx + 2, y + 1, cz).setType(Material.ANVIL);
    }

    private void setWall(World world, int x, int y, int z, int level, int relX, int relZ) {
        // Ein paar Fenster halten den Starterraum offen und nicht bunkerartig.
        boolean window = (level == 2 || level == 3) && ((Math.abs(relX) == 2 && Math.abs(relZ) == 4) || (Math.abs(relZ) == 2 && Math.abs(relX) == 4));
        world.getBlockAt(x, y, z).setType(window ? Material.GLASS : Material.SMOOTH_BRICK);
    }

    private Location home(BaseData base) {
        World world = ensureWorld();
        return new Location(world, base.homeX, base.homeY, base.homeZ, base.yaw, base.pitch);
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        nextIndex = Math.max(0, yaml.getInt("next-index", 0));
        ConfigurationSection root = yaml.getConfigurationSection("bases");
        if (root == null) return;
        for (String raw : root.getKeys(false)) {
            try {
                UUID clanId = UUID.fromString(raw);
                String base = "bases." + raw + ".";
                BaseData data = new BaseData(clanId, yaml.getInt(base + "index"), yaml.getInt(base + "center-x"), yaml.getInt(base + "center-z"));
                data.homeX = yaml.getDouble(base + "home.x"); data.homeY = yaml.getDouble(base + "home.y"); data.homeZ = yaml.getDouble(base + "home.z");
                data.yaw = (float) yaml.getDouble(base + "home.yaw"); data.pitch = (float) yaml.getDouble(base + "home.pitch");
                bases.put(clanId, data);
                nextIndex = Math.max(nextIndex, data.index + 1);
            } catch (RuntimeException ignored) { }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("next-index", nextIndex);
        for (BaseData data : bases.values()) {
            String base = "bases." + data.clanId + ".";
            yaml.set(base + "index", data.index); yaml.set(base + "center-x", data.centerX); yaml.set(base + "center-z", data.centerZ);
            yaml.set(base + "home.x", data.homeX); yaml.set(base + "home.y", data.homeY); yaml.set(base + "home.z", data.homeZ);
            yaml.set(base + "home.yaw", data.yaw); yaml.set(base + "home.pitch", data.pitch);
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("clan-bases.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
