package net.skykings.combat.map.builder;

import net.skykings.combat.spawn.SpawnService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * SkyKings Map V2: klassisches OP-SkyPvP mit High-Spawn und Drop-Down.
 * Ausschliesslich Material-Namen aus Spigot 1.8.8 verwenden.
 */
public final class SkyKingsMapBuilderV2 {

    private static final int BLOCKS_PER_TICK = 5000;
    private static final int SPAWN_Y = 208;
    private static final int PVP_Y = 74;

    private static final class Placement {
        final int x, y, z;
        final Material material;

        Placement(int x, int y, int z, Material material) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.material = material;
        }
    }

    private final JavaPlugin plugin;
    private final World world;
    private final SpawnService spawnService;
    private final Player initiator;
    private final List<Placement> blocks = new ArrayList<Placement>();
    private final Random random = new Random(20260831L);

    public SkyKingsMapBuilderV2(JavaPlugin plugin, World world, SpawnService spawnService, Player initiator) {
        this.plugin = plugin;
        this.world = world;
        this.spawnService = spawnService;
        this.initiator = initiator;
    }

    public void start() {
        plan();
        final int total = blocks.size();
        initiator.sendMessage(ChatColor.GOLD + "SkyKings Map V2 wird gebaut: " + ChatColor.WHITE + total + " Bloecke");

        new BukkitRunnable() {
            private int index;

            @Override
            public void run() {
                int end = Math.min(total, index + BLOCKS_PER_TICK);
                while (index < end) {
                    Placement p = blocks.get(index++);
                    world.getBlockAt(p.x, p.y, p.z).setType(p.material, false);
                }
                if (index >= total) {
                    cancel();
                    finish();
                    return;
                }
                if (index % (BLOCKS_PER_TICK * 20) < BLOCKS_PER_TICK) {
                    initiator.sendMessage(ChatColor.GRAY + "Map V2: " + ChatColor.YELLOW
                            + (int) ((index * 100L) / Math.max(1, total)) + "%");
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void plan() {
        buildHighSpawn();
        buildMainPvp();
        buildRuins();
        buildKingZone();
        buildEndIsland();
        buildBlacksmith();
        buildGoldIsland();
        buildMerchantIsland();
        buildEnchantIsland();
        buildSatellites();
    }

    private void buildHighSpawn() {
        island(0, SPAWN_Y - 5, 0, 46, 40, 15, Material.QUARTZ_BLOCK, Material.STONE, 11);
        disc(0, SPAWN_Y, 0, 28, Material.QUARTZ_BLOCK);
        ring(0, SPAWN_Y + 1, 0, 29, Material.GLOWSTONE);
        disc(0, SPAWN_Y + 1, 0, 7, Material.GOLD_BLOCK);

        int[][] pillars = {{19,19},{19,-19},{-19,19},{-19,-19}};
        for (int[] p : pillars) {
            column(p[0], SPAWN_Y + 1, p[1], 11, Material.QUARTZ_BLOCK);
            set(p[0], SPAWN_Y + 12, p[1], Material.GLOWSTONE);
        }
        ring(0, SPAWN_Y + 12, 0, 22, Material.QUARTZ_BLOCK);

        // Vier breite Sprungausgaenge. Danach ist bewusst Luft bis zur PvP-Map.
        balcony(0, -43, true);
        balcony(0, 43, true);
        balcony(-47, 0, false);
        balcony(47, 0, false);

        platform(-30, SPAWN_Y + 3, -30, 5, Material.SMOOTH_BRICK);
        platform(30, SPAWN_Y + 3, -30, 5, Material.SMOOTH_BRICK);
        platform(-30, SPAWN_Y + 3, 30, 5, Material.SMOOTH_BRICK);
        platform(30, SPAWN_Y + 3, 30, 5, Material.SMOOTH_BRICK);
    }

    private void balcony(int cx, int cz, boolean xWide) {
        if (xWide) {
            fill(-10, SPAWN_Y + 1, cz - 7, 10, SPAWN_Y + 1, cz + 7, Material.SMOOTH_BRICK);
        } else {
            fill(cx - 7, SPAWN_Y + 1, -10, cx + 7, SPAWN_Y + 1, 10, Material.SMOOTH_BRICK);
        }
        set(cx, SPAWN_Y + 2, cz, Material.GLOWSTONE);
    }

    private void buildMainPvp() {
        // Mehrere ueberlappende Inselkoerper erzeugen eine grosse, unregelmaessige Hauptflaeche.
        island(0, PVP_Y, 0, 110, 97, 34, Material.GRASS, Material.DIRT, 101);
        island(-38, PVP_Y - 1, 22, 70, 58, 26, Material.GRASS, Material.DIRT, 102);
        island(48, PVP_Y, -27, 66, 55, 25, Material.GRASS, Material.DIRT, 103);

        landing(0, -58);
        landing(0, 58);
        landing(-65, 0);
        landing(65, 0);

        for (int i = 0; i < 30; i++) {
            int x = random.nextInt(159) - 79;
            int z = random.nextInt(145) - 72;
            int r = 2 + random.nextInt(5);
            disc(x, PVP_Y + 3, z, r, i % 3 == 0 ? Material.COBBLESTONE : Material.STONE);
        }
    }

    private void landing(int x, int z) {
        disc(x, PVP_Y + 3, z, 11, Material.SMOOTH_BRICK);
        ring(x, PVP_Y + 4, z, 12, Material.GLOWSTONE);
        disc(x, PVP_Y + 4, z, 4, Material.GRASS);
    }

    private void buildRuins() {
        int y = PVP_Y + 3;
        fill(-27, y, -24, 27, y, 24, Material.SMOOTH_BRICK);
        fill(-21, y + 1, -18, 21, y + 1, 18, Material.GRASS);

        wall(-30, y + 1, -28, 30, y + 8, -28, Material.SMOOTH_BRICK);
        wall(-30, y + 1, 28, 8, y + 7, 28, Material.COBBLESTONE);
        wall(17, y + 1, 28, 30, y + 5, 28, Material.COBBLESTONE);
        wall(-30, y + 1, -28, -30, y + 7, 9, Material.SMOOTH_BRICK);
        wall(30, y + 1, -28, 30, y + 8, 28, Material.SMOOTH_BRICK);

        tower(-23, y + 1, -21, 7, 18, Material.SMOOTH_BRICK);
        tower(23, y + 1, 19, 6, 14, Material.COBBLESTONE);

        column(0, y + 1, 0, 16, Material.OBSIDIAN);
        column(1, y + 1, 0, 12, Material.OBSIDIAN);
        set(0, y + 17, 0, Material.GLOWSTONE);
    }

    private void buildKingZone() {
        int x = -82, z = -58, y = PVP_Y + 10;
        disc(x, y, z, 20, Material.SMOOTH_BRICK);
        ring(x, y + 1, z, 21, Material.GOLD_BLOCK);
        disc(x, y + 1, z, 7, Material.GOLD_BLOCK);
        column(x, y + 2, z, 9, Material.QUARTZ_BLOCK);
        set(x, y + 11, z, Material.GLOWSTONE);
        bridge(-72, PVP_Y + 4, -45, -70, y, -50, 5, Material.SMOOTH_BRICK);
        bridge(-60, PVP_Y + 4, -62, -68, y, -58, 5, Material.SMOOTH_BRICK);
    }

    private void buildEndIsland() {
        int x = 140, z = -66, y = PVP_Y + 8;
        island(x, y, z, 35, 30, 25, Material.ENDER_STONE, Material.OBSIDIAN, 201);
        ring(x, y + 2, z, 16, Material.OBSIDIAN);
        tower(x + 14, y + 2, z - 8, 5, 17, Material.OBSIDIAN);
        tower(x - 14, y + 2, z + 10, 4, 12, Material.ENDER_STONE);
        bridge(97, PVP_Y + 4, -45, 111, y + 1, -53, 5, Material.OBSIDIAN);
    }

    private void buildBlacksmith() {
        int x = 128, z = 77, y = PVP_Y + 5;
        island(x, y, z, 32, 28, 23, Material.COBBLESTONE, Material.STONE, 301);
        fill(x - 17, y + 2, z - 13, x + 17, y + 2, z + 13, Material.SMOOTH_BRICK);
        wall(x - 17, y + 3, z - 13, x + 17, y + 9, z - 13, Material.COBBLESTONE);
        wall(x - 17, y + 3, z + 13, x + 17, y + 7, z + 13, Material.COBBLESTONE);
        for (int xx = x - 8; xx <= x + 8; xx += 4) set(xx, y + 3, z, Material.ANVIL);
        column(x + 12, y + 3, z + 8, 12, Material.BRICK);
        set(x + 12, y + 15, z + 8, Material.NETHERRACK);
        set(x + 12, y + 16, z + 8, Material.FIRE);
        bridge(94, PVP_Y + 4, 50, 104, y + 1, 64, 5, Material.COBBLESTONE);
    }

    private void buildGoldIsland() {
        int x = -136, z = 62, y = PVP_Y + 6;
        island(x, y, z, 30, 26, 22, Material.SANDSTONE, Material.SANDSTONE, 401);
        for (int level = 0; level < 7; level++) {
            int r = 15 - level * 2;
            if (r < 2) break;
            fill(x - r, y + 2 + level, z - r, x + r, y + 2 + level, z + r, Material.SANDSTONE);
        }
        ring(x, y + 3, z, 10, Material.GOLD_BLOCK);
        bridge(-99, PVP_Y + 4, 41, -112, y + 1, 51, 4, Material.SANDSTONE);
    }

    private void buildMerchantIsland() {
        int x = 38, z = 138, y = PVP_Y + 9;
        island(x, y, z, 38, 32, 25, Material.GRASS, Material.DIRT, 501);
        fill(x - 25, y + 2, z - 5, x + 25, y + 2, z + 5, Material.WOOD);
        for (int xx = x - 20; xx <= x + 20; xx += 10) {
            stall(xx, y + 3, z - 14);
            stall(xx, y + 3, z + 14);
        }
        bridge(30, PVP_Y + 4, 95, 34, y + 1, 106, 5, Material.WOOD);
    }

    private void buildEnchantIsland() {
        int x = -68, z = 137, y = PVP_Y + 11;
        island(x, y, z, 31, 28, 24, Material.GRASS, Material.DIRT, 601);
        disc(x, y + 2, z, 13, Material.SMOOTH_BRICK);
        column(x, y + 3, z, 6, Material.OBSIDIAN);
        set(x, y + 9, z, Material.ENCHANTMENT_TABLE);
        for (int i = -8; i <= 8; i += 4) {
            set(x + i, y + 3, z + 8, Material.BOOKSHELF);
            set(x + i, y + 3, z - 8, Material.BOOKSHELF);
        }
        bridge(-49, PVP_Y + 4, 95, -59, y + 1, 108, 4, Material.SMOOTH_BRICK);
    }

    private void buildSatellites() {
        island(191, PVP_Y + 18, 26, 18, 15, 18, Material.GRASS, Material.DIRT, 701);
        tower(191, PVP_Y + 20, 26, 5, 18, Material.SMOOTH_BRICK);
        island(-186, PVP_Y + 12, -9, 19, 16, 19, Material.STONE, Material.COBBLESTONE, 702);
        platform(-186, PVP_Y + 15, -9, 7, Material.MOSSY_COBBLESTONE);
        island(90, PVP_Y + 25, -162, 17, 14, 18, Material.GRASS, Material.DIRT, 703);
        island(-72, PVP_Y + 22, -167, 16, 13, 17, Material.GRASS, Material.DIRT, 704);
        platform(110, PVP_Y + 18, -123, 6, Material.OBSIDIAN);
        platform(-123, PVP_Y + 17, -122, 6, Material.SMOOTH_BRICK);
        platform(15, PVP_Y + 28, -180, 5, Material.SMOOTH_BRICK);
    }

    private void finish() {
        Location spawn = new Location(world, 0.5D, SPAWN_Y + 2.0D, 0.5D, 180.0F, 20.0F);
        world.setSpawnLocation(0, SPAWN_Y + 2, 0);
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
        world.setGameRuleValue("doMobSpawning", "false");
        spawnService.setSpawn(spawn);
        Bukkit.broadcastMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "SKYKINGS MAP V2 "
                + ChatColor.GREEN + "• High-Spawn Arena fertig gebaut.");
        initiator.teleport(spawn);
    }

    private void island(int cx, int topY, int cz, int rx, int rz, int depth,
                        Material top, Material middle, int seed) {
        Random r = new Random(seed);
        for (int dx = -rx - 3; dx <= rx + 3; dx++) {
            for (int dz = -rz - 3; dz <= rz + 3; dz++) {
                double nx = dx / (double) rx;
                double nz = dz / (double) rz;
                double dist = Math.sqrt(nx * nx + nz * nz);
                double noise = Math.sin((dx + seed) * 0.23D) * 0.05D
                        + Math.cos((dz - seed) * 0.29D) * 0.05D
                        + (r.nextDouble() - 0.5D) * 0.045D;
                if (dist > 1.0D + noise) continue;
                int surface = topY + (int) Math.round(Math.sin(dx * 0.10D) * 1.6D + Math.cos(dz * 0.12D) * 1.4D);
                double body = Math.max(0.0D, 1.0D - Math.pow(Math.min(1.0D, dist), 1.45D));
                int thickness = Math.max(4, (int) Math.round(4 + depth * body + r.nextDouble() * 3));
                for (int y = surface; y >= surface - thickness; y--) {
                    int below = surface - y;
                    Material m = below == 0 ? top : (below <= 3 ? middle : Material.STONE);
                    if (below > 4 && r.nextInt(9) == 0) m = Material.COBBLESTONE;
                    set(cx + dx, y, cz + dz, m);
                }
            }
        }
    }

    private void tower(int cx, int y, int cz, int radius, int height, Material material) {
        for (int dy = 0; dy < height; dy++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d >= radius - 1.2D && d <= radius + 0.4D) set(cx + x, y + dy, cz + z, material);
                }
            }
        }
        disc(cx, y + height - 1, cz, radius, material);
        ring(cx, y + height, cz, radius + 1, material);
    }

    private void stall(int cx, int y, int cz) {
        fill(cx - 3, y, cz - 2, cx + 3, y, cz + 2, Material.WOOD);
        column(cx - 3, y + 1, cz - 2, 4, Material.FENCE);
        column(cx + 3, y + 1, cz - 2, 4, Material.FENCE);
        column(cx - 3, y + 1, cz + 2, 4, Material.FENCE);
        column(cx + 3, y + 1, cz + 2, 4, Material.FENCE);
        fill(cx - 4, y + 5, cz - 3, cx + 4, y + 5, cz + 3, Material.WOOD);
    }

    private void bridge(int x1, int y1, int z1, int x2, int y2, int z2, int width, Material material) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0D : i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            for (int dx = -width / 2; dx <= width / 2; dx++)
                for (int dz = -width / 2; dz <= width / 2; dz++) set(x + dx, y, z + dz, material);
        }
    }

    private void wall(int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        fill(x1, y1, z1, x2, y2, z2, material);
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, material);
    }

    private void disc(int cx, int y, int cz, int radius, Material material) {
        int r2 = radius * radius;
        for (int x = -radius; x <= radius; x++)
            for (int z = -radius; z <= radius; z++)
                if (x * x + z * z <= r2) set(cx + x, y, cz + z, material);
    }

    private void platform(int cx, int y, int cz, int radius, Material material) {
        disc(cx, y, cz, radius, material);
        if (radius > 2) disc(cx, y - 1, cz, radius - 2, Material.STONE);
    }

    private void ring(int cx, int y, int cz, int radius, Material material) {
        int outer = radius * radius;
        int inner = (radius - 2) * (radius - 2);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int d = x * x + z * z;
                if (d <= outer && d >= inner) set(cx + x, y, cz + z, material);
            }
        }
    }

    private void column(int x, int y, int z, int height, Material material) {
        for (int i = 0; i < height; i++) set(x, y + i, z, material);
    }

    private void set(int x, int y, int z, Material material) {
        if (y < 1 || y > 254) return;
        blocks.add(new Placement(x, y, z, material));
    }
}
