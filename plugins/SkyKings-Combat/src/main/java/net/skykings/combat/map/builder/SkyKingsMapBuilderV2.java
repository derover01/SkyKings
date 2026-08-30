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
 * SkyKings Map V2.
 *
 * Klassisches OP-SkyPvP-Prinzip:
 * - sicherer Spawn weit ueber der Kampfmap
 * - Spieler springen direkt nach unten ins PvP
 * - grosse zentrale Hauptinsel statt Hub-/Bruecken-Netz
 * - thematische Nebeninseln als Hotspots und Pearl-Ziele
 */
public final class SkyKingsMapBuilderV2 {

    private static final int BLOCKS_PER_TICK = 5000;
    private static final int SPAWN_Y = 208;
    private static final int PVP_Y = 74;

    private static final class Placement {
        final int x;
        final int y;
        final int z;
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
                    Placement placement = blocks.get(index++);
                    world.getBlockAt(placement.x, placement.y, placement.z).setType(placement.material, false);
                }

                if (index >= total) {
                    cancel();
                    finish();
                    return;
                }

                if (index % (BLOCKS_PER_TICK * 20) < BLOCKS_PER_TICK) {
                    int percent = (int) ((index * 100L) / Math.max(1, total));
                    initiator.sendMessage(ChatColor.GRAY + "Map V2: " + ChatColor.YELLOW + percent + "%");
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void plan() {
        buildHighSpawn();
        buildMainPvpIsland();
        buildCentralRuins();
        buildKingZone();
        buildEndIsland();
        buildBlacksmithIsland();
        buildGoldIsland();
        buildMerchantIsland();
        buildEnchantIsland();
        buildOuterSatellites();
        buildPearlPads();
    }

    /** Spawn schwebt weit ueber der PvP-Map. Von allen Seiten kann direkt heruntergesprungen werden. */
    private void buildHighSpawn() {
        floatingIsland(0, SPAWN_Y - 5, 0, 39, 35, 13,
                Material.QUARTZ_BLOCK, Material.STONE, Material.COBBLESTONE, 11);

        // zentrale Spawnplattform
        disc(0, SPAWN_Y, 0, 24, Material.QUARTZ_BLOCK);
        ring(0, SPAWN_Y + 1, 0, 25, Material.GLOWSTONE);
        disc(0, SPAWN_Y + 1, 0, 8, Material.GOLD_BLOCK);

        // grosser offener Pavillon statt geschlossener Spawnbox
        int[][] pillars = {{18,18},{18,-18},{-18,18},{-18,-18}};
        for (int[] p : pillars) {
            column(p[0], SPAWN_Y + 1, p[1], 10, Material.QUARTZ_BLOCK);
            set(p[0], SPAWN_Y + 11, p[1], Material.GLOWSTONE);
        }
        ring(0, SPAWN_Y + 11, 0, 20, Material.QUARTZ_BLOCK);

        // Vier breite Absprung-Balkone. Danach kommt bewusst nur Luft.
        jumpBalcony(0, SPAWN_Y + 1, -38, true);
        jumpBalcony(0, SPAWN_Y + 1, 38, true);
        jumpBalcony(-42, SPAWN_Y + 1, 0, false);
        jumpBalcony(42, SPAWN_Y + 1, 0, false);

        // Kleine Aussichtspunkte, damit man die Map unter sich sieht.
        platform(-27, SPAWN_Y + 3, -27, 5, Material.STONE_BRICK);
        platform(27, SPAWN_Y + 3, -27, 5, Material.STONE_BRICK);
        platform(-27, SPAWN_Y + 3, 27, 5, Material.STONE_BRICK);
        platform(27, SPAWN_Y + 3, 27, 5, Material.STONE_BRICK);
    }

    private void jumpBalcony(int cx, int y, int cz, boolean eastWest) {
        if (eastWest) {
            for (int x = -9; x <= 9; x++) {
                for (int z = cz - 6; z <= cz + 6; z++) set(x, y, z, Material.STONE_BRICK);
            }
        } else {
            for (int x = cx - 6; x <= cx + 6; x++) {
                for (int z = -9; z <= 9; z++) set(x, y, z, Material.STONE_BRICK);
            }
        }
        set(cx, y + 1, cz, Material.GLOWSTONE);
    }

    /**
     * Die Haupt-PvP-Fläche liegt direkt unter dem Spawn und ist absichtlich deutlich groesser
     * als alle anderen Inseln zusammen. Hier soll der Dauerfight stattfinden.
     */
    private void buildMainPvpIsland() {
        floatingIsland(0, PVP_Y, 0, 108, 96, 31,
                Material.GRASS, Material.DIRT, Material.STONE, 101);

        // zweite ueberlappende Form macht die Kontur weniger kreisrund
        floatingIsland(-32, PVP_Y - 1, 18, 67, 57, 24,
                Material.GRASS, Material.DIRT, Material.STONE, 102);
        floatingIsland(44, PVP_Y, -24, 61, 52, 23,
                Material.GRASS, Material.DIRT, Material.STONE, 103);

        // vier gut lesbare Landing Areas unter den Spawn-Ausgaengen
        landingZone(0, PVP_Y + 3, -55);
        landingZone(0, PVP_Y + 3, 55);
        landingZone(-61, PVP_Y + 3, 0);
        landingZone(61, PVP_Y + 3, 0);

        // unregelmaessige Bodenhindernisse fuer echtes 1.8-Melee statt flacher Platte
        for (int i = 0; i < 28; i++) {
            int x = random.nextInt(151) - 75;
            int z = random.nextInt(137) - 68;
            int radius = 2 + random.nextInt(5);
            int h = 1 + random.nextInt(3);
            for (int dy = 0; dy < h; dy++) disc(x, PVP_Y + 2 + dy, z, Math.max(1, radius - dy),
                    i % 3 == 0 ? Material.COBBLESTONE : Material.STONE);
        }
    }

    private void landingZone(int cx, int y, int cz) {
        disc(cx, y, cz, 11, Material.STONE_BRICK);
        ring(cx, y + 1, cz, 12, Material.GLOWSTONE);
        disc(cx, y + 1, cz, 4, Material.GRASS);
    }

    /** Zentraler Wiedererkennungsort: Burgruine mit mehreren Ebenen, Durchgaengen und Dachkampf. */
    private void buildCentralRuins() {
        int y = PVP_Y + 3;
        // Innenhof
        rectangle(-25, y, -22, 25, y, 22, Material.STONE_BRICK);
        rectangle(-20, y + 1, -17, 20, y + 1, 17, Material.GRASS);

        // kaputte Aussenmauern
        wall(-28, y + 1, -26, 28, y + 8, -26, Material.STONE_BRICK);
        wall(-28, y + 1, 26, 5, y + 7, 26, Material.COBBLESTONE);
        wall(15, y + 1, 26, 28, y + 5, 26, Material.COBBLESTONE);
        wall(-28, y + 1, -26, -28, y + 7, 7, Material.STONE_BRICK);
        wall(-28, y + 1, 17, -28, y + 4, 26, Material.COBBLESTONE);
        wall(28, y + 1, -26, 28, y + 8, 26, Material.STONE_BRICK);

        // zwei unterschiedliche Tuerme
        tower(-22, y + 1, -20, 7, 17, Material.STONE_BRICK);
        tower(22, y + 1, 18, 6, 13, Material.COBBLESTONE);

        // zentraler gebrochener Obelisk als sichtbarer Orientierungspunkt
        column(0, y + 1, 0, 15, Material.OBSIDIAN);
        column(1, y + 1, 0, 11, Material.OBSIDIAN);
        column(0, y + 1, 1, 9, Material.OBSIDIAN);
        set(0, y + 16, 0, Material.GLOWSTONE);

        // Seitliche Treppen-/Podestkaempfe
        stairs(-42, y, -8, 12, true, Material.STONE_BRICK);
        stairs(38, y, 15, 11, false, Material.COBBLESTONE);
    }

    /** King Zone liegt als erhoehter Hotspot am Rand der Hauptinsel statt weit weg im Nichts. */
    private void buildKingZone() {
        int cx = -80;
        int cz = -56;
        int y = PVP_Y + 9;

        disc(cx, y, cz, 19, Material.STONE_BRICK);
        ring(cx, y + 1, cz, 20, Material.GOLD_BLOCK);
        disc(cx, y + 1, cz, 7, Material.GOLD_BLOCK);
        column(cx, y + 2, cz, 8, Material.QUARTZ_BLOCK);
        set(cx, y + 10, cz, Material.GLOWSTONE);

        // zwei Rampen statt nur einem campbaren Zugang
        stairs(cx + 21, PVP_Y + 3, cz, 7, true, Material.STONE_BRICK);
        stairs(cx, PVP_Y + 3, cz + 21, 7, false, Material.STONE_BRICK);
    }

    private void buildEndIsland() {
        int cx = 136, cz = -62, y = PVP_Y + 7;
        floatingIsland(cx, y, cz, 34, 29, 24,
                Material.ENDER_STONE, Material.OBSIDIAN, Material.STONE, 201);
        ring(cx, y + 2, cz, 15, Material.OBSIDIAN);
        tower(cx + 13, y + 2, cz - 7, 5, 16, Material.OBSIDIAN);
        tower(cx - 14, y + 2, cz + 10, 4, 11, Material.ENDER_STONE);
        platform(cx, y + 3, cz, 8, Material.ENDER_STONE);

        // kurzer, breiter Zugang von der Hauptmap; Rest der Insel bleibt pearl-freundlich offen
        bridge(95, PVP_Y + 3, -43, 108, y + 1, -50, 5, Material.OBSIDIAN);
    }

    private void buildBlacksmithIsland() {
        int cx = 124, cz = 73, y = PVP_Y + 4;
        floatingIsland(cx, y, cz, 31, 27, 22,
                Material.COBBLESTONE, Material.STONE, Material.STONE, 301);
        rectangle(cx - 16, y + 2, cz - 12, cx + 16, y + 2, cz + 12, Material.STONE_BRICK);
        wall(cx - 16, y + 3, cz - 12, cx + 16, y + 9, cz - 12, Material.COBBLESTONE);
        wall(cx - 16, y + 3, cz + 12, cx + 16, y + 7, cz + 12, Material.COBBLESTONE);
        chimney(cx + 11, y + 3, cz + 7);
        for (int x = cx - 8; x <= cx + 8; x += 4) set(x, y + 3, cz, Material.ANVIL);
        bridge(92, PVP_Y + 3, 48, 101, y + 1, 61, 4, Material.COBBLESTONE);
    }

    private void buildGoldIsland() {
        int cx = -132, cz = 58, y = PVP_Y + 5;
        floatingIsland(cx, y, cz, 29, 25, 21,
                Material.SANDSTONE, Material.SANDSTONE, Material.STONE, 401);
        pyramid(cx, y + 2, cz, 15, Material.SANDSTONE);
        ring(cx, y + 3, cz, 9, Material.GOLD_BLOCK);
        bridge(-96, PVP_Y + 3, 39, -108, y + 1, 48, 4, Material.SANDSTONE);
    }

    private void buildMerchantIsland() {
        int cx = 36, cz = 133, y = PVP_Y + 8;
        floatingIsland(cx, y, cz, 37, 31, 24,
                Material.GRASS, Material.DIRT, Material.STONE, 501);
        // Marktstrasse mit mehreren offenen Staenden
        rectangle(cx - 24, y + 2, cz - 5, cx + 24, y + 2, cz + 5, Material.WOOD);
        for (int x = cx - 20; x <= cx + 20; x += 10) {
            marketStall(x, y + 3, cz - 13);
            marketStall(x, y + 3, cz + 13);
        }
        bridge(28, PVP_Y + 3, 92, 32, y + 1, 102, 5, Material.WOOD);
    }

    private void buildEnchantIsland() {
        int cx = -64, cz = 132, y = PVP_Y + 10;
        floatingIsland(cx, y, cz, 30, 27, 23,
                Material.GRASS, Material.DIRT, Material.STONE, 601);
        disc(cx, y + 2, cz, 12, Material.STONE_BRICK);
        column(cx, y + 3, cz, 6, Material.OBSIDIAN);
        set(cx, y + 9, cz, Material.ENCHANTMENT_TABLE);
        for (int i = -8; i <= 8; i += 4) {
            set(cx + i, y + 3, cz + 8, Material.BOOKSHELF);
            set(cx + i, y + 3, cz - 8, Material.BOOKSHELF);
        }
        bridge(-47, PVP_Y + 3, 92, -56, y + 1, 104, 4, Material.STONE_BRICK);
    }

    private void buildOuterSatellites() {
        floatingIsland(188, PVP_Y + 18, 25, 18, 15, 18,
                Material.GRASS, Material.DIRT, Material.STONE, 701);
        tower(188, PVP_Y + 20, 25, 5, 18, Material.STONE_BRICK);

        floatingIsland(-183, PVP_Y + 12, -8, 19, 16, 19,
                Material.STONE, Material.COBBLESTONE, Material.STONE, 702);
        platform(-183, PVP_Y + 15, -8, 7, Material.MOSSY_COBBLESTONE);

        floatingIsland(88, PVP_Y + 25, -159, 17, 14, 18,
                Material.GRASS, Material.DIRT, Material.STONE, 703);
        floatingIsland(-70, PVP_Y + 22, -164, 16, 13, 17,
                Material.GRASS, Material.DIRT, Material.STONE, 704);

        floatingIsland(156, PVP_Y + 16, 139, 15, 13, 17,
                Material.GRASS, Material.DIRT, Material.STONE, 705);
        floatingIsland(-150, PVP_Y + 20, 139, 17, 15, 19,
                Material.GRASS, Material.DIRT, Material.STONE, 706);
    }

    private void buildPearlPads() {
        platform(108, PVP_Y + 18, -120, 6, Material.OBSIDIAN);
        platform(-120, PVP_Y + 17, -119, 6, Material.STONE_BRICK);
        platform(174, PVP_Y + 24, 83, 5, Material.QUARTZ_BLOCK);
        platform(-172, PVP_Y + 24, 80, 5, Material.SANDSTONE);
        platform(4, PVP_Y + 30, 176, 5, Material.WOOD);
        platform(13, PVP_Y + 28, -177, 5, Material.STONE_BRICK);
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
                + ChatColor.GREEN + "• High-Spawn SkyPvP Arena wurde fertig gebaut.");
        initiator.teleport(spawn);
        initiator.sendMessage(ChatColor.YELLOW + "Spring vom Spawn nach unten. Die grosse Hauptinsel ist die PvP-Zone.");
    }

