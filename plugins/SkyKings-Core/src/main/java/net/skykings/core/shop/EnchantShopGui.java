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

/** Enchant-Shop: XP/Lapis-Nachschub und direkter Zugriff auf den Enchanting Table. */
public final class EnchantShopGui {

    private final GuiManager guiManager;
    private final ShopTransactionService transactions;

    public EnchantShopGui(GuiManager guiManager, ShopTransactionService transactions) {
        this.guiManager = guiManager;
        this.transactions = transactions;
    }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_PURPLE + "SkyKings | Enchant", 27);

        gui.setItem(10, icon(Material.EXP_BOTTLE, 16, ChatColor.AQUA + "16x XP-Flasche",
                ChatColor.GRAY + "Preis: " + ChatColor.YELLOW + "35.000 Coins"),
                (p,e,s) -> buy(p, new ShopOffer("enchant-xp16", new ItemStack(Material.EXP_BOTTLE, 16), ShopCurrency.COINS, 35000L)));

        gui.setItem(12, icon(Material.INK_SACK, 16, (short) 4, ChatColor.BLUE + "16x Lapis",
                ChatColor.GRAY + "Preis: " + ChatColor.YELLOW + "25.000 Coins"),
                (p,e,s) -> buy(p, new ShopOffer("enchant-lapis16", new ItemStack(Material.INK_SACK, 16, (short) 4), ShopCurrency.COINS, 25000L)));

        gui.setItem(14, icon(Material.EXP_BOTTLE, 64, ChatColor.LIGHT_PURPLE + "64x XP-Flasche",
                ChatColor.GRAY + "Preis: " + ChatColor.YELLOW + "120.000 Coins"),
                (p,e,s) -> buy(p, new ShopOffer("enchant-xp64", new ItemStack(Material.EXP_BOTTLE, 64), ShopCurrency.COINS, 120000L)));

        gui.setItem(16, icon(Material.ENCHANTMENT_TABLE, 1, ChatColor.GOLD + "Verzauberungstisch öffnen",
                ChatColor.GRAY + "Direkter Zugriff auf den Enchanting Table"),
                (p,e,s) -> {
                    p.closeInventory();
                    p.openEnchanting(p.getLocation(), true);
                    SoundFeedback.menuOpen(p);
                });

        guiManager.open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void buy(Player player, ShopOffer offer) {
        ShopPurchaseResult result = transactions.purchase(player, offer, "enchant");
        if (result == ShopPurchaseResult.SUCCESS) {
            player.sendMessage(ChatColor.GREEN + "Kauf erfolgreich.");
            SoundFeedback.success(player);
            open(player);
            return;
        }
        if (result == ShopPurchaseResult.NOT_ENOUGH_MONEY) player.sendMessage(ChatColor.RED + "Du hast nicht genug Coins.");
        else if (result == ShopPurchaseResult.INVENTORY_FULL) player.sendMessage(ChatColor.RED + "Dein Inventar ist voll.");
        else player.sendMessage(ChatColor.RED + "Der Kauf konnte nicht abgeschlossen werden.");
        SoundFeedback.error(player);
    }

    private ItemStack icon(Material material, int amount, String name, String... lore) {
        return icon(material, amount, (short) 0, name, lore);
    }

    private ItemStack icon(Material material, int amount, short data, String name, String... lore) {
        ItemStack item = new ItemStack(material, amount, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}
