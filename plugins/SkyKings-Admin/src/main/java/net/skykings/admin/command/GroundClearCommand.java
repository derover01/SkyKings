package net.skykings.admin.command;

import net.skykings.admin.cleanup.GroundClearService;
import net.skykings.admin.message.SkyKingsAnnouncement;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** /clear startet denselben professionellen 2-Minuten-Boden-Clear-Countdown wie der Auto-Clear. */
public final class GroundClearCommand implements CommandExecutor {

    public static final String PERMISSION = "skykings.staff.clear";

    private final GroundClearService groundClearService;

    public GroundClearCommand(GroundClearService groundClearService) {
        this.groundClearService = groundClearService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "Dafuer hast du keine Berechtigung.");
            return true;
        }
        if (args.length > 0) {
            SkyKingsAnnouncement.staffFeedback(sender, "Verwendung: /" + label);
            return true;
        }
        if (!groundClearService.startManualCountdown()) {
            SkyKingsAnnouncement.staffFeedback(sender, "Es laeuft bereits ein Boden-Clear-Countdown.");
            return true;
        }
        SkyKingsAnnouncement.staffFeedback(sender, "Boden-Clear-Countdown gestartet.");
        return true;
    }
}
