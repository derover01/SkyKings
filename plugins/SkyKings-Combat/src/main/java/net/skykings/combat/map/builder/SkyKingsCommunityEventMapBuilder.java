package net.skykings.combat.map.builder;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Baut eine bewusst kompakte Community-/Giveaway-Map fuer Freitag-/Samstag-Events.
 * Keine Combat-Arena: zentraler Event-Platz, kleine Buehne, Zuschauerbereich und Preis-Podeste.
 */
public final class SkyKingsCommunityEventMapBuilder {

    private static final int BASE_Y = 78;

    private final World world;
    private final Player player;

    public SkyKingsCommunityEventMapBuilder(World world, Player player) {
        this.world = world;
        this.player = player;
    }

    public void build() {
        configureWorld();
        buildFloatingIsland();
        buildPlaza();
        buildStage();
        buildAudience();
        buildPrizePodiums();
        buildEntry();

        Location spawn = new Location(world, 0.5D, BASE_Y + 3.0D, 18.5D, 180.0F, 2.0F);
        world.setSpawnLocation(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());
        player.teleport(spawn);
        player.sendMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "SKYKINGS " + ChatColor.DARK_GRAY + "• "
                + ChatColor.WHITE + "Community-Eventmap erstellt.");
        player.sendMessage(ChatColor.GRAY + "Kompakte Buehne, Zuschauerflaeche und Giveaway-Podeste sind bereit fuer den Feinschliff.");
    }

    private void configureWorld() {
        world.setPVP(false);
        world.setStorm(false);
        world.setThundering(false);
        world.setTime(6000L);
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("doFireTick", "false");
    }

    private void buildFloatingIsland() {
        for (int x = -25; x <= 25; x++) {
            for (int z = -24; z <= 24; z++) {
                double distance = (x * x) / 625.0D + (z * z) / 576.0D;
                if (distance > 1.0D) continue;

                set(x, BASE_Y, z, Material.STONE);
                set(x, BASE_Y + 1, z, Material.SMOOTH_BRICK);

                if (distance < 0.78D) {
                    set(x, BASE_Y - 1, z, Material.STONE);
                }
                if (distance < 0.55D && ((Math.abs(x) + Math.abs(z)) % 3 == 0)) {
                    set(x, BASE_Y - 2, z, Material.COBBLESTONE);
                }
            }
        }
    }

    private void buildPlaza() {
        fill(-17, 17, BASE_Y + 2, -15, 19, Material.SMOOTH_BRICK);
        for (int x = -16; x <= 16; x++) {
            if (x % 4 == 0) {
                set(x, BASE_Y + 2, 18, Material.GLOWSTONE);
                set(x, BASE_Y + 2, -14, Material.GLOWSTONE);
            }
        }
        for (int z = -13; z <= 17; z++) {
            if (z % 4 == 0) {
                set(-16, BASE_Y + 2, z, Material.GLOWSTONE);
                set(16, BASE_Y + 2, z, Material.GLOWSTONE);
            }
        }
    }

    private void buildStage() {
        fill(-9, 9, BASE_Y + 3, -14, -7, Material.QUARTZ_BLOCK);
        fill(-8, 8, BASE_Y + 4, -13, -8, Material.SMOOTH_BRICK);
        fill(-7, 7, BASE_Y + 5, -13, -12, Material.QUARTZ_BLOCK);

        // Rueckwand mit SkyKings-Rahmen.
        for (int x = -9; x <= 9; x++) {
            for (int y = BASE_Y + 5; y <= BASE_Y + 10; y++) {
                if (x == -9 || x == 9 || y == BASE_Y + 10) set(x, y, -14, Material.QUARTZ_BLOCK);
            }
        }
        for (int x = -6; x <= 6; x += 3) set(x, BASE_Y + 8, -14, Material.SEA_LANTERN);

        // Staff-/Host-Mitte.
        fill(-2, 2, BASE_Y + 5, -11, -9, Material.QUARTZ_BLOCK);
        set(0, BASE_Y + 6, -10, Material.BEACON);
    }

    private void buildAudience() {
        // Freie Zuschauerflaeche mit zwei niedrigen Sitzreihen, damit es nicht wie eine Arena wirkt.
        for (int x = -12; x <= 12; x += 3) {
            set(x, BASE_Y + 3, 7, Material.WOOD_STAIRS);
            set(x, BASE_Y + 3, 11, Material.WOOD_STAIRS);
        }
        for (int x = -13; x <= 13; x += 13) {
            set(x, BASE_Y + 3, 3, Material.FENCE);
            set(x, BASE_Y + 4, 3, Material.GLOWSTONE);
        }
    }

    private void buildPrizePodiums() {
        buildPodium(-6, -3, Material.GOLD_BLOCK);
        buildPodium(0, -3, Material.DIAMOND_BLOCK);
        buildPodium(6, -3, Material.EMERALD_BLOCK);
    }

    private void buildPodium(int x, int z, Material accent) {
        fill(x - 1, x + 1, BASE_Y + 3, z - 1, z + 1, Material.QUARTZ_BLOCK);
        set(x, BASE_Y + 4, z, accent);
        set(x, BASE_Y + 5, z, Material.FENCE);
        set(x, BASE_Y + 6, z, Material.GLOWSTONE);
    }

    private void buildEntry() {
        fill(-4, 4, BASE_Y + 2, 17, 22, Material.QUARTZ_BLOCK);
        for (int y = BASE_Y + 3; y <= BASE_Y + 8; y++) {
            set(-5, y, 20, Material.QUARTZ_BLOCK);
            set(5, y, 20, Material.QUARTZ_BLOCK);
        }
        for (int x = -5; x <= 5; x++) set(x, BASE_Y + 8, 20, Material.QUARTZ_BLOCK);
        set(-3, BASE_Y + 8, 20, Material.SEA_LANTERN);
        set(0, BASE_Y + 8, 20, Material.SEA_LANTERN);
        set(3, BASE_Y + 8, 20, Material.SEA_LANTERN);
    }

    private void fill(int minX, int maxX, int y, int minZ, int maxZ, Material material) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) set(x, y, z, material);
        }
    }

    private void set(int x, int y, int z, Material material) {
        world.getBlockAt(x, y, z).setType(material);
    }
}
