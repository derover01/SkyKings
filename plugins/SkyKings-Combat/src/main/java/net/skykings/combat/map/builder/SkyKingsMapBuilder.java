package net.skykings.combat.map.builder;

import net.skykings.combat.map.MapLootTier;
import net.skykings.combat.spawn.SpawnService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Baut die erste spielbare SkyKings-2.0-SkyPvP-Map vollstaendig aus Code. */
public final class SkyKingsMapBuilder {

    private static final int BLOCKS_PER_TICK = 4500;

    private static final class Placement {
        final int x, y, z;
        final Material material;
        Placement(int x, int y, int z, Material material) {
            this.x = x; this.y = y; this.z = z; this.material = material;
        }
    }

    private static final class LootPoint {
        final int x, y, z;
        final MapLootTier tier;
        LootPoint(int x, int y, int z, MapLootTier tier) {
            this.x = x; this.y = y; this.z = z; this.tier = tier;
        }
    }

    private final JavaPlugin plugin;
    private final World world;
    private final SpawnService spawnService;
    private final Player initiator;
    private final List<Placement> blocks = new ArrayList<Placement>();
    private final List<LootPoint> lootPoints = new ArrayList<LootPoint>();
    private final List<Location> supplyPoints = new ArrayList<Location>();
    private final Random random = new Random(88421991L);

    public SkyKingsMapBuilder(JavaPlugin plugin, World world, SpawnService spawnService, Player initiator) {
        this.plugin = plugin;
        this.world = world;
        this.spawnService = spawnService;
        this.initiator = initiator;
    }

