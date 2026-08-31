package net.skykings.combat.event;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/** Staff-Command zum Einrichten von Event-Arenen ohne feste Koordinaten im Code. */
public final class EventArenaCommand implements CommandExecutor {
    private final EventArenaService arenas;

    public EventArenaCommand(EventArenaService arenas) { this.arenas = arenas; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("skykings.admin.event")) {
            player.sendMessage(ChatColor.RED + "Keine Berechtigung.");
            return true;
        }
        if (args.length == 0) { usage(player); return true; }
        String sub = args[0].toLowerCase(java.util.Locale.ROOT);

        if ("set".equals(sub)) {
            if (args.length < 3) { usage(player); return true; }
            arenas.set(args[1], args[2], player.getLocation());
            player.sendMessage(ChatColor.GREEN + "Arena-Punkt gesetzt: " + ChatColor.YELLOW + args[1] + ChatColor.GRAY + " / " + ChatColor.WHITE + args[2]);
            return true;
        }
        if ("list".equals(sub)) {
            Map<String, Integer> list = arenas.list();
            player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "EVENT ARENAS");
            if (list.isEmpty()) player.sendMessage(ChatColor.GRAY + "Noch keine Arenen eingerichtet.");
            for (Map.Entry<String, Integer> entry : list.entrySet()) {
                player.sendMessage(ChatColor.YELLOW + entry.getKey() + ChatColor.GRAY + " - " + entry.getValue() + " Punkt(e)"
                        + (arenas.isReadyForDuel(entry.getKey()) ? ChatColor.GREEN + " [DUEL READY]" : "")
                        + (arenas.isReadyForLms(entry.getKey()) ? ChatColor.AQUA + " [LMS READY]" : ""));
            }
            return true;
        }
        if ("info".equals(sub)) {
            if (args.length < 2) { usage(player); return true; }
            Map<String, String> points = arenas.points(args[1]);
            player.sendMessage(ChatColor.GOLD + "Arena " + ChatColor.YELLOW + args[1]);
            if (points.isEmpty()) player.sendMessage(ChatColor.GRAY + "Nicht eingerichtet.");
            for (Map.Entry<String, String> entry : points.entrySet()) player.sendMessage(ChatColor.YELLOW + entry.getKey() + ChatColor.GRAY + " -> " + entry.getValue());
            return true;
        }
        if ("remove".equals(sub)) {
            if (args.length < 2) { usage(player); return true; }
            boolean removed;
            if (args.length >= 3) removed = arenas.removePoint(args[1], args[2]);
            else removed = arenas.removeArena(args[1]);
            player.sendMessage(removed ? ChatColor.YELLOW + "Arena-Eintrag entfernt." : ChatColor.RED + "Nichts gefunden.");
            return true;
        }

        usage(player);
        return true;
    }

    private void usage(Player player) {
        player.sendMessage(ChatColor.GOLD + "Event-Arena Setup");
        player.sendMessage(ChatColor.YELLOW + "/eventarena set <Arena> <Punkt>" + ChatColor.GRAY + " - aktuelle Position");
        player.sendMessage(ChatColor.YELLOW + "/eventarena info <Arena>");
        player.sendMessage(ChatColor.YELLOW + "/eventarena list");
        player.sendMessage(ChatColor.YELLOW + "/eventarena remove <Arena> [Punkt]");
        player.sendMessage(ChatColor.DARK_GRAY + "Duel: Punkte a + b. LMS: lobby + spawn1..spawnN (mind. 4). Optional: spectator.");
    }
}
