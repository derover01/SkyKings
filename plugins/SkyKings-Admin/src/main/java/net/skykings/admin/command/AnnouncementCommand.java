package net.skykings.admin.command;

import net.skykings.admin.message.SkyKingsAnnouncement;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** /announcement <Text> sendet eine einheitliche serverweite SkyKings-Ankuendigung. */
public final class AnnouncementCommand implements CommandExecutor {

    public static final String PERMISSION = "skykings.staff.announcement";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "Dafuer hast du keine Berechtigung.");
            return true;
        }
        if (args.length == 0) {
            SkyKingsAnnouncement.staffFeedback(sender, "Verwendung: /" + label + " <Nachricht>");
            return true;
        }

        StringBuilder text = new StringBuilder();
        for (String arg : args) {
            if (text.length() > 0) text.append(' ');
            text.append(arg);
        }
        String message = ChatColor.translateAlternateColorCodes('&', text.toString());
        SkyKingsAnnouncement.broadcast("Ankuendigung", ChatColor.WHITE + message);
        return true;
    }
}
