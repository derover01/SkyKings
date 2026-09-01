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
import java.util.Arrays;
import java.util.List;

/** Verwaltet die offiziellen SkyKings-Welten und die alte lokale Testwelt. */
public final class SkyMapCommand implements CommandExecutor {

    private static final String IMPORT_WORLD = "SkyPvP";
    private static final String COMMUNITY_EVENT_WORLD = "SkyCommunityEvent";
    private static final String PLOTS_WORLD = "SkyPlots";
    private static final String ISLANDS_WORLD = "SkyIslands";
    private static final double IMPORT_X = 21.88D;
    private static final double IMPORT_Y = 151.47D;
    private static final double IMPORT_Z = -83.51D;

    private static final List<String> OFFICIAL_WORLDS = Arrays.asList(
            IMPORT_WORLD, PLOTS_WORLD, ISLANDS_WORLD, COMMUNITY_EVENT_WORLD
    );

    private final JavaPlugin plugin;
    private final SpawnService spawnService;

    public SkyMapCommand(JavaPlugin plugin, SpawnService spawnService) {
        this.plugin = plugin;
        this.spawnService = spawnService;

        // Core laedt SkyPlots/SkyIslands bereits mit den jeweiligen Spezial-Generatoren.
        // Die handgebaute Hauptmap und Communitymap werden hier geladen, sobald ihre Ordner existieren.
        Bukkit.getScheduler().runTask(plugin, this::autoLoadOfficialWorlds);
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
            help(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            list(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("load")) {
            return loadExistingWorld(player, args.length >= 2 ? sanitize(args[1]) : IMPORT_WORLD);
        }
        if (args[0].equalsIgnoreCase("community") || args[0].equalsIgnoreCase("giveaway")) {
            return generateCommunityEventWorld(player, args.length >= 2 ? sanitize(args[1]) : COMMUNITY_EVENT_WORLD);
        }
        if (args[0].equalsIgnoreCase("generate")) {
            return generateLegacyTestWorld(player, args.length >= 2 ? sanitize(args[1]) : "SkyKingsArenaV3");
        }

        help(player);
        return true;
    }

    private void help(Player player) {
        player.sendMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "SKYKINGS MAPS");
        player.sendMessage(ChatColor.YELLOW + "/skymap list" + ChatColor.GRAY + " - alle offiziellen Maps + Ladezustand");
        player.sendMessage(ChatColor.YELLOW + "/skymap load [Weltname]" + ChatColor.GRAY + " - vorhandene Welt manuell laden");
        player.sendMessage(ChatColor.AQUA + "/skymap community [Weltname]" + ChatColor.GRAY + " - Community-/Giveaway-Map erstmalig erzeugen");
        player.sendMessage(ChatColor.DARK_GRAY + "/skymap generate [Weltname] - nur alte Test-Arena V3");
    }

