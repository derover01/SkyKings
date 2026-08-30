package net.skykings.core.command;

import net.skykings.core.model.Rank;
import net.skykings.core.rank.RankService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Repariert das gehaltene Item fuer Exile+ oder Spieler mit Repair-Gutscheinrecht. */
public final class RepairCommand implements CommandExecutor {

    public static final String PERMISSION = "skykings.perk.repair";

    private final RankService rankService;

    public RepairCommand(RankService rankService) {
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
                && !rankService.hasAtLeast(player.getUniqueId(), Rank.EXILE)) {
            player.sendMessage(ChatColor.RED + "Du benoetigst mindestens Exile oder das Repair-Recht.");
            return true;
        }
        ItemStack item = player.getItemInHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Halte das Item, das du reparieren moechtest, in der Hand.");
            return true;
        }
        if (item.getType().getMaxDurability() <= 0) {
            player.sendMessage(ChatColor.RED + "Dieses Item kann nicht repariert werden.");
            return true;
        }
        if (item.getDurability() == 0) {
            player.sendMessage(ChatColor.YELLOW + "Dieses Item ist bereits vollstaendig repariert.");
            return true;
        }
        item.setDurability((short) 0);
        player.setItemInHand(item);
        player.updateInventory();
        player.sendMessage(ChatColor.GREEN + "Item repariert.");
        return true;
    }
}
