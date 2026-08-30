package net.skykings.combat.map.builder.v3;

import net.skykings.combat.spawn.SpawnService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * SkyKings Arena V3 - hand-authored composition inspired by classic German OP-SkyPvP.
 * Gameplay landmarks are embedded into believable fantasy architecture instead of isolated utility platforms.
 */
public final class SkyKingsMapBuilderV3 {
    public static final int SPAWN_Y = 216;
    public static final int PVP_Y = 78;
    private static final int BLOCKS_PER_TICK = 6500;

    private final JavaPlugin plugin;
    private final World world;
    private final SpawnService spawnService;
    private final Player initiator;
    private final V3Canvas c;

    public SkyKingsMapBuilderV3(JavaPlugin plugin, World world, SpawnService spawnService, Player initiator) {
        this.plugin = plugin;
        this.world = world;
        this.spawnService = spawnService;
        this.initiator = initiator;
        this.c = new V3Canvas(world);
    }

    public void start() {
        planWorld();
        final List<V3Canvas.BlockPlacement> blocks = c.getPlacements();
        final int total = blocks.size();
        initiator.sendMessage(ChatColor.GOLD + "SkyKings Arena V3: " + ChatColor.WHITE + total
                + ChatColor.GRAY + " platzierte Detail-Bloecke werden gebaut.");

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
                if (index % (BLOCKS_PER_TICK * 15) < BLOCKS_PER_TICK) {
                    initiator.sendMessage(ChatColor.DARK_GRAY + "V3 Build " + ChatColor.YELLOW
                            + ((index * 100L) / Math.max(1, total)) + "%");
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void planWorld() {
        buildSkyCitadel();
        buildFallenCapital();
        buildOldKeep();
        buildCrown();
        buildEndRift();
        buildIronforge();
        buildSkyBazaar();
        buildArcaneSanctum();
        buildAlchemistGarden();
        buildTreasury();
        buildWatchtower();
        buildForgottenChapel();
        buildFloatingForest();
        buildPearlRoute();
    }

    /* ---------------- High Spawn / Sky Citadel ---------------- */

    private void buildSkyCitadel() {
        V3Terrain.island(c, 0, SPAWN_Y - 2, 0, 58, 50, 20, 1101L,
                Material.GRASS, Material.DIRT, Material.STONE);
        V3Terrain.island(c, -30, SPAWN_Y - 3, 15, 32, 26, 14, 1102L,
                Material.GRASS, Material.DIRT, Material.STONE);
        V3Terrain.island(c, 31, SPAWN_Y - 2, -12, 30, 25, 14, 1103L,
                Material.GRASS, Material.DIRT, Material.STONE);

        // central courtyard and royal pavilion
        c.disc(0, SPAWN_Y, 0, 25, Material.SMOOTH_BRICK);
        c.ring(0, SPAWN_Y + 1, 0, 25, 2, Material.MOSSY_COBBLESTONE);
        c.disc(0, SPAWN_Y + 1, 0, 8, Material.QUARTZ_BLOCK);
        c.ring(0, SPAWN_Y + 2, 0, 9, 2, Material.GOLD_BLOCK);

        int[][] royalColumns = {{-13,-13},{13,-13},{-13,13},{13,13}};
        for (int[] p : royalColumns) {
            c.column(p[0], SPAWN_Y + 1, p[1], 13, Material.SMOOTH_BRICK);
            c.column(p[0] + (p[0] < 0 ? 1 : -1), SPAWN_Y + 2, p[1], 11, Material.QUARTZ_BLOCK);
        }
        c.ring(0, SPAWN_Y + 14, 0, 19, 3, Material.SMOOTH_BRICK);
        c.ring(0, SPAWN_Y + 15, 0, 16, 3, Material.WOOD);

        // four towers create a recognizable skyline
        V3Structures.watchTower(c, -43, SPAWN_Y, -35, 5, 18);
        V3Structures.watchTower(c, 43, SPAWN_Y, -35, 5, 21);
        V3Structures.watchTower(c, -43, SPAWN_Y, 35, 5, 20);
        V3Structures.watchTower(c, 43, SPAWN_Y, 35, 5, 18);

        // shop houses integrated into the citadel rather than separate boxes
        V3Structures.medievalHouse(c, -31, SPAWN_Y + 1, -7, 15, 11, true);   // PvP
        V3Structures.medievalHouse(c, 31, SPAWN_Y + 1, -7, 15, 11, false);   // Potion
        V3Structures.medievalHouse(c, -31, SPAWN_Y + 1, 14, 13, 11, false);  // Pearl
        V3Structures.medievalHouse(c, 31, SPAWN_Y + 1, 14, 13, 11, true);    // Netherstar

        // Crate hall as a small temple at the rear
        c.fill(-14, SPAWN_Y + 1, 31, 14, SPAWN_Y + 1, 45, Material.SMOOTH_BRICK);
        for (int x = -12; x <= 12; x += 6) {
            c.column(x, SPAWN_Y + 2, 32, 8, Material.QUARTZ_BLOCK);
            c.set(x, SPAWN_Y + 2, 39, Material.CHEST);
            c.set(x, SPAWN_Y + 3, 39, Material.GLOWSTONE);
        }
        c.fill(-14, SPAWN_Y + 10, 31, 14, SPAWN_Y + 10, 45, Material.WOOD);
        V3Structures.brokenArch(c, 0, SPAWN_Y + 2, 31, 10, 9);

        // Rank / BattlePass hall opposite crates
        c.fill(-17, SPAWN_Y + 1, -45, 17, SPAWN_Y + 1, -32, Material.SMOOTH_BRICK);
        V3Structures.brokenArch(c, 0, SPAWN_Y + 2, -32, 12, 10);
        for (int x = -12; x <= 12; x += 6) {
            c.column(x, SPAWN_Y + 2, -41, 7, Material.SMOOTH_BRICK);
            c.set(x, SPAWN_Y + 9, -41, Material.GLOWSTONE);
        }

        // vegetation and believable lived-in corners
        V3Structures.customTree(c, -48, SPAWN_Y, 7, 9);
        V3Structures.customTree(c, 47, SPAWN_Y, 8, 10);
        V3Structures.customTree(c, -17, SPAWN_Y, 42, 8);
        V3Structures.customTree(c, 18, SPAWN_Y, 43, 9);
        scatterSpawnProps();

        // four massive drop balconies - the PvP map is visible below
        dropBalcony(0, SPAWN_Y + 1, -58, true);
        dropBalcony(0, SPAWN_Y + 1, 58, true);
        dropBalcony(-66, SPAWN_Y + 1, 0, false);
        dropBalcony(66, SPAWN_Y + 1, 0, false);
    }

    private void scatterSpawnProps() {
        int[][] chests = {{-18,18},{19,20},{-20,-20},{21,-19}};
        for (int[] p : chests) {
            c.set(p[0], SPAWN_Y + 1, p[1], Material.CHEST);
            c.set(p[0] + 1, SPAWN_Y + 1, p[1], Material.WORKBENCH);
        }
        int[][] lamps = {{-22,0},{22,0},{0,-22},{0,22}};
        for (int[] p : lamps) {
            c.column(p[0], SPAWN_Y + 1, p[1], 4, Material.FENCE);
            c.set(p[0], SPAWN_Y + 5, p[1], Material.GLOWSTONE);
        }
    }

    private void dropBalcony(int cx, int y, int cz, boolean horizontal) {
        if (horizontal) c.fill(-11, y, cz - 7, 11, y, cz + 7, Material.SMOOTH_BRICK);
        else c.fill(cx - 7, y, -11, cx + 7, y, 11, Material.SMOOTH_BRICK);
        c.ring(cx, y + 1, cz, 7, 1, Material.GOLD_BLOCK);
        V3Structures.brokenArch(c, cx, y + 1, cz, 12, 9);
    }

    /* ---------------- Fallen Capital / Main PvP ---------------- */

    private void buildFallenCapital() {
        // overlapping landmasses make a broken, believable capital instead of a circle
        V3Terrain.island(c, 0, PVP_Y, 0, 112, 91, 35, 2101L, Material.GRASS, Material.DIRT, Material.STONE);
        V3Terrain.island(c, -54, PVP_Y + 1, 25, 67, 55, 27, 2102L, Material.GRASS, Material.DIRT, Material.STONE);
        V3Terrain.island(c, 58, PVP_Y - 1, -24, 71, 59, 30, 2103L, Material.GRASS, Material.DIRT, Material.STONE);
        V3Terrain.island(c, 12, PVP_Y + 3, 61, 58, 42, 23, 2104L, Material.GRASS, Material.DIRT, Material.STONE);

        // central market square
        c.disc(0, PVP_Y + 4, 0, 20, Material.GRAVEL);
        c.disc(0, PVP_Y + 5, 0, 6, Material.SMOOTH_BRICK);
        c.column(0, PVP_Y + 6, 0, 7, Material.SMOOTH_BRICK);
        c.set(0, PVP_Y + 13, 0, Material.GLOWSTONE);

        // dense but asymmetric city blocks
        V3Structures.ruinedHouse(c, -29, PVP_Y + 4, -18, 13, 11);
        V3Structures.medievalHouse(c, -12, PVP_Y + 4, -31, 11, 9, true);
        V3Structures.ruinedHouse(c, 16, PVP_Y + 4, -30, 15, 9);
        V3Structures.medievalHouse(c, 35, PVP_Y + 4, -14, 13, 11, false);
        V3Structures.ruinedHouse(c, 31, PVP_Y + 4, 18, 11, 11);
        V3Structures.medievalHouse(c, 12, PVP_Y + 4, 34, 15, 11, true);
        V3Structures.ruinedHouse(c, -18, PVP_Y + 4, 32, 13, 9);
        V3Structures.medievalHouse(c, -38, PVP_Y + 4, 12, 11, 9, false);

        // village extensions
        V3Structures.ruinedHouse(c, 55, PVP_Y + 3, 30, 13, 9);
        V3Structures.medievalHouse(c, 66, PVP_Y + 3, 6, 11, 9, true);
        V3Structures.ruinedHouse(c, -64, PVP_Y + 5, -5, 11, 9);
        V3Structures.medievalHouse(c, -61, PVP_Y + 5, 35, 13, 11, false);

        // market details
        V3Structures.marketStall(c, -10, PVP_Y + 5, 7, false);
        V3Structures.marketStall(c, 11, PVP_Y + 5, 8, true);
        V3Structures.marketStall(c, -9, PVP_Y + 5, -10, true);
        V3Structures.marketStall(c, 10, PVP_Y + 5, -10, false);

        // trees, rocks and broken city walls
        V3Structures.customTree(c, -52, PVP_Y + 4, -37, 9);
        V3Structures.customTree(c, 53, PVP_Y + 4, 42, 10);
        V3Structures.customTree(c, 8, PVP_Y + 5, 58, 8);
        V3Structures.customTree(c, -47, PVP_Y + 5, 55, 11);
        V3Terrain.rock(c, 69, PVP_Y + 4, -49, 7, 2121L);
        V3Terrain.rock(c, -70, PVP_Y + 4, -45, 6, 2122L);
        V3Terrain.rock(c, 80, PVP_Y + 3, 10, 5, 2123L);

        brokenCityWall(-77, PVP_Y + 4, -24, -77, 36);
        brokenCityWall(78, PVP_Y + 4, -35, 78, 20);
        brokenCityWall(-35, PVP_Y + 4, 70, 27, 70);

        // cliffs and waterfalls make the silhouette strong from spawn
        V3Terrain.cliff(c, -87, PVP_Y - 10, 38, 13, 27, 2141L);
        V3Terrain.cliff(c, 91, PVP_Y - 12, -15, 14, 30, 2142L);
        V3Terrain.waterfall(c, -91, PVP_Y + 3, 45, 38);
        V3Terrain.waterfall(c, 94, PVP_Y + 2, -23, 34);
    }

    private void brokenCityWall(int x1, int y, int z1, int x2, int z2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            if ((i / 7) % 3 == 1) continue;
            double t = steps == 0 ? 0 : i / (double) steps;
            int x = (int)Math.round(x1 + (x2 - x1) * t);
            int z = (int)Math.round(z1 + (z2 - z1) * t);
            int h = 3 + (i % 5);
            c.column(x, y, z, h, i % 4 == 0 ? Material.MOSSY_COBBLESTONE : Material.SMOOTH_BRICK);
        }
    }

    /* ---------------- Old Keep ---------------- */

    private void buildOldKeep() {
        int y = PVP_Y + 8;
        V3Terrain.island(c, -70, y - 2, -72, 42, 34, 23, 3101L, Material.GRASS, Material.DIRT, Material.STONE);
        c.fill(-96, y, -94, -45, y, -51, Material.SMOOTH_BRICK);
        c.fill(-91, y + 1, -89, -50, y + 1, -56, Material.GRAVEL);
        V3Structures.watchTower(c, -91, y + 1, -88, 6, 18);
        V3Structures.watchTower(c, -51, y + 1, -57, 5, 14);
        V3Structures.watchTower(c, -51, y + 1, -88, 5, 11);
        V3Structures.brokenArch(c, -70, y + 1, -51, 13, 11);
        V3Structures.ruinedHouse(c, -70, y + 1, -72, 17, 13);

        // dungeon / armory pocket visible through broken floor
        c.fill(-82, y - 6, -80, -59, y - 6, -65, Material.SMOOTH_BRICK);
        c.hollowBox(-82, y - 6, -80, -59, y, -65, Material.COBBLESTONE);
        c.set(-79, y - 5, -76, Material.CHEST);
        c.set(-62, y - 5, -69, Material.IRON_BLOCK);
        c.set(-65, y - 5, -69, Material.ANVIL);

        V3Structures.stoneBridge(c, -55, PVP_Y + 4, -48, -61, y, -53, 5);
    }

    /* ---------------- The Crown / King Zone ---------------- */

    private void buildCrown() {
        int cx = -143, cz = -92, y = PVP_Y + 14;
        V3Terrain.island(c, cx, y - 2, cz, 46, 37, 31, 4101L, Material.GRASS, Material.DIRT, Material.STONE);
        V3Terrain.cliff(c, cx - 26, y - 15, cz + 8, 12, 27, 4102L);

        c.fill(cx - 26, y, cz - 22, cx + 25, y, cz + 23, Material.SMOOTH_BRICK);
        c.fill(cx - 21, y + 1, cz - 17, cx + 20, y + 1, cz + 18, Material.GRAVEL);
        V3Structures.watchTower(c, cx - 22, y + 1, cz - 19, 6, 20);
        V3Structures.watchTower(c, cx + 21, y + 1, cz - 18, 6, 18);
        V3Structures.brokenArch(c, cx, y + 1, cz + 23, 14, 12);

        // ruined throne hall and KOTH altar
        c.fill(cx - 14, y + 1, cz - 12, cx + 14, y + 1, cz + 9, Material.SMOOTH_BRICK);
        for (int x = cx - 12; x <= cx + 12; x += 6) {
            c.column(x, y + 2, cz - 11, 10, Material.SMOOTH_BRICK);
        }
        c.disc(cx, y + 2, cz, 8, Material.SMOOTH_BRICK);
        c.ring(cx, y + 3, cz, 9, 2, Material.GOLD_BLOCK);
        c.disc(cx, y + 3, cz, 4, Material.OBSIDIAN);
        c.set(cx, y + 4, cz, Material.BEACON);

        // crypt below throne hall
        c.hollowBox(cx - 13, y - 7, cz - 10, cx + 13, y - 1, cz + 10, Material.SMOOTH_BRICK);
        c.fill(cx - 9, y - 6, cz - 6, cx + 9, y - 6, cz + 6, Material.COBBLESTONE);
        c.set(cx, y - 5, cz, Material.CHEST);
        c.set(cx - 5, y - 5, cz, Material.GOLD_BLOCK);
        c.set(cx + 5, y - 5, cz, Material.GOLD_BLOCK);

        // multiple approach routes
        V3Structures.stoneBridge(c, -104, PVP_Y + 5, -62, cx + 30, y, cz + 18, 5);
        c.disc(cx + 40, y + 5, cz - 25, 6, Material.STONE); // pearl side-route
        c.disc(cx + 47, y + 9, cz - 35, 4, Material.STONE);
    }

    /* ---------------- End Rift ---------------- */

    private void buildEndRift() {
        int cx = 151, cz = -101, y = PVP_Y + 13;
        V3Terrain.island(c, cx, y - 2, cz, 43, 35, 30, 5101L, Material.ENDER_STONE, Material.OBSIDIAN, Material.STONE);
        V3Terrain.island(c, cx + 38, y + 9, cz - 12, 15, 11, 14, 5102L, Material.ENDER_STONE, Material.OBSIDIAN, Material.STONE);
        V3Terrain.island(c, cx - 34, y + 5, cz + 26, 13, 10, 13, 5103L, Material.ENDER_STONE, Material.OBSIDIAN, Material.STONE);

        // shattered sanctuary
        c.disc(cx, y + 1, cz, 18, Material.ENDER_STONE);
        c.ring(cx, y + 2, cz, 19, 3, Material.OBSIDIAN);
        int[][] pillars = {{-14,-8},{14,-8},{-10,11},{11,12}};
        for (int[] p : pillars) c.column(cx + p[0], y + 2, cz + p[1], 9 + Math.abs(p[0]) % 8, Material.OBSIDIAN);
        V3Structures.brokenArch(c, cx, y + 2, cz - 18, 12, 11);
        c.set(cx, y + 3, cz, Material.ENDER_CHEST);
        c.set(cx - 4, y + 3, cz + 3, Material.CHEST);

        // void chamber under island
        c.disc(cx + 7, y - 15, cz + 5, 8, Material.OBSIDIAN);
        c.ring(cx + 7, y - 14, cz + 5, 8, 2, Material.ENDER_STONE);
        c.set(cx + 7, y - 13, cz + 5, Material.CHEST);

        V3Structures.stoneBridge(c, 103, PVP_Y + 4, -60, cx - 32, y, cz + 21, 4);
    }

    /* ---------------- Ironforge ---------------- */

    private void buildIronforge() {
        int cx = 145, cz = 86, y = PVP_Y + 8;
        V3Terrain.island(c, cx, y - 2, cz, 42, 34, 27, 6101L, Material.GRASS, Material.DIRT, Material.STONE);
        V3Terrain.cliff(c, cx + 21, y - 10, cz - 15, 10, 22, 6102L);
        V3Structures.medievalHouse(c, cx, y + 1, cz, 19, 15, true);
        V3Structures.medievalHouse(c, cx - 23, y + 1, cz + 12, 11, 9, false);
        c.fill(cx + 16, y + 1, cz - 10, cx + 28, y + 1, cz + 9, Material.COBBLESTONE);
        c.hollowBox(cx + 17, y + 2, cz - 9, cx + 27, y + 8, cz + 8, Material.SMOOTH_BRICK);
        c.set(cx + 22, y + 2, cz, Material.LAVA);
        for (int x = cx + 18; x <= cx + 26; x += 2) c.set(x, y + 2, cz + 5, Material.ANVIL);
        c.fill(cx - 8, y + 1, cz + 17, cx + 8, y + 1, cz + 23, Material.WOOD);
        for (int x = cx - 7; x <= cx + 7; x += 3) c.set(x, y + 2, cz + 20, Material.LOG);
        V3Structures.stoneBridge(c, 95, PVP_Y + 4, 48, cx - 35, y, cz - 20, 5);
    }

    /* ---------------- Sky Bazaar ---------------- */

    private void buildSkyBazaar() {
        int cx = 62, cz = 154, y = PVP_Y + 12;
        V3Terrain.island(c, cx, y - 2, cz, 52, 40, 28, 7101L, Material.GRASS, Material.DIRT, Material.STONE);
        c.disc(cx, y + 1, cz, 17, Material.GRAVEL);
        c.disc(cx, y + 2, cz, 4, Material.SMOOTH_BRICK);
        c.column(cx, y + 3, cz, 5, Material.SMOOTH_BRICK);
        c.set(cx, y + 8, cz, Material.GLOWSTONE);

        V3Structures.medievalHouse(c, cx - 30, y + 1, cz - 12, 13, 11, true);
        V3Structures.medievalHouse(c, cx + 30, y + 1, cz - 10, 15, 11, false);
        V3Structures.medievalHouse(c, cx - 27, y + 1, cz + 18, 11, 9, false);
        V3Structures.medievalHouse(c, cx + 27, y + 1, cz + 19, 13, 9, true);
        V3Structures.marketStall(c, cx - 11, y + 2, cz - 9, true);
        V3Structures.marketStall(c, cx + 11, y + 2, cz - 8, false);
        V3Structures.marketStall(c, cx - 10, y + 2, cz + 10, false);
        V3Structures.marketStall(c, cx + 11, y + 2, cz + 11, true);

        // airship dock silhouette
        c.fill(cx + 38, y + 2, cz + 5, cx + 50, y + 2, cz + 10, Material.WOOD);
        c.column(cx + 49, y + 3, cz + 7, 9, Material.LOG);
        c.fill(cx + 45, y + 10, cz + 7, cx + 53, y + 10, cz + 7, Material.WOOL);

        V3Structures.stoneBridge(c, 43, PVP_Y + 5, 89, cx - 24, y, cz - 30, 5);
    }

    /* ---------------- Arcane Sanctum ---------------- */

    private void buildArcaneSanctum() {
        int cx = -69, cz = 159, y = PVP_Y + 15;
        V3Terrain.island(c, cx, y - 2, cz, 39, 33, 25, 8101L, Material.GRASS, Material.DIRT, Material.STONE);
        c.fill(cx - 20, y + 1, cz - 14, cx + 20, y + 1, cz + 14, Material.SMOOTH_BRICK);
        c.hollowBox(cx - 18, y + 2, cz - 12, cx + 18, y + 11, cz + 12, Material.SMOOTH_BRICK);
        V3Structures.brokenArch(c, cx, y + 2, cz - 12, 11, 10);
        for (int x = cx - 13; x <= cx + 13; x += 4) {
            c.column(x, y + 2, cz + 8, 5, Material.BOOKSHELF);
            c.column(x, y + 2, cz - 8, 5, Material.BOOKSHELF);
        }
        c.disc(cx, y + 2, cz, 7, Material.OBSIDIAN);
        c.set(cx, y + 3, cz, Material.ENCHANTMENT_TABLE);
        c.column(cx - 15, y + 2, cz, 13, Material.SMOOTH_BRICK);
        c.column(cx + 15, y + 2, cz, 16, Material.SMOOTH_BRICK);

        // hidden archive below
        c.hollowBox(cx - 12, y - 8, cz - 9, cx + 12, y - 2, cz + 9, Material.COBBLESTONE);
        c.fill(cx - 10, y - 7, cz - 7, cx + 10, y - 7, cz + 7, Material.WOOD);
        for (int x = cx - 8; x <= cx + 8; x += 4) c.set(x, y - 6, cz + 6, Material.BOOKSHELF);
        c.set(cx, y - 6, cz - 4, Material.CHEST);

        V3Structures.stoneBridge(c, -32, PVP_Y + 6, 91, cx + 24, y, cz - 28, 4);
    }

    /* ---------------- Alchemist Garden ---------------- */

    private void buildAlchemistGarden() {
        int cx = 143, cz = 166, y = PVP_Y + 18;
        V3Terrain.island(c, cx, y - 2, cz, 34, 29, 23, 9101L, Material.GRASS, Material.DIRT, Material.STONE);
        V3Structures.medievalHouse(c, cx - 5, y + 1, cz, 17, 13, true);
        c.fill(cx + 13, y + 1, cz - 10, cx + 28, y + 1, cz + 9, Material.GLASS);
        c.hollowBox(cx + 13, y + 2, cz - 10, cx + 28, y + 8, cz + 9, Material.GLASS);
        c.fill(cx - 28, y + 1, cz - 14, cx - 14, y + 1, cz + 14, Material.GRASS);
        for (int x = cx - 26; x <= cx - 16; x += 3) {
            c.set(x, y + 2, cz - 8, Material.RED_ROSE);
            c.set(x, y + 2, cz, Material.YELLOW_FLOWER);
            c.set(x, y + 2, cz + 8, Material.LONG_GRASS);
        }
        c.disc(cx - 20, y + 1, cz + 20, 7, Material.WATER);
        V3Structures.customTree(c, cx + 22, y + 1, cz + 18, 8);
    }

    /* ---------------- Treasury ---------------- */

    private void buildTreasury() {
        int cx = -156, cz = 59, y = PVP_Y + 11;
        V3Terrain.island(c, cx, y - 2, cz, 38, 31, 25, 10101L, Material.GRASS, Material.DIRT, Material.STONE);
        c.fill(cx - 22, y + 1, cz - 15, cx + 22, y + 1, cz + 15, Material.SMOOTH_BRICK);
        c.hollowBox(cx - 20, y + 2, cz - 13, cx + 20, y + 12, cz + 13, Material.SMOOTH_BRICK);
        V3Structures.brokenArch(c, cx, y + 2, cz - 13, 12, 11);
        for (int x = cx - 14; x <= cx + 14; x += 7) c.column(x, y + 2, cz + 8, 9, Material.QUARTZ_BLOCK);
        c.hollowBox(cx - 10, y + 2, cz + 1, cx + 10, y + 8, cz + 11, Material.IRON_BLOCK);
        c.fill(cx - 7, y + 3, cz + 4, cx + 7, y + 3, cz + 8, Material.GOLD_BLOCK);
        c.set(cx, y + 4, cz + 6, Material.CHEST);
        V3Structures.medievalHouse(c, cx - 26, y + 1, cz + 19, 11, 9, false);
    }

    /* ---------------- Watchtower / Chapel / Forest ---------------- */

    private void buildWatchtower() {
        int cx = 205, cz = 15, y = PVP_Y + 25;
        V3Terrain.island(c, cx, y - 2, cz, 24, 20, 22, 11101L, Material.GRASS, Material.DIRT, Material.STONE);
        V3Structures.watchTower(c, cx, y + 1, cz, 7, 31);
        c.set(cx, y + 32, cz, Material.CHEST);
        V3Structures.customTree(c, cx - 13, y + 1, cz + 10, 7);
    }

    private void buildForgottenChapel() {
        int cx = -218, cz = -7, y = PVP_Y + 24;
        V3Terrain.island(c, cx, y - 2, cz, 24, 19, 21, 12101L, Material.GRASS, Material.DIRT, Material.STONE);
        c.fill(cx - 11, y + 1, cz - 8, cx + 11, y + 1, cz + 10, Material.SMOOTH_BRICK);
        c.hollowBox(cx - 10, y + 2, cz - 7, cx + 10, y + 10, cz + 9, Material.SMOOTH_BRICK);
        V3Structures.brokenArch(c, cx, y + 2, cz - 7, 8, 9);
        c.column(cx, y + 11, cz + 7, 10, Material.SMOOTH_BRICK);
        c.set(cx, y + 21, cz + 7, Material.GLOWSTONE);
        // crypt
        c.hollowBox(cx - 9, y - 7, cz - 6, cx + 9, y - 1, cz + 7, Material.MOSSY_COBBLESTONE);
        c.set(cx, y - 6, cz + 3, Material.CHEST);
    }

    private void buildFloatingForest() {
        int cx = 14, cz = -181, y = PVP_Y + 18;
        V3Terrain.island(c, cx, y - 2, cz, 46, 34, 27, 13101L, Material.GRASS, Material.DIRT, Material.STONE);
        V3Structures.customTree(c, cx - 22, y + 1, cz - 8, 13);
        V3Structures.customTree(c, cx + 19, y + 1, cz - 9, 11);
        V3Structures.customTree(c, cx - 10, y + 1, cz + 14, 10);
        V3Structures.customTree(c, cx + 13, y + 1, cz + 15, 14);
        V3Structures.customTree(c, cx, y + 1, cz, 16);
        V3Terrain.rock(c, cx - 30, y + 1, cz + 14, 6, 13111L);
        V3Terrain.rock(c, cx + 29, y + 1, cz + 5, 7, 13112L);
        V3Terrain.waterfall(c, cx + 32, y + 1, cz - 15, 32);
        // small treehouse platform
        c.fill(cx - 5, y + 12, cz - 3, cx + 5, y + 12, cz + 5, Material.WOOD);
        c.column(cx - 5, y + 13, cz - 3, 4, Material.FENCE);
        c.column(cx + 5, y + 13, cz - 3, 4, Material.FENCE);
        c.set(cx, y + 13, cz + 2, Material.CHEST);
    }

    private void buildPearlRoute() {
        // These are sculpted stepping landmarks rather than obvious square pads.
        pearlRock(116, PVP_Y + 20, -134, 7, 14001L);
        pearlRock(-122, PVP_Y + 18, -143, 6, 14002L);
        pearlRock(185, PVP_Y + 28, 91, 6, 14003L);
        pearlRock(-187, PVP_Y + 24, 112, 7, 14004L);
        pearlRock(84, PVP_Y + 31, 205, 5, 14005L);
        pearlRock(-91, PVP_Y + 29, 205, 5, 14006L);
    }

    private void pearlRock(int x, int y, int z, int radius, long seed) {
        V3Terrain.rock(c, x, y, z, radius, seed);
        c.set(x, y + radius + 1, z, Material.GLOWSTONE);
    }

    private void finish() {
        Location spawn = new Location(world, 0.5D, SPAWN_Y + 2.0D, 0.5D, 180.0F, 12.0F);
        world.setSpawnLocation(0, SPAWN_Y + 2, 0);
        world.setTime(5500L);
        world.setStorm(false);
        world.setThundering(false);
        world.setGameRuleValue("doMobSpawning", "false");
        spawnService.setSpawn(spawn);
        initiator.teleport(spawn);
        Bukkit.broadcastMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "SKYKINGS ARENA V3 "
                + ChatColor.GREEN + "wurde fertig gebaut.");
        initiator.sendMessage(ChatColor.YELLOW + "V3: Sky Citadel oben, Fallen Capital und Charakter-Inseln darunter.");
    }
}