    private void list(Player player) {
        player.sendMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "SKYKINGS MAP LIST");
        for (String worldName : OFFICIAL_WORLDS) {
            World loaded = Bukkit.getWorld(worldName);
            File folder = new File(Bukkit.getWorldContainer(), worldName);
            boolean exists = folder.exists() && new File(folder, "level.dat").exists();
            String state = loaded != null
                    ? ChatColor.GREEN + "GELADEN"
                    : exists ? ChatColor.YELLOW + "VORHANDEN / NICHT GELADEN" : ChatColor.RED + "FEHLT";
            player.sendMessage(ChatColor.WHITE + "• " + worldName + ChatColor.DARK_GRAY + " - " + state);
        }
        player.sendMessage(ChatColor.DARK_GRAY + "SkyEvents ist kein Bestandteil des Map-Systems mehr.");
    }

    private void autoLoadOfficialWorlds() {
        // SkyPlots und SkyIslands werden bereits beim Core-Start mit ihren Spezialgeneratoren geladen.
        autoLoadExistingVoidWorld(IMPORT_WORLD);
        autoLoadExistingVoidWorld(COMMUNITY_EVENT_WORLD);

        for (String name : OFFICIAL_WORLDS) {
            if (Bukkit.getWorld(name) != null) plugin.getLogger().info("Map auto-geladen: " + name);
            else {
                File folder = new File(Bukkit.getWorldContainer(), name);
                if (folder.exists()) plugin.getLogger().warning("Map-Ordner vorhanden, aber Welt nicht geladen: " + name);
                else plugin.getLogger().info("Map noch nicht vorhanden: " + name);
            }
        }
    }

    private boolean autoLoadExistingVoidWorld(String worldName) {
        if (Bukkit.getWorld(worldName) != null) return true;
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!worldFolder.exists() || !new File(worldFolder, "level.dat").exists()) return false;

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.generator(new VoidChunkGenerator());
        creator.generateStructures(false);
        World world = creator.createWorld();
        if (world == null) {
            plugin.getLogger().warning("Map konnte beim Auto-Load nicht geladen werden: " + worldName);
            return false;
        }
        configureLoadedWorld(world);
        return true;
    }

    private boolean generateCommunityEventWorld(Player player, String worldName) {
        if (worldName == null || worldName.length() < 1) worldName = COMMUNITY_EVENT_WORLD;
        if (Bukkit.getWorld(worldName) != null || new File(Bukkit.getWorldContainer(), worldName).exists()) {
            player.sendMessage(ChatColor.RED + "Die Community-Eventwelt '" + worldName + "' existiert bereits.");
            player.sendMessage(ChatColor.GRAY + "Vorhandene Maps werden beim Serverstart automatisch geladen.");
            return true;
        }

        player.sendMessage(ChatColor.AQUA + "Erzeuge kompakte SkyKings Community-Eventmap in " + ChatColor.WHITE + worldName + ChatColor.GRAY + " ...");
        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.generator(new VoidChunkGenerator());
        creator.generateStructures(false);
        World world = creator.createWorld();
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Die Community-Eventwelt konnte nicht erstellt werden.");
            return true;
        }
        configureLoadedWorld(world);
        new SkyKingsCommunityEventMapBuilder(world, player).build();
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
            creator.generator(new VoidChunkGenerator());
            creator.generateStructures(false);
            world = creator.createWorld();
        }

        if (world == null) {
            player.sendMessage(ChatColor.RED + "Die Welt '" + worldName + "' konnte nicht geladen werden.");
            return true;
        }

        configureLoadedWorld(world);
        Location entry;
        if (worldName.equalsIgnoreCase(IMPORT_WORLD)) {
            entry = new Location(world, IMPORT_X, IMPORT_Y, IMPORT_Z, 201.2F, 20.0F);
        } else {
            entry = world.getSpawnLocation().clone().add(0.5D, 1.0D, 0.5D);
        }

        player.teleport(entry);
        player.sendMessage(ChatColor.GREEN + "SkyKings-Welt geladen: " + ChatColor.WHITE + worldName);
        if (worldName.equalsIgnoreCase(IMPORT_WORLD)) player.sendMessage(ChatColor.YELLOW + "Wenn das der richtige globale Spawn ist: /setspawn");
        return true;
    }

    private void configureLoadedWorld(World world) {
        world.setKeepSpawnInMemory(true);
        world.setStorm(false);
        world.setThundering(false);
        world.setGameRuleValue("doMobSpawning", "false");
    }

    private boolean generateLegacyTestWorld(Player player, String worldName) {
        if (worldName == null || worldName.length() < 1) worldName = "SkyKingsArenaV3";
        if (Bukkit.getWorld(worldName) != null || new File(Bukkit.getWorldContainer(), worldName).exists()) {
            player.sendMessage(ChatColor.RED + "Die Welt '" + worldName + "' existiert bereits. Sie wird nicht ueberschrieben.");
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
