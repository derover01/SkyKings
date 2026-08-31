package net.skykings.combat.map.route;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/** Admin-Setup fuer Jump-/Pearl-Routen. */
public final class MapRouteCommand implements CommandExecutor {
    private final MapRouteService routes;

    public MapRouteCommand(MapRouteService routes) { this.routes = routes; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame verfügbar."); return true; }
        Player player = (Player) sender;
        if (!player.hasPermission("skykings.admin.map")) { player.sendMessage(ChatColor.RED + "Keine Berechtigung."); return true; }
        if (args.length < 1) { usage(player); return true; }
        if ("create".equalsIgnoreCase(args[0]) && args.length >= 2) {
            player.sendMessage(routes.create(args[1]) ? ChatColor.GREEN + "Route erstellt." : ChatColor.RED + "Route existiert bereits/Name ungültig.");
            return true;
        }
        if ("addpoint".equalsIgnoreCase(args[0]) && args.length >= 2) {
            player.sendMessage(routes.addPoint(args[1], player) ? ChatColor.GREEN + "Checkpoint hinzugefügt." : ChatColor.RED + "Route nicht gefunden.");
            return true;
        }
        if ("remove".equalsIgnoreCase(args[0]) && args.length >= 2) {
            player.sendMessage(routes.remove(args[1]) ? ChatColor.GREEN + "Route entfernt." : ChatColor.RED + "Route nicht gefunden.");
            return true;
        }
        if ("list".equalsIgnoreCase(args[0])) {
            Map<String, Integer> list = routes.list();
            if (list.isEmpty()) { player.sendMessage(ChatColor.GRAY + "Keine Routen gesetzt."); return true; }
            player.sendMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "MAP ROUTES");
            for (Map.Entry<String, Integer> e : list.entrySet()) player.sendMessage(ChatColor.YELLOW + e.getKey() + ChatColor.GRAY + " • " + e.getValue() + " Checkpoints");
            return true;
        }
        usage(player);
        return true;
    }

    private void usage(Player player) {
        player.sendMessage(ChatColor.YELLOW + "/route create <Name>");
        player.sendMessage(ChatColor.YELLOW + "/route addpoint <Name>" + ChatColor.GRAY + " • aktuelle Position");
        player.sendMessage(ChatColor.YELLOW + "/route remove <Name>");
        player.sendMessage(ChatColor.YELLOW + "/route list");
    }
}
