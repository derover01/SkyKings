package net.skykings.core.shop;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Spezieller PvP-Restock-Shop, bezahlt ausschließlich mit physischen Nethersternen. */
public final class PvpRestockShopGui {

    private final GuiManager guiManager;
    private final ShopTransactionService transactions;

    public PvpRestockShopGui(GuiManager guiManager, ShopTransactionService transactions) {
        this.guiManager = guiManager;
        this.transactions = transactions;
    }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | PvP Restock", 27);

        add(gui, 10, new ShopOffer("pearls-16", new ItemStack(Material.ENDER_PEARL, 16), ShopCurrency.NETHERSTARS, 3L),
                ChatColor.DARK_PURPLE + "16x Enderperlen");
        add(gui, 11, new ShopOffer("op-gaps-4", new ItemStack(Material.GOLDEN_APPLE, 4, (short) 1), ShopCurrency.NETHERSTARS, 8L),
                ChatColor.GOLD + "4x OP-Gap");
        add(gui, 12, new ShopOffer("op-gaps-16", new ItemStack(Material.GOLDEN_APPLE, 16, (short) 1), ShopCurrency.NETHERSTARS, 28L),
                ChatColor.GOLD + "16x OP-Gap");
        add(gui, 14, new ShopOffer("arrows-64", new ItemStack(Material.ARROW, 64), ShopCurrency.NETHERSTARS, 2L),
                ChatColor.WHITE + "64x Pfeile");
        add(gui, 15, new ShopOffer("xp-32", new ItemStack(Material.EXP_BOTTLE, 32), ShopCurrency.NETHERSTARS, 4L),
                ChatColor.AQUA + "32x XP-Flaschen");
        add(gui, 16, new ShopOffer("golden-apples-16", new ItemStack(Material.GOLDEN_APPLE, 16), ShopCurrency.NETHERSTARS, 3L),
                ChatColor.YELLOW + "16x Goldene Äpfel");

        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Physische Nethersterne");
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Im Inventar: " + ChatColor.WHITE + transactions.countNetherstars(player.getInventory()));
        lore.add(ChatColor.DARK_GRAY + "Verdienst: PvP-Kills, Streaks, Crates und Events.");
        meta.setLore(lore);
        info.setItemMeta(meta);
        gui.setItem(22, info);

        guiManager.open(gui);
    }

    private void add(GuiSession gui, int slot, ShopOffer offer, String displayName) {
        ItemStack icon = offer.getItem();
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(displayName);
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Preis: " + ChatColor.LIGHT_PURPLE + offer.getPrice() + " Nethersterne");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Klicken zum Kaufen");
        meta.setLore(lore);
        icon.setItemMeta(meta);
        gui.setItem(slot, icon, (player, event, clickedSlot) -> {
            ShopPurchaseResult result = transactions.purchase(player, offer, "PVP_RESTOCK");
            switch (result) {
                case SUCCESS: player.sendMessage(ChatColor.GREEN + "Restock gekauft."); open(player); break;
                case NOT_ENOUGH_NETHERSTARS: player.sendMessage(ChatColor.RED + "Du hast nicht genug Nethersterne im Inventar."); break;
                case INVENTORY_FULL: player.sendMessage(ChatColor.RED + "Dein Inventar ist voll."); break;
                default: player.sendMessage(ChatColor.RED + "Der Kauf konnte nicht abgeschlossen werden."); break;
            }
        });
    }
}
