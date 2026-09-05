package net.skykings.core.command;

import net.skykings.core.shop.ShopPriceRegistry;
import net.skykings.core.shop.ShopSaleResult;
import net.skykings.core.shop.ShopTransactionService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/** /sell hand und /sell all fuer whitelisted Vanilla-Items aus shops.yml. */
public final class SellCommand implements CommandExecutor {

    private final ShopPriceRegistry prices;
    private final ShopTransactionService transactions;

    public SellCommand(ShopPriceRegistry prices, ShopTransactionService transactions) {
        this.prices = prices;
        this.transactions = transactions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        String mode = args.length == 0 ? "hand" : args[0].toLowerCase(java.util.Locale.ROOT);
        if ("hand".equals(mode)) return sellHand(player);
        if ("all".equals(mode)) return sellAll(player);
        player.sendMessage(ChatColor.RED + "Nutze /sell hand oder /sell all.");
        return true;
    }

    private boolean sellHand(Player player) {
        ItemStack hand = player.getItemInHand();
        long value = prices.getSellValue(hand);
        if (value <= 0L) {
            player.sendMessage(ChatColor.RED + "Dieses Item kann nicht verkauft werden.");
            return true;
        }
        if (value == Long.MAX_VALUE) {
            player.sendMessage(ChatColor.RED + "Verkaufswert ist zu gross.");
            return true;
        }

        Map<Integer, ItemStack> sold = new LinkedHashMap<Integer, ItemStack>();
        ItemStack snapshot = hand.clone();
        sold.put(player.getInventory().getHeldItemSlot(), snapshot);
        ShopSaleResult result = transactions.sell(player, sold, value, "SELL_HAND",
                "Sell hand " + snapshot.getType() + " x" + snapshot.getAmount());
        if (result == ShopSaleResult.SUCCESS) {
            player.sendMessage(ChatColor.GREEN + "Verkauft fuer " + ChatColor.GOLD + format(value) + " Coins" + ChatColor.GREEN + ".");
        } else {
            sendFailure(player, result);
        }
        return true;
    }

    private boolean sellAll(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        Map<Integer, ItemStack> sold = new LinkedHashMap<Integer, ItemStack>();
        long total = 0L;
        int stacks = 0;

        int limit = Math.min(contents.length, player.getInventory().getSize());
        for (int i = 0; i < limit; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;
            long value = prices.getSellValue(item);
            if (value <= 0L) continue;
            if (value == Long.MAX_VALUE || Long.MAX_VALUE - total < value) {
                player.sendMessage(ChatColor.RED + "Verkaufswert ist zu gross.");
                return true;
            }
            total += value;
            stacks++;
            sold.put(i, item.clone());
        }

        if (total <= 0L || sold.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Du hast keine verkaufbaren Items im Inventar.");
            return true;
        }

        ShopSaleResult result = transactions.sell(player, sold, total, "SELL_ALL", "Sell all / stacks=" + stacks);
        if (result == ShopSaleResult.SUCCESS) {
            player.sendMessage(ChatColor.GREEN + "Verkauft: " + stacks + " Stacks fuer " + ChatColor.GOLD
                    + format(total) + " Coins" + ChatColor.GREEN + ".");
        } else {
            sendFailure(player, result);
        }
        return true;
    }

    private void sendFailure(Player player, ShopSaleResult result) {
        if (result == ShopSaleResult.BALANCE_OVERFLOW) {
            player.sendMessage(ChatColor.RED + "Verkauf blockiert: Dein Coin-Kontostand waere danach zu hoch.");
        } else if (result == ShopSaleResult.REVIEW_REQUIRED) {
            player.sendMessage(ChatColor.RED + "Verkauf blockiert: Eine Shop-Transaktion braucht Staff-Pruefung. Bitte nicht erneut versuchen.");
        } else if (result == ShopSaleResult.STALE_INVENTORY) {
            player.sendMessage(ChatColor.RED + "Dein Inventar hat sich geaendert. Bitte versuche den Verkauf erneut.");
        } else {
            player.sendMessage(ChatColor.RED + "Verkauf konnte nicht sicher gespeichert werden. Es wurden keine neuen Verkaufsaktionen gestartet.");
        }
    }

    private String format(long value) {
        return String.format(java.util.Locale.GERMANY, "%,d", value);
    }
}