    private void floatingIsland(int cx, int topY, int cz, int rx, int rz, int depth,
                                Material top, Material middle, Material core, int seed) {
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
                    Material material = below == 0 ? top : (below <= 3 ? middle : core);
                    if (below > 4 && r.nextInt(9) == 0) material = Material.COBBLESTONE;
                    set(cx + dx, y, cz + dz, material);
                }
            }
        }
    }

    private void tower(int cx, int y, int cz, int radius, int height, Material material) {
        for (int dy = 0; dy < height; dy++) {
            int rr = radius;
            for (int x = -rr; x <= rr; x++) {
                for (int z = -rr; z <= rr; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d >= rr - 1.2 && d <= rr + 0.4) set(cx + x, y + dy, cz + z, material);
                }
            }
        }
        disc(cx, y + height - 1, cz, radius, material);
        ring(cx, y + height, cz, radius + 1, material);
    }

    private void chimney(int x, int y, int z) {
        column(x, y, z, 12, Material.BRICK);
        set(x, y + 12, z, Material.NETHERRACK);
        set(x, y + 13, z, Material.FIRE);
    }

    private void pyramid(int cx, int y, int cz, int radius, Material material) {
        for (int level = 0; level < radius / 2; level++) {
            int r = radius - level * 2;
            if (r < 2) break;
            rectangle(cx - r, y + level, cz - r, cx + r, y + level, cz + r, material);
        }
    }

    private void marketStall(int cx, int y, int cz) {
        rectangle(cx - 3, y, cz - 2, cx + 3, y, cz + 2, Material.WOOD);
        column(cx - 3, y + 1, cz - 2, 4, Material.FENCE);
        column(cx + 3, y + 1, cz - 2, 4, Material.FENCE);
        column(cx - 3, y + 1, cz + 2, 4, Material.FENCE);
        column(cx + 3, y + 1, cz + 2, 4, Material.FENCE);
        rectangle(cx - 4, y + 5, cz - 3, cx + 4, y + 5, cz + 3, Material.WOOD);
    }

    private void stairs(int x, int y, int z, int length, boolean xAxis, Material material) {
        for (int i = 0; i < length; i++) {
            int xx = xAxis ? x + i * 2 : x;
            int zz = xAxis ? z : z + i * 2;
            rectangle(xx - 2, y + i, zz - 2, xx + 2, y + i, zz + 2, material);
        }
    }

    private void bridge(int x1, int y1, int z1, int x2, int y2, int z2, int width, Material material) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            for (int dx = -width / 2; dx <= width / 2; dx++) {
                for (int dz = -width / 2; dz <= width / 2; dz++) set(x + dx, y, z + dz, material);
            }
        }
    }

    private void wall(int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++) set(x, y, z, material);
    }

    private void rectangle(int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++) set(x, y, z, material);
    }

    private void disc(int cx, int y, int cz, int radius, Material material) {
        int r2 = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= r2) set(cx + x, y, cz + z, material);
            }
        }
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
