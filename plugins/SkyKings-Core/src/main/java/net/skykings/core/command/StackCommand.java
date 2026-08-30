package net.skykings.core.command;

import net.skykings.core.model.Rank;
import net.skykings.core.rank.RankService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Verdichtet gleichartige stackbare Inventar-Items fuer Knight+. */
public final class StackCommand implements CommandExecutor {

    private final RankService rankService;

    public StackCommand(RankService rankService) {
        this.rankService = rankService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!rankService.hasAtLeast(player.getUniqueId(), Rank.KNIGHT)) {
            player.sendMessage(ChatColor.RED + "Du benoetigst mindestens den Rang Knight fuer /stack.");
            return true;
        }

        ItemStack[] contents = player.getInventory().getContents();
        List<ItemStack> compacted = new ArrayList<ItemStack>();
        int moved = 0;
        for (ItemStack original : contents) {
            if (original == null || original.getAmount() <= 0) {
                continue;
            }
            ItemStack remaining = original.clone();
            for (ItemStack target : compacted) {
                if (remaining.getAmount() <= 0) break;
                if (!target.isSimilar(remaining)) continue;
                int room = target.getMaxStackSize() - target.getAmount();
                if (room <= 0) continue;
                int transfer = Math.min(room, remaining.getAmount());
                target.setAmount(target.getAmount() + transfer);
                remaining.setAmount(remaining.getAmount() - transfer);
                moved += transfer;
            }
            while (remaining.getAmount() > 0) {
                int amount = Math.min(remaining.getMaxStackSize(), remaining.getAmount());
                ItemStack part = remaining.clone();
                part.setAmount(amount);
                compacted.add(part);
                remaining.setAmount(remaining.getAmount() - amount);
            }
        }

        player.getInventory().clear();
        for (int i = 0; i < compacted.size() && i < player.getInventory().getSize(); i++) {
            player.getInventory().setItem(i, compacted.get(i));
        }
        player.updateInventory();
        player.sendMessage(ChatColor.GREEN + "Inventar gestackt" + (moved > 0 ? ChatColor.GRAY + " (" + moved + " Items zusammengefuehrt)." : ChatColor.GRAY + "."));
        return true;
    }
}
