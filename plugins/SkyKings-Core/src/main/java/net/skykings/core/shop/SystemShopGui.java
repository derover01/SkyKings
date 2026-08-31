package net.skykings.core.shop;

import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Zentraler System-Shop im verbindlichen SkyKings UI-System. */
public final class SystemShopGui {
    private final GuiManager guiManager;
    private final ShopTransactionService transactions;

    public SystemShopGui(GuiManager guiManager, ShopTransactionService transactions) {
        this.guiManager = guiManager;
        this.transactions = transactions;
    }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, UiTheme.title("Shop"), 27);

        add(gui, player, 10, new ShopOffer("food-steak", new ItemStack(Material.COOKED_BEEF, 16), ShopCurrency.COINS, 25000L), "16x Steak", "SYSTEM");
        add(gui, player, 11, new ShopOffer("arrows", new ItemStack(Material.ARROW, 64), ShopCurrency.COINS, 50000L), "64x Pfeile", "SYSTEM");
        add(gui, player, 12, new ShopOffer("xp-bottles", new ItemStack(Material.EXP_BOTTLE, 16), ShopCurrency.COINS, 125000L), "16x XP-Flaschen", "SYSTEM");
        add(gui, player, 14, new ShopOffer("pvp-pearls", new ItemStack(Material.ENDER_PEARL, 16), ShopCurrency.NETHERSTARS, 3L), "16x Enderperlen", "PVP_RESTOCK");
        add(gui, player, 15, new ShopOffer("pvp-op-gaps", new ItemStack(Material.GOLDEN_APPLE, 4, (short) 1), ShopCurrency.NETHERSTARS, 8L), "4x OP-Gapple", "PVP_RESTOCK");

        gui.setItem(4, UiItems.item(Material.EMERALD,
                UiTheme.PRIMARY + "Market",
                UiTheme.MUTED + "Grundversorgung und PvP-Restock.",
                UiTheme.MUTED + "Coins und physische SkyKings Sterne."));
        gui.setItem(22, UiItems.item(Material.NETHER_STAR,
                UiTheme.TEXT + "Dein Guthaben",
                UiTheme.MUTED + "Coins " + UiTheme.TEXT + UiFormat.number(transactions.getCoinBalance(player.getUniqueId())),
                UiTheme.MUTED + "SkyKings Sterne " + UiTheme.TEXT + transactions.countNetherstars(player.getInventory())));

        guiManager.open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void add(GuiSession gui, Player player, int slot, ShopOffer offer, String displayName, String shopId) {
        ItemStack icon = offer.getItem();
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(UiTheme.TEXT + displayName);
        List<String> lore = new ArrayList<String>();
        long balance = offer.getCurrency() == ShopCurrency.COINS
                ? transactions.getCoinBalance(player.getUniqueId())
                : transactions.countNetherstars(player.getInventory());
        String unit = offer.getCurrency() == ShopCurrency.COINS ? " Coins" : " Sterne";
        lore.add(UiTheme.MUTED + "Preis " + UiTheme.TEXT + UiFormat.number(offer.getPrice()) + unit);
        lore.add(UiTheme.MUTED + "Dein Guthaben " + UiTheme.TEXT + UiFormat.number(balance) + unit);
        if (balance < offer.getPrice()) {
            lore.add(UiTheme.DANGER + "Dir fehlen " + UiFormat.number(offer.getPrice() - balance) + unit + ".");
            lore.add("");
            lore.add(UiTheme.DISABLED + "Nicht genug Guthaben");
        } else {
            lore.add("");
            lore.add(UiItems.action("Klicken zum Kaufen"));
        }
        meta.setLore(lore);
        icon.setItemMeta(meta);
        gui.setItem(slot, icon, (p, event, clickedSlot) -> {
            ShopPurchaseResult result = transactions.purchase(p, offer, shopId);
            sendResult(p, result, offer);
            if (result == ShopPurchaseResult.SUCCESS) open(p);
        });
    }

    private void sendResult(Player player, ShopPurchaseResult result, ShopOffer offer) {
        switch (result) {
            case SUCCESS:
                player.sendMessage(UiTheme.SUCCESS + "Kauf erfolgreich");
                player.sendMessage(UiTheme.MUTED + UiFormat.number(offer.getPrice())
                        + (offer.getCurrency() == ShopCurrency.COINS ? " Coins" : " SkyKings Sterne") + " wurden verwendet.");
                SoundFeedback.success(player);
                break;
            case NOT_ENOUGH_MONEY:
                player.sendMessage(UiTheme.DANGER + "Nicht genug Coins.");
                SoundFeedback.error(player);
                break;
            case NOT_ENOUGH_NETHERSTARS:
                player.sendMessage(UiTheme.DANGER + "Nicht genug SkyKings Sterne.");
                SoundFeedback.error(player);
                break;
            case INVENTORY_FULL:
                player.sendMessage(UiTheme.DANGER + "Inventar voll.");
                SoundFeedback.error(player);
                break;
            default:
                player.sendMessage(UiTheme.DANGER + "Kauf nicht moeglich.");
                SoundFeedback.error(player);
                break;
        }
    }
}
