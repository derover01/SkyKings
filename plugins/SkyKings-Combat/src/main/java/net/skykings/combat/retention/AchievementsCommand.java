package net.skykings.combat.retention;

import net.skykings.combat.map.zone.MapMasteryService;
import net.skykings.combat.stats.PvpStatsService;
import net.skykings.core.pvp.PvpStatsSnapshot;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Achievement-Book auf Basis echter PvP-/Map-Ziele. */
public final class AchievementsCommand implements CommandExecutor {
    private final PvpStatsService stats;
    private final MapMasteryService mastery;

    public AchievementsCommand(PvpStatsService stats, MapMasteryService mastery) {
        this.stats = stats;
        this.mastery = mastery;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        PvpStatsSnapshot s = stats.getStats(player.getUniqueId());
        player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "ACHIEVEMENTS");
        line(player, "First Blood", s.getKills() >= 1, "Erster PvP-Kill");
        line(player, "Unstoppable", s.getBestStreak() >= 10, "10er Killstreak");
        line(player, "Untouchable", s.getBestStreak() >= 25, "25er Killstreak");
        line(player, "Sky Legend", s.getBestStreak() >= 50, "50er Killstreak");
        line(player, "King Slayer", mastery.getKingCaptures(player.getUniqueId()) >= 10, "10 King-Altar Captures");
        line(player, "The Hunter", mastery.getHotZoneKills(player.getUniqueId()) >= 25, "25 Hot-Zone-Kills");
        line(player, "End Raider", mastery.getEndKills(player.getUniqueId()) >= 10, "10 End-Zone-Kills");
        line(player, "Explorer", mastery.getSecrets(player.getUniqueId()) >= 5, "5 Secrets finden");
        return true;
    }

    private void line(Player player, String name, boolean unlocked, String condition) {
        player.sendMessage((unlocked ? ChatColor.GREEN + "✔ " : ChatColor.DARK_GRAY + "✖ ")
                + (unlocked ? ChatColor.WHITE : ChatColor.GRAY) + name + ChatColor.DARK_GRAY + " - " + condition);
    }
}
