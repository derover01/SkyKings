package net.skykings.admin.message;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/** Einheitliches, kompaktes Broadcast-Design fuer SkyKings-System- und Team-Ankuendigungen. */
public final class SkyKingsAnnouncement {

    private static final String LINE = ChatColor.DARK_GRAY.toString() + ChatColor.STRIKETHROUGH
            + "----------------------------------------";

    private SkyKingsAnnouncement() { }

    public static void broadcast(String category, String message) {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(LINE);
        Bukkit.broadcastMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "SKYKINGS"
                + ChatColor.DARK_GRAY + "  •  " + ChatColor.WHITE + ChatColor.BOLD + category.toUpperCase());
        Bukkit.broadcastMessage(ChatColor.GRAY + message);
        Bukkit.broadcastMessage(LINE);
        Bukkit.broadcastMessage("");
    }

    public static void staffFeedback(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.AQUA + "SkyKings" + ChatColor.DARK_GRAY + "] "
                + ChatColor.GRAY + message);
    }
}
