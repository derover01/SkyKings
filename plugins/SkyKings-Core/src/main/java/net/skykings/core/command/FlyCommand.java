package net.skykings.core.command;

import net.skykings.core.model.Rank;
import net.skykings.core.rank.RankService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /fly fuer Knight+; Combat deaktiviert/sperrt Fly waehrend PvP separat. */
public final class FlyCommand implements CommandExecutor {

    private final RankService rankService;

    public FlyCommand(RankService rankService) {
        this.rankService = rankService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.isOp() && !rankService.hasAtLeast(player.getUniqueId(), Rank.KNIGHT)) {
            player.sendMessage(ChatColor.RED + "Du benoetigst mindestens den Rang Knight fuer /fly.");
            return true;
        }
        boolean enable = !player.getAllowFlight();
        player.setAllowFlight(enable);
        if (!enable && player.isFlying()) {
            player.setFlying(false);
        }
        player.sendMessage(enable
                ? ChatColor.GREEN + "Flugmodus aktiviert. Im PvP wird er automatisch deaktiviert."
                : ChatColor.YELLOW + "Flugmodus deaktiviert.");
        return true;
    }
}
