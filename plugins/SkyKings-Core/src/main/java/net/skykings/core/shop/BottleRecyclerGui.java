package net.skykings.core.shop;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.economy.EconomyService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Recycelt leere Glasflaschen aus PvP/Potion-Nutzung gegen kleine Coin-Betraege. */
public final class BottleRecyclerGui {

    private static final long COINS_PER_BOTTLE = 250L;

    private final GuiManager guiManager;
    private final ShopTransactionService directTransactions;

    public BottleRecyclerGui(GuiManager guiManager, ShopTransactionService transactions) {
        this.guiManager = guiManager;
        this.directTransactions = transactions;
    }

    /** Bootstrap-kompatibel; beim ersten echten NPC-Klick ist die Core-API bereits registriert. */
    public BottleRecyclerGui(GuiManager guiManager, EconomyService ignoredEconomy) {
        this.guiManager = guiManager;
        this.directTransactions = null;
    }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Bottle Recycler", 27);
        int bottles = count(player);
        long value = bottles * COINS_PER_BOTTLE;

        gui.setItem(11, named(Material.GLASS_BOTTLE, ChatColor.AQUA + "Flaschen recyceln",
                ChatColor.GRAY + "Leere Flaschen: " + ChatColor.WHITE + bottles,
                ChatColor.GRAY + "Wert: " + ChatColor.GOLD + format(value) + " Coins",
                "",
                ChatColor.YELLOW + "Klicken: alle Flaschen verkaufen"), (p,e,s) -> recycle(p));

        gui.setItem(15, named(Material.GOLD_INGOT, ChatColor.GOLD + "Recycling-Preis",
                ChatColor.GRAY + "1 Flasche = " + ChatColor.YELLOW + format(COINS_PER_BOTTLE) + " Coins"));
        guiManager.open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void recycle(Player player) {
        ShopTransactionService transactions = transactions();
        if (transactions == null) {
            player.sendMessage(ChatColor.RED + "Shop-Transaktionen sind noch nicht sicher bereit. Recycling abgebrochen.");
            SoundFeedback.error(player);
            return;
        }

        Map<Integer, ItemStack> sold = new LinkedHashMap<Integer, ItemStack>();
        int bottles = 0;
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType() != Material.GLASS_BOTTLE) continue;
            sold.put(slot, item.clone());
            bottles += item.getAmount();
        }
        if (bottles <= 0) {
            player.sendMessage(ChatColor.RED + "Du hast keine leeren Glasflaschen dabei.");
            SoundFeedback.error(player);
            return;
        }

        long payout = bottles * COINS_PER_BOTTLE;
        ShopSaleResult result = transactions.sell(player, sold, payout, "BOTTLE_RECYCLER", bottles + " bottles");
        if (result == ShopSaleResult.SUCCESS) {
            player.sendMessage(ChatColor.GREEN + "Recycelt: " + ChatColor.WHITE + bottles + ChatColor.GREEN
                    + " Flaschen fuer " + ChatColor.GOLD + format(payout) + ChatColor.GREEN + " Coins.");
            SoundFeedback.reward(player);
            open(player);
            return;
        }

        SoundFeedback.error(player);
        if (result == ShopSaleResult.BALANCE_OVERFLOW) {
            player.sendMessage(ChatColor.RED + "Recycling blockiert: Dein Coin-Kontostand waere danach zu hoch.");
        } else if (result == ShopSaleResult.REVIEW_REQUIRED) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "Recycling blockiert: Eine Shop-Transaktion braucht Staff-Pruefung. Bitte nicht erneut versuchen.");
        } else if (result == ShopSaleResult.STALE_INVENTORY) {
            player.sendMessage(ChatColor.RED + "Dein Inventar hat sich geaendert. Oeffne den Recycler erneut.");
        } else {
            player.sendMessage(ChatColor.RED + "Recycling konnte nicht sicher gespeichert werden.");
        }
    }

    private ShopTransactionService transactions() {
        if (directTransactions != null) return directTransactions;
        SkyKingsCoreAPI api = Bukkit.getServicesManager().load(SkyKingsCoreAPI.class);
        return api == null ? null : api.getShopTransactionService();
    }

    private int count(Player player) {
        int amount = 0;
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item != null && item.getType() == Material.GLASS_BOTTLE) amount += item.getAmount();
        }
        return amount;
    }

    private ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private String format(long value) {
        return String.format("%,d", value).replace(',', '.');
    }
}
