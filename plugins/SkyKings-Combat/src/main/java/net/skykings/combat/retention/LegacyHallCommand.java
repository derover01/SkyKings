package net.skykings.combat.retention;

import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiTheme;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /legacyhall zeigt Historie; Staff kann Top-3-Koepfe/Hologramme permanent platzieren. */
public final class LegacyHallCommand implements CommandExecutor {
    private final LegacyHallService hall;
    public LegacyHallCommand(LegacyHallService hall) { this.hall = hall; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        Player player = (Player) sender;
        if (args.length == 0) { hall.open(player, 1); return true; }
        if ("page".equalsIgnoreCase(args[0]) && args.length >= 2) {
            try { hall.open(player, Integer.parseInt(args[1])); }
            catch (NumberFormatException ex) { player.sendMessage(UiTheme.DANGER + "Ungueltige Seite."); }
            return true;
        }
        if (!player.hasPermission("skykings.admin.legacyhall")) {
            player.sendMessage(UiTheme.DANGER + "Keine Berechtigung."); return true;
        }
        if ("set".equalsIgnoreCase(args[0]) && args.length >= 3) {
            try {
                int season = Integer.parseInt(args[1]), rank = Integer.parseInt(args[2]);
                if (rank < 1 || rank > 3 || !hall.setDisplay(player, season, rank)) {
                    player.sendMessage(UiTheme.DANGER + "Kein Legacy-Eintrag fuer diese Season/Platzierung.");
                    SoundFeedback.error(player);
                } else {
                    player.sendMessage(UiTheme.SUCCESS + "Legacy Display gesetzt.");
                    SoundFeedback.success(player);
                }
            } catch (NumberFormatException ex) { player.sendMessage(UiTheme.DANGER + "Season und Platz muessen Zahlen sein."); }
            return true;
        }
        if ("remove".equalsIgnoreCase(args[0]) && args.length >= 3) {
            try {
                int season = Integer.parseInt(args[1]), rank = Integer.parseInt(args[2]);
                player.sendMessage(hall.removeDisplay(season, rank) ? UiTheme.SUCCESS + "Legacy Display entfernt." : UiTheme.DANGER + "Display nicht gefunden.");
            } catch (NumberFormatException ex) { player.sendMessage(UiTheme.DANGER + "Season und Platz muessen Zahlen sein."); }
            return true;
        }
        player.sendMessage(UiTheme.TEXT + "Legacy Hall");
        player.sendMessage(UiTheme.WARNING + "/legacyhall" + UiTheme.MUTED + " - Historie");
        player.sendMessage(UiTheme.WARNING + "/legacyhall set <Season> <1-3>" + UiTheme.MUTED + " - Head/Hologramm hier platzieren");
        player.sendMessage(UiTheme.WARNING + "/legacyhall remove <Season> <1-3>");
        return true;
    }
}
