package net.skykings.combat.retention;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /season und /pvplevel koennen denselben Executor nutzen. */
public final class SeasonCommand implements CommandExecutor {
    private final SeasonProgressService progress;

    public SeasonCommand(SeasonProgressService progress) { this.progress = progress; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        int level = progress.getLevel(player.getUniqueId());
        int xp = progress.getXp(player.getUniqueId());
        player.sendMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "SEASON " + progress.getSeason());
        player.sendMessage(ChatColor.GRAY + "PvP-Level: " + ChatColor.WHITE + level + "/100");
        player.sendMessage(ChatColor.GRAY + "Season-XP: " + ChatColor.WHITE + xp);
        if (level < 100) player.sendMessage(ChatColor.GRAY + "Bis Level " + (level + 1) + ": " + ChatColor.YELLOW + progress.xpToNext(player.getUniqueId()) + " XP");
        else player.sendMessage(ChatColor.GOLD + "MAX LEVEL erreicht.");
        return true;
    }
}
