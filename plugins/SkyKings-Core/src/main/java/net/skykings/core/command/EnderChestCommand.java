package net.skykings.core.command;

import net.skykings.core.model.Rank;
import net.skykings.core.rank.RankService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /ec fuer Gold+ oder Spieler mit dauerhaftem Enderchest-Recht. */
public final class EnderChestCommand implements CommandExecutor {

    public static final String PERMISSION = "skykings.perk.enderchest";

    private final RankService rankService;

    public EnderChestCommand(RankService rankService) {
        this.rankService = rankService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur für Spieler verfügbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(PERMISSION)
                && !rankService.hasAtLeast(player.getUniqueId(), Rank.GOLD)) {
            player.sendMessage(ChatColor.RED + "Du benötigst mindestens Gold oder das Enderchest-Recht.");
            return true;
        }
        player.openInventory(player.getEnderChest());
        return true;
    }
}
