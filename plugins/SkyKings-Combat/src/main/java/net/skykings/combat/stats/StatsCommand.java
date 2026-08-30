package net.skykings.combat.stats;

import net.skykings.core.pvp.PvpStatsSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** /stats [Spieler] zeigt die persistenten SkyKings-PvP-Stats. */
public final class StatsCommand implements CommandExecutor {

    private final PvpStatsTracker stats;

    public StatsCommand(PvpStatsTracker stats) {
        this.stats = stats;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Verwendung: /stats <Spieler>");
                return true;
            }
            target = (Player) sender;
        } else if (args.length == 1) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Der Spieler muss aktuell online sein.");
                return true;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Verwendung: /stats [Spieler]");
            return true;
        }

        PvpStatsSnapshot s = stats.getStats(target.getUniqueId());
        sender.sendMessage(ChatColor.DARK_GRAY + "──────── " + ChatColor.GOLD + ChatColor.BOLD + "SKYKINGS STATS" + ChatColor.DARK_GRAY + " ────────");
        sender.sendMessage(ChatColor.GRAY + "Spieler: " + ChatColor.WHITE + target.getName());
        sender.sendMessage(ChatColor.GRAY + "Kills: " + ChatColor.GREEN + s.getKills()
                + ChatColor.DARK_GRAY + "  |  " + ChatColor.GRAY + "Tode: " + ChatColor.RED + s.getDeaths());
        sender.sendMessage(ChatColor.GRAY + "K/D: " + ChatColor.AQUA + String.format(Locale.US, "%.2f", s.getKd()));
        sender.sendMessage(ChatColor.GRAY + "Killstreak: " + ChatColor.GOLD + s.getCurrentStreak()
                + ChatColor.DARK_GRAY + "  |  " + ChatColor.GRAY + "Beststreak: " + ChatColor.YELLOW + s.getBestStreak());
        sender.sendMessage(ChatColor.DARK_GRAY + "────────────────────────");
        return true;
    }
}
