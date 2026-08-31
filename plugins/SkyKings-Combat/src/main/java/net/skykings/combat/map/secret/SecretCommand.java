package net.skykings.combat.map.secret;

import net.skykings.combat.map.zone.MapZone;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class SecretCommand implements CommandExecutor {
    private final SecretDiscoveryService service;
    public SecretCommand(SecretDiscoveryService service) { this.service = service; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        if (!player.hasPermission("skykings.admin.map")) {
            player.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "/secret add <Name> [Radius]");
            player.sendMessage(ChatColor.YELLOW + "/secret remove <Name>");
            player.sendMessage(ChatColor.YELLOW + "/secret list");
            return true;
        }
        if ("add".equalsIgnoreCase(args[0]) && args.length >= 2) {
            double radius = 2D;
            if (args.length >= 3) {
                try { radius = Double.parseDouble(args[2]); }
                catch (NumberFormatException ex) { player.sendMessage(ChatColor.RED + "Ungültiger Radius."); return true; }
            }
            radius = Math.max(1D, Math.min(10D, radius));
            service.add(args[1], player, radius);
            player.sendMessage(ChatColor.GREEN + "Secret '" + args[1] + "' gesetzt. Radius: " + radius);
            return true;
        }
        if ("remove".equalsIgnoreCase(args[0]) && args.length >= 2) {
            player.sendMessage(service.remove(args[1]) ? ChatColor.YELLOW + "Secret entfernt." : ChatColor.RED + "Secret nicht gefunden.");
            return true;
        }
        if ("list".equalsIgnoreCase(args[0])) {
            Map<String, MapZone> secrets = service.getSecrets();
            player.sendMessage(ChatColor.AQUA + "Secrets: " + ChatColor.WHITE + secrets.size());
            for (Map.Entry<String, MapZone> entry : secrets.entrySet()) {
                MapZone zone = entry.getValue();
                player.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + entry.getKey() + ChatColor.DARK_GRAY
                        + " @ " + zone.getWorld() + " " + Math.round(zone.getX()) + "," + Math.round(zone.getY()) + "," + Math.round(zone.getZ()));
            }
            return true;
        }
        return true;
    }
}
