package net.skykings.combat.map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/** Admin-Setup fuer spezielle Map-Inseln. */
public final class MapLandmarkCommand implements CommandExecutor {
    private final MapLandmarkService landmarks;

    public MapLandmarkCommand(MapLandmarkService landmarks) { this.landmarks = landmarks; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame verfügbar."); return true; }
        Player player = (Player) sender;
        if (!player.hasPermission("skykings.admin.map")) { player.sendMessage(ChatColor.RED + "Keine Berechtigung."); return true; }
        if (args.length < 1) { usage(player); return true; }

        if ("set".equalsIgnoreCase(args[0]) && args.length >= 2) {
            MapLandmarkService.Type type = MapLandmarkService.parse(args[1]);
            if (type == null) { player.sendMessage(ChatColor.RED + "Typ: gold, level, blacksmith oder merchant."); return true; }
            double radius = 8D;
            if (args.length >= 3) {
                try { radius = Double.parseDouble(args[2]); } catch (NumberFormatException ex) { player.sendMessage(ChatColor.RED + "Ungültiger Radius."); return true; }
            }
            if (radius < 2D || radius > 50D) { player.sendMessage(ChatColor.RED + "Radius muss zwischen 2 und 50 liegen."); return true; }
            landmarks.set(type, player, radius);
            player.sendMessage(ChatColor.GREEN + type.name() + " Island gesetzt. Radius: " + radius);
            return true;
        }

        if ("remove".equalsIgnoreCase(args[0]) && args.length >= 2) {
            MapLandmarkService.Type type = MapLandmarkService.parse(args[1]);
            if (type == null) { player.sendMessage(ChatColor.RED + "Unbekannter Typ."); return true; }
            player.sendMessage(landmarks.remove(type) ? ChatColor.GREEN + "Landmark entfernt." : ChatColor.RED + "Landmark nicht gesetzt.");
            return true;
        }

        if ("list".equalsIgnoreCase(args[0])) {
            Map<MapLandmarkService.Type, MapLandmarkService.Entry> list = landmarks.list();
            if (list.isEmpty()) { player.sendMessage(ChatColor.GRAY + "Keine Landmark-Zonen gesetzt."); return true; }
            player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "MAP LANDMARKS");
            for (Map.Entry<MapLandmarkService.Type, MapLandmarkService.Entry> e : list.entrySet()) {
                MapLandmarkService.Entry v = e.getValue();
                player.sendMessage(ChatColor.YELLOW + e.getKey().name() + ChatColor.GRAY + " • " + v.world
                        + " • " + Math.round(v.x) + ", " + Math.round(v.y) + ", " + Math.round(v.z)
                        + " • r=" + Math.round(v.radius));
            }
            return true;
        }

        usage(player);
        return true;
    }

    private void usage(Player player) {
        player.sendMessage(ChatColor.YELLOW + "/landmark set <gold|level|blacksmith|merchant> [Radius]");
        player.sendMessage(ChatColor.YELLOW + "/landmark remove <Typ>");
        player.sendMessage(ChatColor.YELLOW + "/landmark list");
    }
}
