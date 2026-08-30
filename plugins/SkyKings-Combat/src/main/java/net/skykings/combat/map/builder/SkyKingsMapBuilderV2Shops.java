package net.skykings.combat.map.builder;

import net.skykings.combat.spawn.SpawnService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Startet die klassische SkyKings Map V2 und setzt danach den Spawn-Markt.
 * Die Shops werden erst gebaut, wenn der V2-Builder den globalen Spawn gesetzt hat.
 */
public final class SkyKingsMapBuilderV2Shops {

    private static final int SPAWN_Y = 208;

    private final JavaPlugin plugin;
    private final World world;
    private final SpawnService spawnService;
    private final Player initiator;

    public SkyKingsMapBuilderV2Shops(JavaPlugin plugin, World world, SpawnService spawnService, Player initiator) {
        this.plugin = plugin;
        this.world = world;
        this.spawnService = spawnService;
        this.initiator = initiator;
    }

    public void start() {
        new SkyKingsMapBuilderV2(plugin, world, spawnService, initiator).start();

        final BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                Location spawn = spawnService.getSpawn();
                if (spawn == null || spawn.getWorld() == null || !spawn.getWorld().getName().equals(world.getName())) return;
                if (spawn.getY() < 190.0D) return;

                task[0].cancel();
                buildSpawnMarket();
                initiator.sendMessage(ChatColor.GOLD + "Spawn-Markt fertig: " + ChatColor.WHITE
                        + "PvP, Potions, Pearls, Nethersterne und Crates.");
            }
        }, 20L, 20L);
    }

    private void buildSpawnMarket() {
        // Diagonale Shop-Fluegel lassen die vier grossen Absprung-Balkone frei.
        shop(-24, SPAWN_Y + 1, -15, "PVP SHOP", Material.IRON_BLOCK, Material.STONE_BRICK);
        shop(24, SPAWN_Y + 1, -15, "POTION SHOP", Material.BREWING_STAND, Material.QUARTZ_BLOCK);
        shop(-24, SPAWN_Y + 1, 15, "PEARL SHOP", Material.ENDER_STONE, Material.OBSIDIAN);
        shop(24, SPAWN_Y + 1, 15, "NETHERSTAR", Material.GOLD_BLOCK, Material.QUARTZ_BLOCK);

        // Crates / Rewards als eigener kleiner Bereich hinter dem zentralen Spawnpunkt.
        crateArea(0, SPAWN_Y + 1, 25);

        // Platzhalter fuer Rank/BattlePass/Top-NPCs auf der gegenueberliegenden Seite.
        infoArea(0, SPAWN_Y + 1, -25);
    }

    private void shop(int cx, int y, int cz, String title, Material accent, Material frame) {
        // 13x9 offener Stand mit Rueckwand und Dach; NPC/Villager kommt spaeter in die Mitte.
        fill(cx - 6, y, cz - 4, cx + 6, y, cz + 4, Material.STONE_BRICK);
        fill(cx - 6, y + 1, cz + 4, cx + 6, y + 6, cz + 4, frame);

        for (int dy = 1; dy <= 6; dy++) {
            set(cx - 6, y + dy, cz - 4, frame);
            set(cx + 6, y + dy, cz - 4, frame);
            set(cx - 6, y + dy, cz + 4, frame);
            set(cx + 6, y + dy, cz + 4, frame);
        }
        fill(cx - 6, y + 7, cz - 4, cx + 6, y + 7, cz + 4, frame);
        fill(cx - 5, y + 7, cz - 3, cx + 5, y + 7, cz + 3, Material.WOOD);

        // Verkaufstheke.
        fill(cx - 4, y + 1, cz, cx + 4, y + 2, cz, Material.WOOD);
        set(cx, y + 2, cz + 1, accent);
        set(cx - 3, y + 2, cz + 1, Material.GLOWSTONE);
        set(cx + 3, y + 2, cz + 1, Material.GLOWSTONE);

        // Schild an der Front.
        setSign(cx, y + 3, cz - 4, title, "", "SkyKings", "");
    }

    private void crateArea(int cx, int y, int cz) {
        fill(cx - 10, y, cz - 5, cx + 10, y, cz + 5, Material.QUARTZ_BLOCK);
        for (int x = cx - 8; x <= cx + 8; x += 4) {
            set(x, y + 1, cz, Material.CHEST);
            set(x, y + 1, cz + 3, Material.ENDER_CHEST);
        }
        setSign(cx, y + 2, cz - 5, "CRATES", "Rewards", "Preview / Open", "");
    }

    private void infoArea(int cx, int y, int cz) {
        fill(cx - 10, y, cz - 4, cx + 10, y, cz + 4, Material.STONE_BRICK);
        for (int x = cx - 8; x <= cx + 8; x += 4) {
            for (int dy = 1; dy <= 4; dy++) set(x, y + dy, cz + 3, Material.QUARTZ_BLOCK);
            set(x, y + 5, cz + 3, Material.GLOWSTONE);
        }
        setSign(cx, y + 2, cz - 4, "RANKS / TOP", "Battle Pass", "Kits / Info", "");
    }

    private void setSign(int x, int y, int z, String l1, String l2, String l3, String l4) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.WALL_SIGN);
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            sign.setLine(0, ChatColor.GOLD + l1);
            sign.setLine(1, ChatColor.GRAY + l2);
            sign.setLine(2, ChatColor.WHITE + l3);
            sign.setLine(3, ChatColor.DARK_GRAY + l4);
            sign.update(true);
        }
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) set(x, y, z, material);
            }
        }
    }

    private void set(int x, int y, int z, Material material) {
        world.getBlockAt(x, y, z).setType(material, false);
    }
}
