package net.skykings.core.command;

import net.skykings.core.rank.RankProgressionResult;
import net.skykings.core.rank.RankProgressionService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /rankup kauft exakt den naechsten Free-Rank. Paid-Raenge sind nie kaufbar. */
public final class RankupCommand implements CommandExecutor {

    private final RankProgressionService progressionService;

    public RankupCommand(RankProgressionService progressionService) {
        this.progressionService = progressionService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler.");
            return true;
        }
        Player player = (Player) sender;
        RankProgressionResult result = progressionService.purchaseNext(player.getUniqueId());

        switch (result.getStatus()) {
            case SUCCESS:
                player.sendMessage(ChatColor.GREEN + "Rankup erfolgreich! " + ChatColor.GRAY + "Du bist jetzt "
                        + ChatColor.GOLD + result.getTargetRank().name() + ChatColor.GRAY + ". Kosten: "
                        + ChatColor.YELLOW + formatCoins(result.getCost()) + " Coins");
                break;
            case INSUFFICIENT_COINS:
                player.sendMessage(ChatColor.RED + "Dir fehlen Coins fuer den naechsten Rang "
                        + ChatColor.GOLD + result.getTargetRank().name() + ChatColor.RED + ". Preis: "
                        + ChatColor.YELLOW + formatCoins(result.getCost()) + " Coins");
                break;
            case MAX_FREE_RANK:
                player.sendMessage(ChatColor.AQUA + "Du hast mit Diamond bereits den hoechsten kaufbaren Free-Rang.");
                break;
            case PAID_RANK:
                player.sendMessage(ChatColor.GOLD + "Du besitzt bereits einen Paid-Rang. Paid-Raenge koennen nicht mit Coins gekauft werden.");
                break;
            default:
                player.sendMessage(ChatColor.RED + "Rankup konnte nicht ausgefuehrt werden.");
        }
        return true;
    }

    private String formatCoins(long value) {
        return String.format(java.util.Locale.GERMANY, "%,d", value);
    }
}
