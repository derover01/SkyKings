package net.skykings.combat.event;

import net.skykings.core.ui.UiTheme;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Spieler-Queue + Staff-Steuerung fuer das SkyKings Tournament. */
public final class TournamentCommand implements CommandExecutor {
    private final TournamentService service;

    public TournamentCommand(TournamentService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(java.util.Locale.ROOT);

        if ("status".equals(sub)) {
            sender.sendMessage(UiTheme.PRIMARY + "TOURNAMENT" + UiTheme.MUTED
                    + " • " + (service.isRunning() ? "RUNDE " + service.getRoundNumber() : "Queue " + service.queueSize() + " Spieler"));
            sender.sendMessage(UiTheme.MUTED + "Spieler: /tournament join | leave");
            return true;
        }

        if ("start".equals(sub) || "stop".equals(sub)) {
            if (!sender.hasPermission("skykings.admin.event")) {
                sender.sendMessage(UiTheme.DANGER + "Keine Berechtigung.");
                return true;
            }
            if ("start".equals(sub)) {
                if (!service.start()) {
                    sender.sendMessage(UiTheme.DANGER + "Tournament konnte nicht starten."
                            + UiTheme.MUTED + " Mindestens 4 Spieler + Arena-Punkte a/b/lobby oder spectator erforderlich.");
                } else sender.sendMessage(UiTheme.SUCCESS + "Tournament gestartet.");
            } else {
                service.stop(true);
                sender.sendMessage(UiTheme.SUCCESS + "Tournament beendet.");
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Nur Spieler koennen der Tournament Queue beitreten.");
            return true;
        }
        Player player = (Player) sender;
        if ("join".equals(sub)) {
            if (!service.join(player)) player.sendMessage(UiTheme.DANGER + "Du kannst der Queue gerade nicht beitreten.");
            return true;
        }
        if ("leave".equals(sub) || "quit".equals(sub)) {
            if (!service.leave(player)) player.sendMessage(UiTheme.DANGER + "Du kannst die Queue gerade nicht verlassen.");
            return true;
        }

        player.sendMessage(UiTheme.WARNING + "/tournament <join|leave|status>" + UiTheme.MUTED + " • Staff: start|stop");
        return true;
    }
}
