package net.skykings.combat.map.zone;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/** /hotzone add <name> [radius], remove <name>, list. */
public final class HotZoneCommand implements CommandExecutor {
    private final HotZoneService service;

    public HotZoneCommand(HotZoneService service) { this.service = service; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur für Spieler verfügbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("skykings.admin.map")) {
            player.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            usage(player);
            return true;
        }
        if ("add".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Nutze /hotzone add <Name> [Radius].");
                return true;
            }
            double radius = 10D;
            if (args.length >= 3) {
                try { radius = Double.parseDouble(args[2]); }
                catch (NumberFormatException ex) {
                    player.sendMessage(ChatColor.RED + "Radius muss eine Zahl sein.");
                    return true;
                }
            }
            if (radius < 2D || radius > 75D) {
                player.sendMessage(ChatColor.RED + "Radius muss zwischen 2 und 75 liegen.");
                return true;
            }
            service.add(args[1], player, radius);
            player.sendMessage(ChatColor.GREEN + "Hot Zone " + ChatColor.YELLOW + args[1] + ChatColor.GREEN
                    + " wurde hier gesetzt. Radius: " + radius);
            return true;
        }
        if ("remove".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Nutze /hotzone remove <Name>.");
                return true;
            }
            if (!service.remove(args[1])) player.sendMessage(ChatColor.RED + "Hot Zone nicht gefunden.");
            else player.sendMessage(ChatColor.YELLOW + "Hot Zone entfernt.");
            return true;
        }
        if ("list".equalsIgnoreCase(args[0])) {
            Map<String, MapZone> zones = service.getZones();
            player.sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "Hot Zones " + ChatColor.GRAY + "(" + zones.size() + ")");
            if (zones.isEmpty()) player.sendMessage(ChatColor.GRAY + "Noch keine Hot Zones gesetzt.");
            for (Map.Entry<String, MapZone> entry : zones.entrySet()) {
                MapZone z = entry.getValue();
                player.sendMessage(ChatColor.YELLOW + entry.getKey() + ChatColor.GRAY + " - " + z.getWorld() + " @ "
                        + Math.round(z.getX()) + ", " + Math.round(z.getY()) + ", " + Math.round(z.getZ())
                        + " | r=" + z.getRadius());
            }
            return true;
        }
        usage(player);
        return true;
    }

    private void usage(Player player) {
        player.sendMessage(ChatColor.RED + "/hotzone add <Name> [Radius]");
        player.sendMessage(ChatColor.RED + "/hotzone remove <Name>");
        player.sendMessage(ChatColor.RED + "/hotzone list");
    }
}
