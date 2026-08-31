package net.skykings.combat.event;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /duel Spieler [Arena], /duel accept, /duel deny. */
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
            player.sendMessage(ChatColor.GOLD + "Duel-System");
            player.sendMessage(ChatColor.YELLOW + "/duel <Spieler> [Arena]");
            player.sendMessage(ChatColor.YELLOW + "/duel accept");
            player.sendMessage(ChatColor.YELLOW + "/duel deny");
            return true;
        }

        if ("accept".equalsIgnoreCase(args[0]) || "annehmen".equalsIgnoreCase(args[0])) {
            DuelService.StartResult result = duels.accept(player);
            if (result != DuelService.StartResult.SUCCESS) sendStartError(player, result);
            return true;
        }
        if ("deny".equalsIgnoreCase(args[0]) || "decline".equalsIgnoreCase(args[0]) || "ablehnen".equalsIgnoreCase(args[0])) {
            if (!duels.deny(player)) player.sendMessage(ChatColor.RED + "Du hast keine aktive Duel-Anfrage.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Spieler nicht gefunden.");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Du kannst dich nicht selbst herausfordern.");
            return true;
        }
        String arena = args.length >= 2 ? args[1] : "duel";
        if (!duels.request(player, target, arena)) {
            player.sendMessage(ChatColor.RED + "Duel-Anfrage nicht moeglich. Einer von euch ist bereits beschaeftigt.");
        }
        return true;
    }

    private void sendStartError(Player player, DuelService.StartResult result) {
        switch (result) {
            case ARENA_NOT_READY:
                player.sendMessage(ChatColor.RED + "Duel-Arena ist noch nicht eingerichtet. Staff: /eventarena set duel a|b");
                break;
            case COMBAT_TAGGED:
                player.sendMessage(ChatColor.RED + "Ein Spieler ist noch im normalen Combat. Duel wurde nicht gestartet.");
                break;
            case TELEPORT_FAILED:
                player.sendMessage(ChatColor.RED + "Duel konnte nicht sicher gestartet werden. Teleport fehlgeschlagen.");
                break;
            case PLAYER_BUSY:
            default:
                player.sendMessage(ChatColor.RED + "Keine gueltige Duel-Anfrage mehr oder ein Spieler ist bereits beschaeftigt.");
                break;
        }
    }
}
