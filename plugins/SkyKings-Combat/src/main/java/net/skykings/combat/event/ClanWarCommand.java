package net.skykings.combat.event;

import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/** Spieler-/Staff-Command fuer sichere Clan-Wars. */
public final class ClanWarCommand implements CommandExecutor {
    private final ClanWarService wars;

    public ClanWarCommand(ClanWarService wars) {
        this.wars = wars;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Nur ingame.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("accept") || sub.equals("annehmen")) {
            ClanWarService.StartResult result = wars.accept(player);
            if (result == ClanWarService.StartResult.SUCCESS) {
                player.sendMessage(UiTheme.SUCCESS + "Clan War angenommen.");
                SoundFeedback.success(player);
            } else {
                player.sendMessage(UiTheme.DANGER + message(result));
                SoundFeedback.error(player);
            }
            return true;
        }
        if (sub.equals("deny") || sub.equals("ablehnen")) {
            if (wars.deny(player)) {
                player.sendMessage(UiTheme.MUTED + "Clan-War Anfrage abgelehnt.");
            } else {
                player.sendMessage(UiTheme.DANGER + "Keine offene Clan-War Anfrage.");
                SoundFeedback.error(player);
            }
            return true;
        }
        if (sub.equals("status")) {
            player.sendMessage(UiTheme.PRIMARY + "CLAN WAR" + UiTheme.MUTED + " • " + wars.status());
            return true;
        }
        if (sub.equals("stop")) {
            if (!player.hasPermission("skykings.admin.event")) {
                player.sendMessage(UiTheme.DANGER + "Keine Berechtigung.");
                SoundFeedback.error(player);
                return true;
            }
            wars.stop(true);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(UiTheme.DANGER + "Spieler nicht gefunden.");
            SoundFeedback.error(player);
            return true;
        }
        if (!wars.challenge(player, target)) {
            player.sendMessage(UiTheme.DANGER + "Clan-War Anfrage nicht moeglich."
                    + UiTheme.MUTED + " Beide Clan-Owner brauchen mindestens 2 freie Mitglieder online.");
            SoundFeedback.error(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(UiTheme.PRIMARY + "CLAN WAR");
        player.sendMessage(UiTheme.TEXT + "/clanwar <Clan-Owner>" + UiTheme.MUTED + " • 2v2 bis 5v5 herausfordern");
        player.sendMessage(UiTheme.TEXT + "/clanwar accept" + UiTheme.MUTED + " • Anfrage annehmen");
        player.sendMessage(UiTheme.TEXT + "/clanwar deny" + UiTheme.MUTED + " • Anfrage ablehnen");
        player.sendMessage(UiTheme.TEXT + "/clanwar status" + UiTheme.MUTED + " • laufenden War anzeigen");
    }

    private String message(ClanWarService.StartResult result) {
        switch (result) {
            case NO_CHALLENGE: return "Keine offene Clan-War Anfrage.";
            case INVALID_CLAN: return "Clan oder Owner hat sich geaendert.";
            case NOT_ENOUGH_PLAYERS: return "Mindestens 2 freie Spieler pro Clan muessen online sein.";
            case ARENA_NOT_READY: return "Clan-War Arena ist noch nicht eingerichtet.";
            case BUSY: return "Es laeuft bereits ein Clan War.";
            default: return "Clan War konnte nicht gestartet werden.";
        }
    }
}
