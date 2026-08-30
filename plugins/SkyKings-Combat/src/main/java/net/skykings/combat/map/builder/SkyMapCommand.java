package net.skykings.combat.map.builder;

import net.skykings.combat.map.builder.v3.SkyKingsMapBuilderV3;
import net.skykings.combat.spawn.SpawnService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/** Verwaltet die aktuelle SkyKings Arena und kann bestehende SkyPvP-Welten laden. */
public final class SkyMapCommand implements CommandExecutor {

    private static final String IMPORT_WORLD = "SkyPvP";
    private static final double IMPORT_X = 21.88D;
    private static final double IMPORT_Y = 151.47D;
    private static final double IMPORT_Z = -83.51D;

    private final JavaPlugin plugin;
    private final SpawnService spawnService;

    public SkyMapCommand(JavaPlugin plugin, SpawnService spawnService) {
        this.plugin = plugin;
        this.spawnService = spawnService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("skykings.admin.map")) {
            player.sendMessage(ChatColor.RED + "Dafuer hast du keine Berechtigung.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "/skymap load [Weltname]");
            player.sendMessage(ChatColor.GRAY + "Laedt die handgebaute SkyPvP-Welt. Standard: " + IMPORT_WORLD);
            player.sendMessage(ChatColor.DARK_GRAY + "/skymap generate [Weltname] bleibt nur fuer alte Testwelten verfuegbar.");
            return true;
        }

        if (args[0].equalsIgnoreCase("load")) {
            return loadExistingWorld(player, args.length >= 2 ? sanitize(args[1]) : IMPORT_WORLD);
        }

        if (args[0].equalsIgnoreCase("generate")) {
            return generateLegacyTestWorld(player, args.length >= 2 ? sanitize(args[1]) : "SkyKingsArenaV3");
        }

        player.sendMessage(ChatColor.YELLOW + "/skymap load [Weltname]");
        return true;
    }

    private boolean loadExistingWorld(Player player, String worldName) {
        if (worldName == null || worldName.length() < 1) worldName = IMPORT_WORLD;

        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!worldFolder.exists() || !new File(worldFolder, "level.dat").exists()) {
            player.sendMessage(ChatColor.RED + "Weltordner nicht gefunden: " + worldFolder.getAbsolutePath());
            player.sendMessage(ChatColor.GRAY + "Entpacke die Map so, dass dort " + worldName + File.separator + "level.dat liegt.");
            return true;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(World.Environment.NORMAL);
            // Vorhandene Region-Dateien bleiben erhalten; neue Chunks ausserhalb der Map bleiben leer.
            creator.generator(new VoidChunkGenerator());
            creator.generateStructures(false);
            world = creator.createWorld();
        }

        if (world == null) {
            player.sendMessage(ChatColor.RED + "Die Welt '" + worldName + "' konnte nicht geladen werden.");
            return true;
        }

        world.setKeepSpawnInMemory(true);
        world.setStorm(false);
        world.setThundering(false);
        world.setGameRuleValue("doMobSpawning", "false");

        Location entry;
        if (worldName.equalsIgnoreCase(IMPORT_WORLD)) {
            entry = new Location(world, IMPORT_X, IMPORT_Y, IMPORT_Z, 201.2F, 20.0F);
        } else {
            entry = world.getSpawnLocation().clone().add(0.5D, 1.0D, 0.5D);
        }

        player.teleport(entry);
        player.sendMessage(ChatColor.GREEN + "SkyPvP-Welt geladen: " + ChatColor.WHITE + worldName);
        player.sendMessage(ChatColor.YELLOW + "Du bist an der gespeicherten Map-Position. Wenn das der richtige Spawn ist: /setspawn");
        return true;
    }

    private boolean generateLegacyTestWorld(Player player, String worldName) {
        if (worldName == null || worldName.length() < 1) worldName = "SkyKingsArenaV3";
        if (Bukkit.getWorld(worldName) != null || new File(Bukkit.getWorldContainer(), worldName).exists()) {
            player.sendMessage(ChatColor.RED + "Die Welt '" + worldName + "' existiert bereits. Aus Sicherheitsgruenden wird sie nicht ueberschrieben.");
            return true;
        }

        player.sendMessage(ChatColor.DARK_GRAY + "Erstelle alte Test-Arena V3 in Void-Welt " + worldName + " ...");
        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.generator(new VoidChunkGenerator());
        creator.generateStructures(false);
        World world = creator.createWorld();
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Die Welt konnte nicht erstellt werden.");
            return true;
        }

        world.setKeepSpawnInMemory(true);
        new SkyKingsMapBuilderV3(plugin, world, spawnService, player).start();
        return true;
    }

    private String sanitize(String raw) {
        return raw == null ? "" : raw.replaceAll("[^A-Za-z0-9_\\-]", "");
    }
}
