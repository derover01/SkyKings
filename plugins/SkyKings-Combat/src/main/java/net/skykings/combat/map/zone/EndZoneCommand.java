package net.skykings.combat.map.zone;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class EndZoneCommand implements CommandExecutor {
    private final EndZoneService service;
    public EndZoneCommand(EndZoneService service) { this.service = service; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        if (!player.hasPermission("skykings.admin.map")) {
            player.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "/endzone set [Radius], /endzone remove, /endzone info");
            return true;
        }
        if ("set".equalsIgnoreCase(args[0])) {
            double radius = 12D;
            if (args.length >= 2) {
                try { radius = Double.parseDouble(args[1]); }
                catch (NumberFormatException ex) { player.sendMessage(ChatColor.RED + "Ungültiger Radius."); return true; }
            }
            radius = Math.max(2D, Math.min(50D, radius));
            service.set(player, radius);
            player.sendMessage(ChatColor.GREEN + "End Zone gesetzt. Radius: " + radius);
            return true;
        }
        if ("remove".equalsIgnoreCase(args[0])) {
            service.remove();
            player.sendMessage(ChatColor.YELLOW + "End Zone entfernt.");
            return true;
        }
        if ("info".equalsIgnoreCase(args[0])) {
            MapZone zone = service.getZone();
            if (zone == null) player.sendMessage(ChatColor.RED + "Keine End Zone gesetzt.");
            else player.sendMessage(ChatColor.LIGHT_PURPLE + "End Zone: " + ChatColor.WHITE + zone.getWorld()
                    + " @ " + Math.round(zone.getX()) + ", " + Math.round(zone.getY()) + ", " + Math.round(zone.getZ())
                    + " r=" + zone.getRadius());
            return true;
        }
        return true;
    }
}
