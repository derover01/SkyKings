package net.skykings.combat.map.zone;

import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiTheme;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Staff setzt die Open-World-PvP-Cuboids, die Target/Most-Wanted und spaetere Systeme nutzen. */
public final class PvpRegionCommand implements CommandExecutor {
    private final PvpRegionService regions;
    public PvpRegionCommand(PvpRegionService regions) { this.regions = regions; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        Player player = (Player) sender;
        if (!player.hasPermission("skykings.admin.pvpregion")) { player.sendMessage(UiTheme.DANGER + "Keine Berechtigung."); return true; }
        if (args.length == 0) { usage(player); return true; }

        if ("pos1".equalsIgnoreCase(args[0])) {
            regions.setPos1(player.getLocation());
            player.sendMessage(UiTheme.SUCCESS + "PvP-Region Pos1 gesetzt."); SoundFeedback.success(player); return true;
        }
        if ("pos2".equalsIgnoreCase(args[0])) {
            regions.setPos2(player.getLocation());
            player.sendMessage(UiTheme.SUCCESS + "PvP-Region Pos2 gesetzt."); SoundFeedback.success(player); return true;
        }
        if ("create".equalsIgnoreCase(args[0]) && args.length >= 2) {
            if (regions.create(args[1])) { player.sendMessage(UiTheme.SUCCESS + "PvP-Region erstellt: " + args[1]); SoundFeedback.success(player); }
            else { player.sendMessage(UiTheme.DANGER + "Pos1 und Pos2 muessen in derselben Welt gesetzt sein."); SoundFeedback.error(player); }
            return true;
        }
        if ("remove".equalsIgnoreCase(args[0]) && args.length >= 2) {
            player.sendMessage(regions.remove(args[1]) ? UiTheme.SUCCESS + "PvP-Region entfernt." : UiTheme.DANGER + "Region nicht gefunden.");
            return true;
        }
        if ("list".equalsIgnoreCase(args[0])) {
            player.sendMessage(UiTheme.TEXT + "PvP Regions");
            if (regions.getRegions().isEmpty()) player.sendMessage(UiTheme.MUTED + "Keine Regionen gesetzt.");
            for (String id : regions.getRegions().keySet()) player.sendMessage(UiTheme.PRIMARY + id);
            return true;
        }
        usage(player); return true;
    }

    private void usage(Player player) {
        player.sendMessage(UiTheme.TEXT + "PvP Regions");
        player.sendMessage(UiTheme.WARNING + "/pvpregion pos1 | pos2");
        player.sendMessage(UiTheme.WARNING + "/pvpregion create <ID>");
        player.sendMessage(UiTheme.WARNING + "/pvpregion remove <ID> | list");
    }
}
