package net.skykings.admin.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** /announcement <Text> sendet eine auffällige serverweite Team-Ankündigung. */
public final class AnnouncementCommand implements CommandExecutor {

    public static final String PERMISSION = "skykings.staff.announcement";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "Dafür hast du keine Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /announcement <Nachricht>");
            return true;
        }
        StringBuilder text = new StringBuilder();
        for (String arg : args) {
            if (text.length() > 0) text.append(' ');
            text.append(arg);
        }
        String message = ChatColor.translateAlternateColorCodes('&', text.toString());
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "» " + ChatColor.GOLD.toString() + ChatColor.BOLD + "SKYKINGS ANKÜNDIGUNG" + ChatColor.DARK_GRAY + " «");
        Bukkit.broadcastMessage(ChatColor.WHITE + message);
        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "────────────────────────────");
        Bukkit.broadcastMessage("");
        return true;
    }
}