    public void start() {
        planMap();
        final int total = blocks.size();
        initiator.sendMessage(ChatColor.GOLD + "SkyKings Map v1 wird gebaut: " + ChatColor.WHITE + total + " Bloecke.");

        new BukkitRunnable() {
            private int index;
            @Override public void run() {
                int end = Math.min(total, index + BLOCKS_PER_TICK);
                while (index < end) {
                    Placement p = blocks.get(index++);
                    world.getBlockAt(p.x, p.y, p.z).setType(p.material, false);
                }
                if (index >= total) {
                    cancel();
                    finishMap();
                } else if (index % (BLOCKS_PER_TICK * 20) < BLOCKS_PER_TICK) {
                    initiator.sendMessage(ChatColor.GRAY + "Map-Bau: " + ChatColor.YELLOW + ((index * 100L) / total) + "%");
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void planMap() {
        island(0, 100, 0, 38, 34, 25, Material.GRASS, Material.DIRT, Material.STONE, 101);
        spawnPavilion();

        island(0, 78, -92, 76, 64, 34, Material.GRASS, Material.DIRT, Material.STONE, 202);
        ruins(0, 79, -92);

        island(-112, 92, -128, 31, 28, 29, Material.STONE, Material.COBBLESTONE, Material.STONE, 303);
        kingIsland(-112, 93, -128);
        island(116, 91, -132, 30, 27, 28, Material.ENDER_STONE, Material.OBSIDIAN, Material.STONE, 404);
        endIsland(116, 92, -132);
        island(-150, 88, 10, 26, 23, 25, Material.SANDSTONE, Material.SANDSTONE, Material.STONE, 505);
        goldIsland(-150, 89, 10);
        island(112, 85, 22, 28, 25, 26, Material.COBBLESTONE, Material.STONE, Material.STONE, 606);
        blacksmithIsland(112, 86, 22);
        island(105, 94, 112, 34, 29, 27, Material.GRASS, Material.DIRT, Material.STONE, 707);
        merchantIsland(105, 95, 112);
        island(-76, 95, 122, 29, 25, 25, Material.GRASS, Material.DIRT, Material.STONE, 808);
        levelIsland(-76, 96, 122);
        island(158, 91, 84, 20, 18, 22, Material.GRASS, Material.DIRT, Material.STONE, 909);
        potionIsland(158, 92, 84);
        island(-166, 82, -75, 22, 20, 24, Material.STONE, Material.COBBLESTONE, Material.STONE, 1001);
        bowTowerIsland(-166, 83, -75);
        island(8, 88, 160, 31, 27, 25, Material.GRASS, Material.DIRT, Material.STONE, 1102);
        eventIsland(8, 89, 160);
        island(178, 103, -42, 15, 13, 20, Material.MOSSY_COBBLESTONE, Material.COBBLESTONE, Material.STONE, 1203);
        secretIsland(178, 104, -42);

        island(-65, 85, -190, 15, 13, 18, Material.GRASS, Material.DIRT, Material.STONE, 1304);
        island(62, 82, -205, 17, 14, 19, Material.GRASS, Material.DIRT, Material.STONE, 1405);
        island(185, 88, 20, 14, 12, 17, Material.GRASS, Material.DIRT, Material.STONE, 1506);
        island(-135, 91, 105, 16, 13, 18, Material.GRASS, Material.DIRT, Material.STONE, 1607);

        bridge(-17, 98, -25, -4, 80, -38, 5, Material.SMOOTH_BRICK);
        bridge(19, 98, -24, 42, 80, -52, 5, Material.SMOOTH_BRICK);
        bridge(-42, 79, -120, -82, 92, -127, 3, Material.COBBLESTONE);
        bridge(45, 80, -119, 87, 91, -130, 3, Material.QUARTZ_BLOCK);
        bridge(-55, 80, -64, -124, 88, -3, 3, Material.SANDSTONE);
        bridge(55, 80, -65, 91, 85, 12, 3, Material.COBBLESTONE);
        bridge(88, 86, 35, 96, 94, 85, 3, Material.WOOD);
        bridge(83, 95, 117, -47, 95, 122, 3, Material.WOOD);
        bridge(129, 95, 110, 144, 92, 91, 3, Material.WOOD);
        bridge(-101, 90, 17, -151, 83, -58, 3, Material.COBBLESTONE);
        bridge(-45, 96, 132, -4, 89, 154, 3, Material.SMOOTH_BRICK);

        platform(61, 90, -153, 6, Material.OBSIDIAN);
        platform(-89, 96, -171, 5, Material.SMOOTH_BRICK);
        platform(145, 99, -70, 5, Material.QUARTZ_BLOCK);
        platform(-20, 101, 84, 5, Material.WOOD);

        loot(-20, 80, -77, MapLootTier.COMMON);
        loot(27, 80, -104, MapLootTier.COMMON);
        loot(-10, 81, -133, MapLootTier.COMMON);
        loot(-53, 81, -91, MapLootTier.COMMON);
        loot(46, 81, -75, MapLootTier.COMMON);
        loot(113, 93, -132, MapLootTier.RARE);
        loot(111, 87, 22, MapLootTier.RARE);
        loot(-75, 97, 122, MapLootTier.RARE);
        loot(-150, 90, 10, MapLootTier.RARE);
        loot(105, 96, 112, MapLootTier.RARE);
        loot(-112, 95, -128, MapLootTier.EPIC);
        loot(8, 90, 160, MapLootTier.EPIC);
        loot(178, 105, -42, MapLootTier.EPIC);

        supply(9, 80, -62); supply(-39, 80, -110); supply(38, 80, -127); supply(-112, 94, -111);
        supply(116, 93, -116); supply(105, 96, 96); supply(8, 90, 150); supply(-150, 90, 22);
    }

    private void finishMap() {
        for (LootPoint point : lootPoints) {
            Block chest = world.getBlockAt(point.x, point.y, point.z);
            chest.setType(Material.CHEST, false);
            signMarker(point.x + 1, point.y, point.z, point.tier.name() + " LOOT");
        }
        for (Location point : supplyPoints) signMarker(point.getBlockX() + 2, point.getBlockY(), point.getBlockZ(), "SUPPLY POINT");

        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doFireTick", "false");

        Location spawn = new Location(world, 0.5D, 106.0D, 0.5D, 180.0F, 0.0F);
        spawnService.setSpawn(spawn);
        initiator.teleport(spawn.clone().add(0.0D, 1.0D, 0.0D));

        Bukkit.broadcastMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "SKYKINGS MAP " + ChatColor.GREEN
                + "• Map v1 in Welt '" + world.getName() + "' wurde fertig generiert.");
        initiator.sendMessage(ChatColor.YELLOW + "Loot-Chests sind markiert. Mit /maploot set <tier> registrieren.");
        initiator.sendMessage(ChatColor.YELLOW + "Supply-Plattformen sind markiert. Dort /supplydrop addpoint nutzen.");
    }

    private void island(int cx, int topY, int cz, int rx, int rz, int depth, Material top, Material middle, Material core, int seed) {
        Random local = new Random(seed);
        for (int dx = -rx - 2; dx <= rx + 2; dx++) {
            for (int dz = -rz - 2; dz <= rz + 2; dz++) {
                double nx = dx / (double) rx, nz = dz / (double) rz;
                double dist = Math.sqrt(nx * nx + nz * nz);
                double noise = Math.sin((dx + seed) * 0.31D) * 0.055D + Math.cos((dz - seed) * 0.27D) * 0.05D
                        + (local.nextDouble() - 0.5D) * 0.035D;
                if (dist > 1.0D + noise) continue;
                int surface = topY + (int) Math.round(Math.sin(dx * 0.16D) + Math.cos(dz * 0.13D));
                double body = Math.max(0.0D, 1.0D - Math.pow(Math.min(1.0D, dist), 1.55D));
                int thickness = Math.max(3, (int) Math.round(4.0D + depth * body + local.nextDouble() * 3.0D));
                for (int y = surface; y >= surface - thickness; y--) {
                    int below = surface - y;
                    Material material = below == 0 ? top : (below <= 3 ? middle : (local.nextInt(8) == 0 ? Material.COBBLESTONE : core));
                    set(cx + dx, y, cz + dz, material);
                }
            }
        }
    }

    private void spawnPavilion() {
        platform(0, 104, 0, 12, Material.QUARTZ_BLOCK); ring(0, 105, 0, 13, Material.SMOOTH_BRICK);
        for (int[] p : new int[][]{{10,10},{10,-10},{-10,10},{-10,-10}}) { column(p[0], 105, p[1], 7, Material.QUARTZ_BLOCK); set(p[0], 112, p[1], Material.GLOWSTONE); }
        ring(0, 112, 0, 11, Material.QUARTZ_BLOCK);
        bridge(0, 105, -13, 0, 101, -31, 5, Material.SMOOTH_BRICK); bridge(-13, 105, 0, -31, 101, 0, 5, Material.SMOOTH_BRICK);
        bridge(13, 105, 0, 31, 101, 0, 5, Material.SMOOTH_BRICK); bridge(0, 105, 13, 0, 101, 31, 5, Material.SMOOTH_BRICK);
    }

    private void ruins(int cx, int y, int cz) {
        for (int i = 0; i < 7; i++) { int x = cx - 45 + random.nextInt(91), z = cz - 35 + random.nextInt(71), h = 4 + random.nextInt(8); column(x, y + 1, z, h, i % 2 == 0 ? Material.COBBLESTONE : Material.SMOOTH_BRICK); }
        wall(cx - 18, y + 1, cz - 8, cx + 18, y + 5, cz - 8, Material.SMOOTH_BRICK);
        wall(cx + 24, y + 1, cz + 14, cx + 24, y + 4, cz + 34, Material.COBBLESTONE);
    }

    private void kingIsland(int cx, int y, int cz) {
        platform(cx, y + 1, cz, 12, Material.QUARTZ_BLOCK); ring(cx, y + 2, cz, 13, Material.GOLD_BLOCK); platform(cx, y + 2, cz, 5, Material.GOLD_BLOCK);
        for (int[] p : new int[][]{{9,9},{9,-9},{-9,9},{-9,-9}}) { column(cx + p[0], y + 2, cz + p[1], 8, Material.SMOOTH_BRICK); set(cx + p[0], y + 10, cz + p[1], Material.GLOWSTONE); }
        wall(cx - 3, y + 3, cz - 8, cx + 3, y + 8, cz - 8, Material.QUARTZ_BLOCK);
    }

    private void endIsland(int cx, int y, int cz) { ring(cx, y + 1, cz, 11, Material.OBSIDIAN); column(cx, y + 1, cz - 10, 9, Material.OBSIDIAN); platform(cx + 12, y + 5, cz + 8, 3, Material.OBSIDIAN); }
    private void goldIsland(int cx, int y, int cz) { platform(cx, y + 1, cz, 9, Material.SANDSTONE); ring(cx, y + 2, cz, 10, Material.GOLD_BLOCK); column(cx, y + 2, cz, 7, Material.GOLD_BLOCK); }
    private void blacksmithIsland(int cx, int y, int cz) { platform(cx, y + 1, cz, 10, Material.SMOOTH_BRICK); wall(cx - 9, y + 2, cz - 7, cx + 9, y + 6, cz - 7, Material.COBBLESTONE); set(cx - 3, y + 2, cz, Material.ANVIL); set(cx + 3, y + 2, cz, Material.ANVIL); }
    private void merchantIsland(int cx, int y, int cz) { platform(cx, y + 1, cz, 14, Material.WOOD); for (int i = -2; i <= 2; i++) { int sx = cx + i * 6; wall(sx - 2, y + 2, cz - 8, sx + 2, y + 4, cz - 8, Material.WOOD); platform(sx, y + 2, cz - 5, 2, Material.WOOD); } }
    private void levelIsland(int cx, int y, int cz) { platform(cx, y + 1, cz, 11, Material.SMOOTH_BRICK); ring(cx, y + 2, cz, 9, Material.LAPIS_BLOCK); set(cx, y + 3, cz, Material.ENCHANTMENT_TABLE); }
    private void potionIsland(int cx, int y, int cz) { platform(cx, y + 1, cz, 8, Material.WOOD); ring(cx, y + 2, cz, 7, Material.GLASS); set(cx, y + 2, cz, Material.BREWING_STAND); }
    private void bowTowerIsland(int cx, int y, int cz) { platform(cx, y + 1, cz, 8, Material.SMOOTH_BRICK); hollowTower(cx, y + 2, cz, 5, 18, Material.SMOOTH_BRICK); platform(cx, y + 20, cz, 7, Material.COBBLESTONE); }
    private void eventIsland(int cx, int y, int cz) { platform(cx, y + 1, cz, 16, Material.SMOOTH_BRICK); ring(cx, y + 2, cz, 15, Material.IRON_BLOCK); ring(cx, y + 2, cz, 9, Material.GOLD_BLOCK); }
    private void secretIsland(int cx, int y, int cz) { platform(cx, y + 1, cz, 6, Material.MOSSY_COBBLESTONE); hollowTower(cx, y + 2, cz, 4, 8, Material.MOSSY_COBBLESTONE); platform(cx - 5, y - 8, cz + 4, 3, Material.OBSIDIAN); }

    private void hollowTower(int cx, int y, int cz, int radius, int height, Material material) { for (int yy = y; yy < y + height; yy++) for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) if (Math.max(Math.abs(dx), Math.abs(dz)) == radius && !(yy == y + 1 && dz == -radius && Math.abs(dx) <= 1)) set(cx + dx, yy, cz + dz, material); }
    private void platform(int cx, int y, int cz, int radius, Material material) { int r2 = radius * radius; for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) if (dx * dx + dz * dz <= r2) set(cx + dx, y, cz + dz, material); }
    private void ring(int cx, int y, int cz, int radius, Material material) { int outer = radius * radius, inner = (radius - 2) * (radius - 2); for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) { int d = dx * dx + dz * dz; if (d <= outer && d >= inner) set(cx + dx, y, cz + dz, material); } }
    private void column(int x, int y, int z, int height, Material material) { for (int yy = y; yy < y + height; yy++) set(x, yy, z, material); }
    private void wall(int x1, int y1, int z1, int x2, int y2, int z2, Material material) { for (int x = Math.min(x1,x2); x <= Math.max(x1,x2); x++) for (int y = Math.min(y1,y2); y <= Math.max(y1,y2); y++) for (int z = Math.min(z1,z2); z <= Math.max(z1,z2); z++) set(x,y,z,material); }

