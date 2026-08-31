package net.skykings.combat.map.zone;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /kingaltar set <radius>, remove, info. */
public final class KingAltarCommand implements CommandExecutor {
    private final KingAltarService service;

    public KingAltarCommand(KingAltarService service) { this.service = service; }

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
        if ("set".equalsIgnoreCase(args[0])) {
            double radius = 8D;
            if (args.length >= 2) {
                try { radius = Double.parseDouble(args[1]); }
                catch (NumberFormatException ex) {
                    player.sendMessage(ChatColor.RED + "Radius muss eine Zahl sein.");
                    return true;
                }
            }
            if (radius < 2D || radius > 50D) {
                player.sendMessage(ChatColor.RED + "Radius muss zwischen 2 und 50 liegen.");
                return true;
            }
            service.setZone(player, radius);
            player.sendMessage(ChatColor.GREEN + "King Altar gesetzt. Radius: " + ChatColor.YELLOW + radius);
            return true;
        }
        if ("remove".equalsIgnoreCase(args[0])) {
            service.removeZone();
            player.sendMessage(ChatColor.YELLOW + "King Altar entfernt.");
            return true;
        }
        if ("info".equalsIgnoreCase(args[0])) {
            MapZone zone = service.getZone();
            if (zone == null) {
                player.sendMessage(ChatColor.RED + "Kein King Altar gesetzt.");
                return true;
            }
            player.sendMessage(ChatColor.GOLD + "King Altar: " + ChatColor.GRAY + zone.getWorld() + " @ "
                    + Math.round(zone.getX()) + ", " + Math.round(zone.getY()) + ", " + Math.round(zone.getZ())
                    + " | Radius " + zone.getRadius());
            player.sendMessage(ChatColor.GRAY + "Cooldown: " + service.getCooldown() + "s | Fortschritt: " + service.getProgress() + "/60");
            return true;
        }
        usage(player);
        return true;
    }

    private void usage(Player player) {
        player.sendMessage(ChatColor.GOLD + "/kingaltar set [Radius]" + ChatColor.GRAY + " - hier setzen");
        player.sendMessage(ChatColor.GOLD + "/kingaltar remove");
        player.sendMessage(ChatColor.GOLD + "/kingaltar info");
    }
}
