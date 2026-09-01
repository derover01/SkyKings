package net.skykings.combat.map.builder;

import net.skykings.combat.map.builder.v3.V3Canvas;
import net.skykings.combat.map.builder.v3.V3Structures;
import net.skykings.combat.map.builder.v3.V3Terrain;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Separate SkyKings-Eventwelt im Stil der alten SkyPvP-Fantasyfestung.
 * Hub + Duel + LMS + Tournament + Juggernaut bleiben als eigene schwebende Inseln klar lesbar.
 */
public final class SkyKingsEventMapBuilder {
    public static final int Y = 108;
    private static final int BLOCKS_PER_TICK = 5500;

    private final JavaPlugin plugin;
    private final World world;
    private final Player initiator;
    private final V3Canvas c;

    public SkyKingsEventMapBuilder(JavaPlugin plugin, World world, Player initiator) {
        this.plugin = plugin;
        this.world = world;
        this.initiator = initiator;
        this.c = new V3Canvas(world);
    }

    public void start() {
        plan();
        final List<V3Canvas.BlockPlacement> blocks = c.getPlacements();
        final int total = blocks.size();
        initiator.sendMessage(ChatColor.GOLD + "SkyKings Event-Festung: " + ChatColor.WHITE + total
                + ChatColor.GRAY + " Bloecke werden serverfreundlich aufgebaut.");

        new BukkitRunnable() {
            private int index;
            @Override public void run() {
                int end = Math.min(total, index + BLOCKS_PER_TICK);
                while (index < end) {
                    V3Canvas.BlockPlacement p = blocks.get(index++);
                    world.getBlockAt(p.x, p.y, p.z).setType(p.material, false);
                }
                if (index >= total) {
                    cancel();
                    finish();
                    return;
                }
                if (index % (BLOCKS_PER_TICK * 10) < BLOCKS_PER_TICK) {
                    initiator.sendMessage(ChatColor.DARK_GRAY + "Event-Map " + ChatColor.YELLOW
                            + ((index * 100L) / Math.max(1, total)) + "%");
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void plan() {
        buildHub();
        buildDuelArena();
        buildLmsArena();
        buildTournamentArena();
        buildJuggernautArena();
        buildVisualLinks();
    }

    private void buildHub() {
        V3Terrain.island(c, 0, Y - 2, 0, 43, 38, 20, 8101L,
                Material.GRASS, Material.DIRT, Material.STONE);
        c.disc(0, Y, 0, 25, Material.SMOOTH_BRICK);
        c.ring(0, Y + 1, 0, 25, 2, Material.MOSSY_COBBLESTONE);
        c.disc(0, Y + 1, 0, 7, Material.QUARTZ_BLOCK);
        c.ring(0, Y + 2, 0, 8, 2, Material.GOLD_BLOCK);
        c.set(0, Y + 2, 0, Material.BEACON);
        V3Structures.watchTower(c, -30, Y, -24, 4, 15);
        V3Structures.watchTower(c, 30, Y, -24, 4, 15);
        V3Structures.watchTower(c, -30, Y, 24, 4, 15);
        V3Structures.watchTower(c, 30, Y, 24, 4, 15);
        V3Structures.brokenArch(c, 0, Y + 1, -31, 12, 9);
        V3Structures.brokenArch(c, 31, Y + 1, 0, 12, 9);
        V3Structures.brokenArch(c, 0, Y + 1, 31, 12, 9);
        V3Structures.brokenArch(c, -31, Y + 1, 0, 12, 9);
        c.disc(0, Y + 2, 12, 3, Material.EMERALD_BLOCK);
        c.set(0, Y + 3, 12, Material.GLOWSTONE);
        V3Structures.customTree(c, -22, Y + 1, 8, 8);
        V3Structures.customTree(c, 22, Y + 1, 8, 8);
        lamps(0, Y + 1, 0, 20);
    }

    private void buildDuelArena() {
        int cx = 0, cz = -122;
        V3Terrain.island(c, cx, Y - 4, cz, 49, 39, 23, 8201L, Material.GRASS, Material.DIRT, Material.STONE);
        c.fill(cx - 31, Y, cz - 24, cx + 31, Y, cz + 24, Material.SMOOTH_BRICK);
        c.ring(cx, Y + 1, cz, 29, 2, Material.MOSSY_COBBLESTONE);
        for (int x = cx - 31; x <= cx + 31; x++) {
            c.set(x, Y + 1, cz - 24, Material.NETHER_BRICK);
            c.set(x, Y + 1, cz + 24, Material.NETHER_BRICK);
            if ((x - cx) % 4 == 0) c.set(x, Y + 2, cz - 24, Material.IRON_FENCE);
            if ((x - cx) % 4 == 0) c.set(x, Y + 2, cz + 24, Material.IRON_FENCE);
        }
        for (int z = cz - 24; z <= cz + 24; z++) {
            c.set(cx - 31, Y + 1, z, Material.NETHER_BRICK);
            c.set(cx + 31, Y + 1, z, Material.NETHER_BRICK);
        }
        V3Structures.brokenArch(c, cx, Y + 1, cz - 24, 14, 8);
        V3Structures.brokenArch(c, cx, Y + 1, cz + 24, 14, 8);
        V3Structures.watchTower(c, cx - 35, Y, cz - 27, 4, 13);
        V3Structures.watchTower(c, cx + 35, Y, cz - 27, 4, 13);
        V3Structures.watchTower(c, cx - 35, Y, cz + 27, 4, 13);
        V3Structures.watchTower(c, cx + 35, Y, cz + 27, 4, 13);
        c.disc(cx - 20, Y + 1, cz, 2, Material.GOLD_BLOCK);
        c.disc(cx + 20, Y + 1, cz, 2, Material.DIAMOND_BLOCK);
        spectator(cx, Y + 8, cz + 34, 9);
    }

    private void buildLmsArena() {
        int cx = 132, cz = 0;
        V3Terrain.island(c, cx, Y - 5, cz, 62, 56, 31, 8301L, Material.GRASS, Material.DIRT, Material.STONE);
        V3Terrain.island(c, cx + 22, Y - 2, cz - 18, 31, 27, 18, 8302L, Material.GRASS, Material.DIRT, Material.STONE);
        c.disc(cx, Y, cz, 13, Material.GRAVEL);
        c.ring(cx, Y + 1, cz, 14, 2, Material.SMOOTH_BRICK);
        V3Structures.ruinedHouse(c, cx - 27, Y + 1, cz - 21, 13, 11);
        V3Structures.ruinedHouse(c, cx + 30, Y + 1, cz + 18, 15, 11);
        V3Structures.medievalHouse(c, cx - 28, Y + 1, cz + 23, 11, 9, false);
        V3Structures.watchTower(c, cx + 39, Y, cz - 31, 5, 18);
        V3Structures.customTree(c, cx - 43, Y, cz, 9);
        V3Structures.customTree(c, cx + 10, Y, cz + 40, 10);
        V3Terrain.rock(c, cx + 2, Y, cz - 35, 7, 8310L);
        V3Terrain.rock(c, cx - 12, Y, cz + 28, 5, 8311L);
        c.disc(cx - 47, Y + 1, cz - 39, 3, Material.EMERALD_BLOCK);
        int[][] spawns = {{0,-39},{27,-28},{40,0},{28,28},{0,40},{-28,28},{-40,0},{-27,-28}};
        for (int[] s : spawns) marker(cx + s[0], Y + 1, cz + s[1], Material.GLOWSTONE);
        spectator(cx + 52, Y + 13, cz + 42, 10);
    }

    private void buildTournamentArena() {
        int cx = 0, cz = 132;
        V3Terrain.island(c, cx, Y - 5, cz, 59, 52, 27, 8401L, Material.GRASS, Material.DIRT, Material.STONE);
        c.disc(cx, Y, cz, 39, Material.SMOOTH_BRICK);
        c.disc(cx, Y + 1, cz, 30, Material.GRASS);
        c.ring(cx, Y + 1, cz, 39, 5, Material.NETHER_BRICK);
        c.ring(cx, Y + 4, cz, 39, 3, Material.SMOOTH_BRICK);
        c.ring(cx, Y + 7, cz, 39, 2, Material.SMOOTH_BRICK);
        V3Structures.brokenArch(c, cx, Y + 1, cz - 39, 14, 10);
        V3Structures.brokenArch(c, cx, Y + 1, cz + 39, 14, 10);
        for (int[] p : new int[][]{{-33,-22},{33,-22},{-33,22},{33,22}}) {
            c.column(cx + p[0], Y + 1, cz + p[1], 12, Material.QUARTZ_BLOCK);
            c.set(cx + p[0], Y + 13, cz + p[1], Material.GOLD_BLOCK);
            c.set(cx + p[0], Y + 14, cz + p[1], Material.GLOWSTONE);
        }
        c.disc(cx, Y + 2, cz - 19, 2, Material.GOLD_BLOCK);
        c.disc(cx, Y + 2, cz + 19, 2, Material.DIAMOND_BLOCK);
        marker(cx - 19, Y + 2, cz, Material.EMERALD_BLOCK);
        marker(cx + 19, Y + 2, cz, Material.REDSTONE_BLOCK);
        spectator(cx + 43, Y + 10, cz, 11);
    }

    private void buildJuggernautArena() {
        int cx = -132, cz = 0;
        V3Terrain.island(c, cx, Y - 4, cz, 55, 48, 28, 8501L, Material.GRASS, Material.DIRT, Material.STONE);
        c.fill(cx - 35, Y, cz - 29, cx + 35, Y, cz + 29, Material.NETHER_BRICK);
        c.disc(cx, Y + 1, cz, 24, Material.SMOOTH_BRICK);
        c.ring(cx, Y + 2, cz, 25, 3, Material.OBSIDIAN);
        V3Structures.watchTower(c, cx - 38, Y, cz - 31, 5, 20);
        V3Structures.watchTower(c, cx + 38, Y, cz - 31, 5, 20);
        V3Structures.watchTower(c, cx - 38, Y, cz + 31, 5, 20);
        V3Structures.watchTower(c, cx + 38, Y, cz + 31, 5, 20);
        c.fill(cx - 6, Y + 2, cz - 6, cx + 6, Y + 2, cz + 6, Material.QUARTZ_BLOCK);
        c.fill(cx - 4, Y + 3, cz + 2, cx + 4, Y + 3, cz + 6, Material.GOLD_BLOCK);
        c.fill(cx - 2, Y + 4, cz + 4, cx + 2, Y + 8, cz + 6, Material.OBSIDIAN);
        marker(cx, Y + 4, cz, Material.REDSTONE_BLOCK);
        int[][] spawns = {{0,-22},{22,0},{0,22},{-22,0},{16,-16},{16,16},{-16,16},{-16,-16}};
        for (int[] s : spawns) marker(cx + s[0], Y + 2, cz + s[1], Material.GLOWSTONE);
        c.disc(cx - 31, Y + 2, cz - 23, 3, Material.EMERALD_BLOCK);
        spectator(cx, Y + 13, cz + 37, 10);
    }

    private void buildVisualLinks() {
        V3Structures.stoneBridge(c, 0, Y + 1, -34, 0, Y + 1, -52, 5);
        V3Structures.stoneBridge(c, 34, Y + 1, 0, 52, Y + 1, 0, 5);
        V3Structures.stoneBridge(c, 0, Y + 1, 34, 0, Y + 1, 52, 5);
        V3Structures.stoneBridge(c, -34, Y + 1, 0, -52, Y + 1, 0, 5);
    }

    private void lamps(int cx, int y, int cz, int radius) {
        for (int[] p : new int[][]{{-radius,0},{radius,0},{0,-radius},{0,radius}}) {
            c.column(cx + p[0], y, cz + p[1], 4, Material.FENCE);
            c.set(cx + p[0], y + 4, cz + p[1], Material.GLOWSTONE);
        }
    }

    private void marker(int x, int y, int z, Material material) {
        c.disc(x, y, z, 2, material);
        c.set(x, y + 1, z, Material.GLOWSTONE);
    }

    private void spectator(int cx, int y, int cz, int radius) {
        c.disc(cx, y, cz, radius, Material.QUARTZ_BLOCK);
        c.ring(cx, y + 1, cz, radius, 2, Material.GOLD_BLOCK);
        c.ring(cx, y + 2, cz, radius, 1, Material.IRON_FENCE);
    }

    private void finish() {
        world.setSpawnLocation(0, Y + 3, 12);
        world.setPVP(true);
        world.setStorm(false);
        world.setThundering(false);
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setTime(6000L);
        Location hub = new Location(world, 0.5D, Y + 3D, 12.5D, 180F, 0F);
        if (initiator.isOnline()) initiator.teleport(hub);
        initiator.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "EVENT-MAP FERTIG");
        initiator.sendMessage(ChatColor.GRAY + "Hub + Duel (Nord) + LMS (Ost) + Tournament (Sued) + Juggernaut (West).");
        initiator.sendMessage(ChatColor.YELLOW + "Naechster Schritt: Arena-Punkte mit /eventarena set ... speichern.");
    }
}
