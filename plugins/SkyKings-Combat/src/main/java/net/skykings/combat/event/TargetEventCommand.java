package net.skykings.combat.event;

import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Staff startet/stoppt Most Wanted; Spieler koennen Status ansehen. */
public final class TargetEventCommand implements CommandExecutor {
    private final TargetEventService service;
    public TargetEventCommand(TargetEventService service) { this.service = service; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || "status".equalsIgnoreCase(args[0])) {
            if (!service.isActive()) sender.sendMessage(UiTheme.MUTED + "Most Wanted ist aktuell nicht aktiv.");
            else {
                Player target = Bukkit.getPlayer(service.getTarget());
                sender.sendMessage(UiTheme.TEXT + "Most Wanted");
                sender.sendMessage(UiTheme.MUTED + "Target " + UiTheme.TEXT + (target == null ? "offline" : target.getName()));
                sender.sendMessage(UiTheme.MUTED + "Zeit " + UiTheme.WARNING + UiFormat.durationSeconds(service.getRemaining()));
            }
            return true;
        }
        if (!sender.hasPermission("skykings.admin.targetevent")) {
            sender.sendMessage(UiTheme.DANGER + "Keine Berechtigung."); return true;
        }
        if ("start".equalsIgnoreCase(args[0])) {
            if (service.isActive()) { sender.sendMessage(UiTheme.DANGER + "Most Wanted laeuft bereits."); return true; }
            Player chosen = null;
            if (args.length >= 2) {
                chosen = Bukkit.getPlayer(args[1]);
                if (chosen == null || !service.start(chosen)) {
                    sender.sendMessage(UiTheme.DANGER + "Spieler muss online und in einer PvP-Region sein.");
                    return true;
                }
            } else {
                chosen = service.startRandom();
                if (chosen == null) {
                    sender.sendMessage(UiTheme.DANGER + "Kein geeigneter Spieler befindet sich in einer PvP-Region.");
                    return true;
                }
            }
            sender.sendMessage(UiTheme.SUCCESS + "Most Wanted gestartet: " + chosen.getName());
            return true;
        }
        if ("stop".equalsIgnoreCase(args[0])) {
            service.stop(true);
            sender.sendMessage(UiTheme.SUCCESS + "Most Wanted beendet.");
            return true;
        }
        sender.sendMessage(UiTheme.TEXT + "Most Wanted");
        sender.sendMessage(UiTheme.WARNING + "/targetevent start [Spieler]");
        sender.sendMessage(UiTheme.WARNING + "/targetevent stop | status");
        return true;
    }
}
