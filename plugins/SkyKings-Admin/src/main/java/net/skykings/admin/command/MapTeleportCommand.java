package net.skykings.admin.command;

import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Staff-Schnellreise zu den technischen SkyKings-Welten. */
public final class MapTeleportCommand implements CommandExecutor, TabCompleter {
    public static final String PERMISSION = "skykings.admin.maptp";
    private static final List<String> ALIASES = Arrays.asList("main", "plots", "islands", "community");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfügbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(UiTheme.DANGER + "Dafür hast du keine Berechtigung.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(UiTheme.WARNING + "Nutze: /maptp <main|plots|islands|community>");
            return true;
        }

        String alias = args[0].toLowerCase();
        String worldName = worldName(alias);
        if (worldName == null) {
            player.sendMessage(UiTheme.DANGER + "Unbekannte Map: " + ChatColor.WHITE + args[0]);
            SoundFeedback.error(player);
            return true;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            if ("main".equals(alias) || "community".equals(alias)) {
                player.sendMessage(UiTheme.WARNING + "Map wird geladen …");
                player.performCommand("skymap load " + worldName);
                return true;
            }
            player.sendMessage(UiTheme.DANGER + "Die Welt " + ChatColor.WHITE + worldName + UiTheme.DANGER + " ist nicht geladen.");
            SoundFeedback.error(player);
            return true;
        }

        Location target = world.getSpawnLocation().clone().add(0.5D, 1.0D, 0.5D);
        player.teleport(target);
        player.sendMessage(UiTheme.SUCCESS + "Map: " + ChatColor.WHITE + worldName);
        SoundFeedback.success(player);
        return true;
    }

    private String worldName(String alias) {
        if ("main".equals(alias)) return "SkyPvP";
        if ("plots".equals(alias)) return "SkyPlots";
        if ("islands".equals(alias)) return "SkyIslands";
        if ("community".equals(alias)) return "SkyCommunityEvent";
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION) || args.length != 1) return Collections.emptyList();
        String prefix = args[0].toLowerCase();
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        for (String value : ALIASES) if (value.startsWith(prefix)) out.add(value);
        return out;
    }
}
