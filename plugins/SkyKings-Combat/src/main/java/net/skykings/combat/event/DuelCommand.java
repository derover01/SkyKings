package net.skykings.combat.event;

import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /duel Spieler [Coins] [Arena], /duel accept, /duel deny. */
public final class DuelCommand implements CommandExecutor {
    private final DuelService duels;

    public DuelCommand(DuelService duels) { this.duels = duels; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage(UiTheme.TEXT + "Duels");
            player.sendMessage(UiTheme.WARNING + "/duel <Spieler>" + UiTheme.MUTED + " - ohne Einsatz");
            player.sendMessage(UiTheme.WARNING + "/duel <Spieler> <Coins> [Arena]" + UiTheme.MUTED + " - Wager");
            player.sendMessage(UiTheme.WARNING + "/duel accept | deny");
            return true;
        }

        if ("accept".equalsIgnoreCase(args[0]) || "annehmen".equalsIgnoreCase(args[0])) {
            DuelService.StartResult result = duels.accept(player);
            if (result != DuelService.StartResult.SUCCESS) sendStartError(player, result);
            return true;
        }
        if ("deny".equalsIgnoreCase(args[0]) || "decline".equalsIgnoreCase(args[0]) || "ablehnen".equalsIgnoreCase(args[0])) {
            if (!duels.deny(player)) {
                player.sendMessage(UiTheme.DANGER + "Keine aktive Duel-Anfrage.");
                SoundFeedback.error(player);
            }
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(UiTheme.DANGER + "Spieler nicht gefunden.");
            SoundFeedback.error(player);
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(UiTheme.DANGER + "Du kannst dich nicht selbst herausfordern.");
            return true;
        }

        long wager = 0L;
        String arena = "duel";
        if (args.length >= 2) {
            try {
                wager = parseCoins(args[1]);
                if (args.length >= 3) arena = args[2];
            } catch (NumberFormatException notCoins) {
                // Legacy-Komfort: /duel Spieler Arena bleibt gueltig.
                arena = args[1];
            }
        }
        if (wager < 0L || wager > DuelService.MAX_WAGER) {
            player.sendMessage(UiTheme.DANGER + "Einsatz muss zwischen 0 und " + UiFormat.coins(DuelService.MAX_WAGER) + " liegen.");
            SoundFeedback.error(player);
            return true;
        }

        if (!duels.request(player, target, arena, wager)) {
            player.sendMessage(UiTheme.DANGER + "Duel-Anfrage nicht moeglich.");
            player.sendMessage(UiTheme.MUTED + "Pruefe Coins, Combat-Status und ob einer von euch bereits beschaeftigt ist.");
            SoundFeedback.error(player);
        }
        return true;
    }

    private long parseCoins(String raw) {
        String value = raw.trim().toLowerCase(java.util.Locale.ROOT).replace(".", "").replace("_", "");
        long multiplier = 1L;
        if (value.endsWith("k")) { multiplier = 1_000L; value = value.substring(0, value.length() - 1); }
        else if (value.endsWith("m")) { multiplier = 1_000_000L; value = value.substring(0, value.length() - 1); }
        long base = Long.parseLong(value);
        return Math.multiplyExact(base, multiplier);
    }

    private void sendStartError(Player player, DuelService.StartResult result) {
        switch (result) {
            case ARENA_NOT_READY:
                player.sendMessage(UiTheme.DANGER + "Duel-Arena ist noch nicht eingerichtet.");
                player.sendMessage(UiTheme.MUTED + "Staff: /eventarena set duel a|b");
                break;
            case COMBAT_TAGGED:
                player.sendMessage(UiTheme.DANGER + "Einer von euch ist noch im normalen Combat.");
                break;
            case TELEPORT_FAILED:
                player.sendMessage(UiTheme.DANGER + "Duel-Teleport fehlgeschlagen.");
                break;
            case NOT_ENOUGH_MONEY:
                player.sendMessage(UiTheme.DANGER + "Einer von euch hat nicht mehr genug Coins fuer den Einsatz.");
                break;
            case INVALID_WAGER:
                player.sendMessage(UiTheme.DANGER + "Ungueltiger Duel-Einsatz.");
                break;
            case PLAYER_BUSY:
            default:
                player.sendMessage(UiTheme.DANGER + "Keine gueltige Anfrage oder Spieler bereits beschaeftigt.");
                break;
        }
        SoundFeedback.error(player);
    }
}
