package net.skykings.admin.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /clearchat bzw. /cc leert den sichtbaren Chat für alle Online-Spieler. */
public final class ClearChatCommand implements CommandExecutor {

    public static final String PERMISSION = "skykings.staff.clearchat";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "Dafür hast du keine Berechtigung.");
            return true;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 100; i++) player.sendMessage("");
            player.sendMessage(ChatColor.DARK_GRAY + "» " + ChatColor.GOLD.toString() + ChatColor.BOLD + "SkyKings"
                    + ChatColor.DARK_GRAY + " | " + ChatColor.YELLOW + "Der Chat wurde von "
                    + ChatColor.WHITE + sender.getName() + ChatColor.YELLOW + " geleert.");
        }
        return true;
    }
}
