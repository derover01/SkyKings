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

/** Erster öffentlicher Shop auf Basis der weltunabhängigen Shop-Engine. */
public final class SystemShopGui {

    private final GuiManager guiManager;
    private final ShopTransactionService transactions;

    public SystemShopGui(GuiManager guiManager, ShopTransactionService transactions) {
        this.guiManager = guiManager;
        this.transactions = transactions;
    }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Shop", 27);

        add(gui, 10, new ShopOffer("food-steak", new ItemStack(Material.COOKED_BEEF, 16), ShopCurrency.COINS, 25000L),
                ChatColor.GOLD + "16x Steak", "SYSTEM");
        add(gui, 11, new ShopOffer("arrows", new ItemStack(Material.ARROW, 64), ShopCurrency.COINS, 50000L),
                ChatColor.WHITE + "64x Pfeile", "SYSTEM");
        add(gui, 12, new ShopOffer("xp-bottles", new ItemStack(Material.EXP_BOTTLE, 16), ShopCurrency.COINS, 125000L),
                ChatColor.AQUA + "16x XP-Flaschen", "SYSTEM");

        ItemStack pearls = new ItemStack(Material.ENDER_PEARL, 16);
        add(gui, 14, new ShopOffer("pvp-pearls", pearls, ShopCurrency.NETHERSTARS, 3L),
                ChatColor.DARK_PURPLE + "16x Enderperlen", "PVP_RESTOCK");

        ItemStack opGaps = new ItemStack(Material.GOLDEN_APPLE, 4, (short) 1);
        add(gui, 15, new ShopOffer("pvp-op-gaps", opGaps, ShopCurrency.NETHERSTARS, 8L),
                ChatColor.GOLD + "4x OP-Gap", "PVP_RESTOCK");

        ItemStack info = named(Material.NETHER_STAR, ChatColor.YELLOW + "Dein PvP-Guthaben",
                ChatColor.GRAY + "Physische Nethersterne im Inventar: " + ChatColor.WHITE
                        + transactions.countNetherstars(player.getInventory()),
                ChatColor.DARK_GRAY + "Nethersterne sind echte Items und keine digitale Währung.");
        gui.setItem(22, info);

        guiManager.open(gui);
    }

    private void add(GuiSession gui, int slot, ShopOffer offer, String displayName, String shopId) {
        ItemStack icon = offer.getItem();
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(displayName);
        List<String> lore = new ArrayList<String>();
        if (offer.getCurrency() == ShopCurrency.COINS) {
            lore.add(ChatColor.GRAY + "Preis: " + ChatColor.GOLD + format(offer.getPrice()) + " Coins");
        } else {
            lore.add(ChatColor.GRAY + "Preis: " + ChatColor.LIGHT_PURPLE + offer.getPrice() + " Nethersterne");
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "Klicken zum Kaufen");
        meta.setLore(lore);
        icon.setItemMeta(meta);
        gui.setItem(slot, icon, (player, event, clickedSlot) -> {
            ShopPurchaseResult result = transactions.purchase(player, offer, shopId);
            sendResult(player, result);
            if (result == ShopPurchaseResult.SUCCESS) open(player);
        });
    }

    private void sendResult(Player player, ShopPurchaseResult result) {
        switch (result) {
            case SUCCESS:
                player.sendMessage(ChatColor.GREEN + "Kauf erfolgreich.");
                break;
            case NOT_ENOUGH_MONEY:
                player.sendMessage(ChatColor.RED + "Du hast nicht genug Coins.");
                break;
            case NOT_ENOUGH_NETHERSTARS:
                player.sendMessage(ChatColor.RED + "Du hast nicht genug physische Nethersterne im Inventar.");
                break;
            case INVENTORY_FULL:
                player.sendMessage(ChatColor.RED + "Dein Inventar ist voll.");
                break;
            default:
                player.sendMessage(ChatColor.RED + "Der Kauf konnte nicht abgeschlossen werden.");
                break;
        }
    }

    private ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null && lore.length > 0) meta.setLore(java.util.Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private String format(long value) {
        return String.format(java.util.Locale.GERMANY, "%,d", value);
    }
}
