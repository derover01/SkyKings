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

/** PvP-Restock-Shop ausschliesslich gegen physische SkyKings Sterne. */
public final class PvpRestockShopGui {
    private final GuiManager guiManager;
    private final ShopTransactionService transactions;

    public PvpRestockShopGui(GuiManager guiManager, ShopTransactionService transactions) {
        this.guiManager = guiManager;
        this.transactions = transactions;
    }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, UiTheme.title("PvP Restock"), 27);
        add(gui, player, 10, new ShopOffer("pearls-16", new ItemStack(Material.ENDER_PEARL, 16), ShopCurrency.NETHERSTARS, 3L), "16x Enderperlen");
        add(gui, player, 11, new ShopOffer("op-gaps-4", new ItemStack(Material.GOLDEN_APPLE, 4, (short) 1), ShopCurrency.NETHERSTARS, 8L), "4x OP-Gapple");
        add(gui, player, 12, new ShopOffer("op-gaps-16", new ItemStack(Material.GOLDEN_APPLE, 16, (short) 1), ShopCurrency.NETHERSTARS, 28L), "16x OP-Gapple");
        add(gui, player, 14, new ShopOffer("arrows-64", new ItemStack(Material.ARROW, 64), ShopCurrency.NETHERSTARS, 2L), "64x Pfeile");
        add(gui, player, 15, new ShopOffer("xp-32", new ItemStack(Material.EXP_BOTTLE, 32), ShopCurrency.NETHERSTARS, 4L), "32x XP-Flaschen");
        add(gui, player, 16, new ShopOffer("golden-apples-16", new ItemStack(Material.GOLDEN_APPLE, 16), ShopCurrency.NETHERSTARS, 3L), "16x Goldene Aepfel");

        gui.setItem(4, UiItems.item(Material.DIAMOND_SWORD,
                UiTheme.PRIMARY + "PvP Restock",
                UiTheme.MUTED + "Schneller Nachschub fuer Open-World-PvP.",
                UiTheme.MUTED + "Bezahlung nur mit physischen SkyKings Sternen."));
        gui.setItem(22, UiItems.item(Material.NETHER_STAR,
                UiTheme.TEXT + "SkyKings Sterne",
                UiTheme.MUTED + "Im Inventar",
                UiTheme.TEXT.toString() + transactions.countNetherstars(player.getInventory()),
                UiTheme.MUTED + "Verdienst durch PvP, Streaks, Crates und Events."));
        guiManager.open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void add(GuiSession gui, Player player, int slot, ShopOffer offer, String displayName) {
        ItemStack icon = offer.getItem();
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(UiTheme.TEXT + displayName);
        int balance = transactions.countNetherstars(player.getInventory());
        List<String> lore = new ArrayList<String>();
        lore.add(UiTheme.MUTED + "Preis " + UiTheme.TEXT + UiFormat.number(offer.getPrice()) + " Sterne");
        lore.add(UiTheme.MUTED + "Dein Bestand " + UiTheme.TEXT + UiFormat.number(balance));
        if (balance < offer.getPrice()) {
            lore.add(UiTheme.DANGER + "Dir fehlen " + UiFormat.number(offer.getPrice() - balance) + " Sterne.");
            lore.add("");
            lore.add(UiTheme.DISABLED + "Nicht genug Sterne");
        } else {
            lore.add("");
            lore.add(UiItems.action("Klicken zum Kaufen"));
        }
        meta.setLore(lore);
        icon.setItemMeta(meta);
        gui.setItem(slot, icon, (p, event, clickedSlot) -> {
            ShopPurchaseResult result = transactions.purchase(p, offer, "PVP_RESTOCK");
            switch (result) {
                case SUCCESS:
                    p.sendMessage(UiTheme.SUCCESS + "Restock gekauft");
                    SoundFeedback.success(p);
                    open(p);
                    break;
                case NOT_ENOUGH_NETHERSTARS:
                    p.sendMessage(UiTheme.DANGER + "Nicht genug SkyKings Sterne.");
                    SoundFeedback.error(p);
                    break;
                case INVENTORY_FULL:
                    p.sendMessage(UiTheme.DANGER + "Inventar voll.");
                    SoundFeedback.error(p);
                    break;
                default:
                    p.sendMessage(UiTheme.DANGER + "Kauf nicht moeglich.");
                    SoundFeedback.error(p);
                    break;
            }
        });
    }
}