    private void bridge(int x1, int y1, int z1, int x2, int y2, int z2, int width, Material material) {
        int steps = Math.max(Math.abs(x2-x1), Math.max(Math.abs(y2-y1), Math.abs(z2-z1))) * 2;
        for (int i = 0; i <= Math.max(1, steps); i++) { double t = i / (double) Math.max(1, steps); int x = (int)Math.round(x1+(x2-x1)*t), y=(int)Math.round(y1+(y2-y1)*t), z=(int)Math.round(z1+(z2-z1)*t); for (int w=-width/2; w<=width/2; w++) if (Math.abs(x2-x1)>Math.abs(z2-z1)) set(x,y,z+w,material); else set(x+w,y,z,material); }
    }

    private void loot(int x, int y, int z, MapLootTier tier) { set(x, y - 1, z, Material.GLOWSTONE); lootPoints.add(new LootPoint(x,y,z,tier)); }
    private void supply(int x, int y, int z) { platform(x, y - 1, z, 2, Material.IRON_BLOCK); supplyPoints.add(new Location(world,x,y,z)); }
    private void signMarker(int x, int y, int z, String label) { world.getBlockAt(x, y, z).setType(Material.SIGN_POST); if (world.getBlockAt(x,y,z).getState() instanceof org.bukkit.block.Sign) { org.bukkit.block.Sign sign=(org.bukkit.block.Sign)world.getBlockAt(x,y,z).getState(); sign.setLine(0, ChatColor.GOLD + "[SKYKINGS]"); sign.setLine(1, label.length()>15?label.substring(0,15):label); sign.update(true); } }
    private void set(int x, int y, int z, Material material) { if (y > 0 && y < 255) blocks.add(new Placement(x,y,z,material)); }
}
