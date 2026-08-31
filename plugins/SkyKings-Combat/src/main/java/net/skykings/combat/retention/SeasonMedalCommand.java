package net.skykings.combat.retention;

import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.ConfirmationMenu;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** /medals fuer Spieler und /seasonadmin finish als bestaetigter Season-Abschluss. */
public final class SeasonMedalCommand implements CommandExecutor {
    private final SeasonMedalService medals;
    private final SeasonProgressService progress;

    public SeasonMedalCommand(SeasonMedalService medals, SeasonProgressService progress) {
        this.medals = medals;
        this.progress = progress;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("seasonadmin".equalsIgnoreCase(command.getName())) return seasonAdmin(sender, args);
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        Player player = (Player) sender;
        UUID target = player.getUniqueId();
        if (args.length >= 1) {
            OfflinePlayer off = Bukkit.getOfflinePlayer(args[0]);
            if (off == null || (off.getName() == null && !off.hasPlayedBefore())) {
                player.sendMessage(UiTheme.DANGER + "Spieler nicht gefunden.");
                SoundFeedback.error(player);
                return true;
            }
            target = off.getUniqueId();
        }
        medals.open(player, target);
        return true;
    }

    private boolean seasonAdmin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Season-Finish muss ingame bestaetigt werden."); return true; }
        final Player player = (Player) sender;
        if (!player.hasPermission("skykings.admin.season")) { player.sendMessage(UiTheme.DANGER + "Keine Berechtigung."); return true; }
        if (args.length == 0 || !"finish".equalsIgnoreCase(args[0])) {
            player.sendMessage(UiTheme.TEXT + "Season Admin");
            player.sendMessage(UiTheme.WARNING + "/seasonadmin finish");
            return true;
        }
        final int season = progress.getSeason();
        ConfirmationMenu.open(player,
                UiItems.item(Material.NETHER_STAR,
                        UiTheme.LEGENDARY + "Season " + season + " abschliessen",
                        UiTheme.MUTED + "Legacy Hall + Medaillen werden archiviert.",
                        UiTheme.DANGER + "Season-XP wird danach zurueckgesetzt."),
                "Season Finish",
                "Season " + season + " permanent archivieren",
                () -> {
                    if (medals.finishSeason()) {
                        player.sendMessage(UiTheme.SUCCESS + "Season " + season + " abgeschlossen.");
                        SoundFeedback.reward(player);
                    } else {
                        player.sendMessage(UiTheme.DANGER + "Season konnte nicht abgeschlossen werden. Ranking ist leer oder Finish laeuft bereits.");
                        SoundFeedback.error(player);
                    }
                },
                null);
        return true;
    }
}
