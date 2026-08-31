package net.skykings.core.command;

import net.skykings.core.shop.ShopPriceRegistry;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** /worth zeigt den Verkaufspreis des gehaltenen Items. */
public final class WorthCommand implements CommandExecutor {

    private final ShopPriceRegistry prices;

    public WorthCommand(ShopPriceRegistry prices) {
        this.prices = prices;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur für Spieler verfügbar.");
            return true;
        }
        Player player = (Player) sender;
        ItemStack hand = player.getItemInHand();
        long value = prices.getSellValue(hand);
        if (value <= 0L) {
            player.sendMessage(ChatColor.RED + "Dieses Item kann nicht an den Systemshop verkauft werden.");
            return true;
        }
        ShopPriceRegistry.Price price = prices.get(hand);
        player.sendMessage(ChatColor.GRAY + "Wert pro Item: " + ChatColor.GOLD + format(price.getSell()) + " Coins");
        player.sendMessage(ChatColor.GRAY + "Wert des Stacks: " + ChatColor.GOLD + format(value) + " Coins");
        return true;
    }

    private String format(long value) {
        return String.format(java.util.Locale.GERMANY, "%,d", value);
    }
}
