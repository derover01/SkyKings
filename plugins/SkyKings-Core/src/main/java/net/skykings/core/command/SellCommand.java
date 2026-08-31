package net.skykings.core.command;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.shop.ShopPriceRegistry;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** /sell hand und /sell all für whitelisted Vanilla-Items aus shops.yml. */
public final class SellCommand implements CommandExecutor {

    private final ShopPriceRegistry prices;
    private final EconomyService economy;

    public SellCommand(ShopPriceRegistry prices, EconomyService economy) {
        this.prices = prices;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur für Spieler verfügbar.");
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
        ItemStack snapshot = hand.clone();
        player.setItemInHand(null);
        try {
            economy.deposit(player.getUniqueId(), value, "SYSTEM_SHOP", "Sell hand " + snapshot.getType() + " x" + snapshot.getAmount());
        } catch (RuntimeException ex) {
            player.setItemInHand(snapshot);
            player.sendMessage(ChatColor.RED + "Verkauf fehlgeschlagen. Deine Items wurden zurückgegeben.");
            return true;
        }
        player.updateInventory();
        player.sendMessage(ChatColor.GREEN + "Verkauft für " + ChatColor.GOLD + format(value) + " Coins" + ChatColor.GREEN + ".");
        return true;
    }

    private boolean sellAll(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] snapshot = new ItemStack[contents.length];
        long total = 0L;
        int stacks = 0;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;
            snapshot[i] = item.clone();
            long value = prices.getSellValue(item);
            if (value <= 0L) continue;
            if (Long.MAX_VALUE - total < value) {
                player.sendMessage(ChatColor.RED + "Verkaufswert ist zu groß.");
                return true;
            }
            total += value;
            stacks++;
        }

        if (total <= 0L) {
            player.sendMessage(ChatColor.RED + "Du hast keine verkaufbaren Items im Inventar.");
            return true;
        }

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && prices.getSellValue(item) > 0L) player.getInventory().setItem(i, null);
        }

        try {
            economy.deposit(player.getUniqueId(), total, "SYSTEM_SHOP", "Sell all / stacks=" + stacks);
        } catch (RuntimeException ex) {
            for (int i = 0; i < snapshot.length; i++) player.getInventory().setItem(i, snapshot[i]);
            player.sendMessage(ChatColor.RED + "Verkauf fehlgeschlagen. Dein Inventar wurde wiederhergestellt.");
            return true;
        }

        player.updateInventory();
        player.sendMessage(ChatColor.GREEN + "Verkauft: " + stacks + " Stacks für " + ChatColor.GOLD + format(total) + " Coins" + ChatColor.GREEN + ".");
        return true;
    }

    private String format(long value) {
        return String.format(java.util.Locale.GERMANY, "%,d", value);
    }
}
