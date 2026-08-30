package net.skykings.combat.map.builder;

import net.skykings.combat.spawn.SpawnService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Erstellt die prozedurale SkyKings-Arena in einer neuen Void-Welt. */
public final class SkyMapCommand implements CommandExecutor {

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
        if (args.length == 0 || !args[0].equalsIgnoreCase("generate")) {
            player.sendMessage(ChatColor.YELLOW + "/skymap generate [Weltname]");
            return true;
        }

        String worldName = args.length >= 2 ? sanitize(args[1]) : "SkyKingsArena";
        if (worldName.length() < 1) worldName = "SkyKingsArena";
        if (Bukkit.getWorld(worldName) != null || new java.io.File(Bukkit.getWorldContainer(), worldName).exists()) {
            player.sendMessage(ChatColor.RED + "Die Welt '" + worldName + "' existiert bereits. Aus Sicherheitsgruenden wird sie nicht ueberschrieben.");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "Erstelle Void-Welt " + ChatColor.WHITE + worldName + ChatColor.GRAY + " ...");
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
        new SkyKingsMapBuilder(plugin, world, spawnService, player).start();
        return true;
    }

    private String sanitize(String raw) {
        return raw == null ? "" : raw.replaceAll("[^A-Za-z0-9_\\-]", "");
    }
}
