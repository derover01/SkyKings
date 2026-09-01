package net.skykings.combat.event;

import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Spieler-Queue und Staff-Steuerung fuer Juggernaut. */
public final class JuggernautCommand implements CommandExecutor {
    private final JuggernautService service;

    public JuggernautCommand(JuggernautService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(java.util.Locale.ROOT);

        if ("status".equals(sub)) {
            if (service.isRunning()) {
                Player boss = service.getBossId() == null ? null : Bukkit.getPlayer(service.getBossId());
                sender.sendMessage(UiTheme.LEGENDARY + "JUGGERNAUT" + UiTheme.MUTED + " • Boss "
                        + UiTheme.TEXT + (boss == null ? "?" : boss.getName())
                        + UiTheme.MUTED + " • Angreifer " + UiTheme.TEXT + service.attackersAlive());
            } else {
                sender.sendMessage(UiTheme.LEGENDARY + "JUGGERNAUT" + UiTheme.MUTED + " • Queue "
                        + UiTheme.TEXT + service.queueSize() + UiTheme.MUTED + " Spieler");
            }
            sender.sendMessage(UiTheme.MUTED + "Spieler: /juggernaut join | leave");
            return true;
        }

        if ("start".equals(sub) || "stop".equals(sub)) {
            if (!sender.hasPermission("skykings.admin.event")) {
                sender.sendMessage(UiTheme.DANGER + "Keine Berechtigung.");
                return true;
            }
            if ("start".equals(sub)) {
                if (!service.start()) {
                    sender.sendMessage(UiTheme.DANGER + "Juggernaut konnte nicht starten."
                            + UiTheme.MUTED + " Mindestens 3 Spieler + boss/lobby + 2 Spawnpunkte erforderlich.");
                } else sender.sendMessage(UiTheme.SUCCESS + "Juggernaut gestartet.");
            } else {
                service.stop(true);
                sender.sendMessage(UiTheme.SUCCESS + "Juggernaut beendet.");
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Nur Spieler koennen der Juggernaut Queue beitreten.");
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

        player.sendMessage(UiTheme.WARNING + "/juggernaut <join|leave|status>" + UiTheme.MUTED + " • Staff: start|stop");
        return true;
    }
}
