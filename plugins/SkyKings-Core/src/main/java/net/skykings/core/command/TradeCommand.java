package net.skykings.core.command;

import net.skykings.core.trade.TradeGuiService;
import net.skykings.core.trade.TradeService;
import net.skykings.core.trade.TradeSession;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /trade <Spieler>, /trade accept <Spieler>, /trade deny */
public final class TradeCommand implements CommandExecutor {

    private final TradeService tradeService;
    private final TradeGuiService tradeGuiService;

    public TradeCommand(TradeService tradeService, TradeGuiService tradeGuiService) {
        this.tradeService = tradeService;
        this.tradeGuiService = tradeGuiService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler verfuegbar.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Nutze /trade <Spieler>, /trade accept <Spieler> oder /trade deny");
            return true;
        }

        if (args[0].equalsIgnoreCase("deny")) {
            tradeService.deny(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "Trade-Anfrage abgelehnt.");
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Nutze /trade accept <Spieler>");
                return true;
            }
            Player senderPlayer = Bukkit.getPlayerExact(args[1]);
            if (senderPlayer == null) {
                player.sendMessage(ChatColor.RED + "Spieler nicht gefunden.");
                return true;
            }
            TradeSession session = tradeService.accept(player.getUniqueId(), senderPlayer.getUniqueId());
            if (session == null) {
                player.sendMessage(ChatColor.RED + "Keine passende Trade-Anfrage gefunden.");
                return true;
            }
            senderPlayer.sendMessage(ChatColor.GREEN + player.getName() + " hat deine Trade-Anfrage angenommen.");
            player.sendMessage(ChatColor.GREEN + "Trade mit " + senderPlayer.getName() + " gestartet.");
            tradeGuiService.openForBoth(session);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || target.equals(player)) {
            player.sendMessage(ChatColor.RED + "Spieler nicht gefunden oder ungueltiges Ziel.");
            return true;
        }
        if (!tradeService.request(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Trade-Anfrage konnte nicht erstellt werden. Einer von euch tradet bereits.");
            return true;
        }
        player.sendMessage(ChatColor.GREEN + "Trade-Anfrage an " + target.getName() + " gesendet.");
        target.sendMessage(ChatColor.GOLD + player.getName() + " moechte mit dir handeln.");
        target.sendMessage(ChatColor.YELLOW + "/trade accept " + player.getName() + ChatColor.GRAY + " oder " + ChatColor.RED + "/trade deny");
        return true;
    }
}
