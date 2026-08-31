package net.skykings.combat.map.zone;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Zeigt Phase-6-Mastery fuer eigene oder fremde Online-Spieler. */
public final class MapMasteryCommand implements CommandExecutor {
    private final MapMasteryService mastery;

    public MapMasteryCommand(MapMasteryService mastery) { this.mastery = mastery; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Spieler nicht online.");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Nutze /mapmastery <Spieler>.");
                return true;
            }
            target = (Player) sender;
        }

        sender.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "MAP MASTERY " + ChatColor.YELLOW + target.getName());
        sender.sendMessage(ChatColor.GRAY + "Hot-Zone-Kills: " + ChatColor.WHITE + mastery.getHotZoneKills(target.getUniqueId()));
        sender.sendMessage(ChatColor.GRAY + "King-Altar-Captures: " + ChatColor.WHITE + mastery.getKingCaptures(target.getUniqueId()));
        sender.sendMessage(ChatColor.GRAY + "End-Zone-Kills: " + ChatColor.WHITE + mastery.getEndKills(target.getUniqueId()));
        sender.sendMessage(ChatColor.GRAY + "Secrets gefunden: " + ChatColor.WHITE + mastery.getSecrets(target.getUniqueId()));
        sender.sendMessage(ChatColor.GRAY + "Titel: " + ChatColor.AQUA + mastery.getTitle(target.getUniqueId()));
        return true;
    }
}
