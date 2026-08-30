package net.skykings.core.command;

import net.skykings.core.model.Rank;
import net.skykings.core.rank.RankService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /fly fuer Knight+ oder Spieler mit einem Fly-Gutscheinrecht. */
public final class FlyCommand implements CommandExecutor {

    public static final String PERMISSION = "skykings.perk.fly";

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
        if (!player.isOp() && !player.hasPermission(PERMISSION)
                && !rankService.hasAtLeast(player.getUniqueId(), Rank.KNIGHT)) {
            player.sendMessage(ChatColor.RED + "Du benoetigst mindestens Knight oder das Fly-Recht.");
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
