package net.skykings.core.shop;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/** Seltene rotierende Angebote fuer Event-/Black-Market-NPCs. */
public final class TravelingMerchantGui {

    private final GuiManager guiManager;
    private final ShopTransactionService transactions;

    public TravelingMerchantGui(GuiManager guiManager, ShopTransactionService transactions) {
        this.guiManager = guiManager;
        this.transactions = transactions;
    }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Black Market", 27);

        add(gui, 10, new ShopOffer("blackmarket_gapples", new ItemStack(Material.GOLDEN_APPLE, 8, (short) 1), ShopCurrency.NETHERSTARS, 18),
                ChatColor.GOLD + "8x OP-Gapple", "18 Nethersterne");
        add(gui, 12, new ShopOffer("blackmarket_pearls", new ItemStack(Material.ENDER_PEARL, 16), ShopCurrency.NETHERSTARS, 10),
                ChatColor.LIGHT_PURPLE + "16x Enderperle", "10 Nethersterne");
        add(gui, 14, new ShopOffer("blackmarket_diamond", new ItemStack(Material.DIAMOND, 16), ShopCurrency.NETHERSTARS, 22),
                ChatColor.AQUA + "16x Diamant", "22 Nethersterne");
        add(gui, 16, new ShopOffer("blackmarket_xp", new ItemStack(Material.EXP_BOTTLE, 64), ShopCurrency.NETHERSTARS, 12),
                ChatColor.GREEN + "64x XP-Flasche", "12 Nethersterne");

        gui.setItem(22, named(Material.WATCH, ChatColor.YELLOW + "Reisender Haendler",
                ChatColor.GRAY + "Seltene Angebote gegen physische Nethersterne.",
                ChatColor.DARK_GRAY + "Das Sortiment kann spaeter pro Event/Tag rotieren."));

        guiManager.open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void add(GuiSession gui, int slot, ShopOffer offer, String name, String price) {
        ItemStack icon = offer.getItem();
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Preis: " + ChatColor.YELLOW + price,
                ChatColor.YELLOW + "Klicken zum Kaufen"));
        icon.setItemMeta(meta);
        gui.setItem(slot, icon, (p,e,s) -> {
            ShopPurchaseResult result = transactions.purchase(p, offer, "black_market");
            if (result == ShopPurchaseResult.SUCCESS) {
                p.sendMessage(ChatColor.GREEN + "Black-Market-Kauf erfolgreich.");
                SoundFeedback.reward(p);
            } else {
                p.sendMessage(ChatColor.RED + purchaseError(result));
                SoundFeedback.error(p);
            }
        });
    }

    private String purchaseError(ShopPurchaseResult result) {
        switch (result) {
            case NOT_ENOUGH_NETHERSTARS: return "Du hast nicht genug Nethersterne.";
            case NOT_ENOUGH_MONEY: return "Du hast nicht genug Coins.";
            case INVENTORY_FULL: return "Dein Inventar ist voll.";
            default: return "Der Kauf konnte nicht abgeschlossen werden.";
        }
    }

    private ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}
