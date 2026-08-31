package net.skykings.combat.map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Admin-Command fuer dynamische Map-Hologramme. */
public final class MapDisplayCommand implements CommandExecutor {
    private final MapDisplayService service;

    public MapDisplayCommand(MapDisplayService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("skykings.admin.map")) {
            player.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "/mapdisplay set <topkills|king|hotzones>");
            player.sendMessage(ChatColor.YELLOW + "/mapdisplay remove <topkills|king|hotzones>");
            player.sendMessage(ChatColor.YELLOW + "/mapdisplay list");
            return true;
        }
        if ("list".equalsIgnoreCase(args[0])) {
            player.sendMessage(ChatColor.GOLD + "Map-Displays: " + ChatColor.WHITE + service.list().keySet());
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Display-Typ fehlt.");
            return true;
        }
        MapDisplayService.Type type = MapDisplayService.parse(args[1]);
        if (type == null) {
            player.sendMessage(ChatColor.RED + "Typ: topkills, king oder hotzones.");
            return true;
        }
        if ("set".equalsIgnoreCase(args[0])) {
            service.set(type, player);
            player.sendMessage(ChatColor.GREEN + "Map-Display " + type.name() + " gesetzt.");
            return true;
        }
        if ("remove".equalsIgnoreCase(args[0]) || "delete".equalsIgnoreCase(args[0])) {
            player.sendMessage(service.remove(type) ? ChatColor.YELLOW + "Map-Display entfernt." : ChatColor.RED + "Display nicht gesetzt.");
            return true;
        }
        return true;
    }
}
