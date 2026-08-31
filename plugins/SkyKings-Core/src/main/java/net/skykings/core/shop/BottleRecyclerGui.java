package net.skykings.core.shop;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.sound.SoundFeedback;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/** Recycelt leere Glasflaschen aus PvP/Potion-Nutzung gegen kleine Coin-Betraege. */
public final class BottleRecyclerGui {

    private static final long COINS_PER_BOTTLE = 250L;

    private final GuiManager guiManager;
    private final EconomyService economyService;

    public BottleRecyclerGui(GuiManager guiManager, EconomyService economyService) {
        this.guiManager = guiManager;
        this.economyService = economyService;
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
        int bottles = count(player);
        if (bottles <= 0) {
            player.sendMessage(ChatColor.RED + "Du hast keine leeren Glasflaschen dabei.");
            SoundFeedback.error(player);
            return;
        }

        removeAll(player);
        long payout = bottles * COINS_PER_BOTTLE;
        economyService.deposit(player.getUniqueId(), payout, "BOTTLE_RECYCLER", bottles + " bottles");
        player.sendMessage(ChatColor.GREEN + "Recycelt: " + ChatColor.WHITE + bottles + ChatColor.GREEN
                + " Flaschen für " + ChatColor.GOLD + format(payout) + ChatColor.GREEN + " Coins.");
        SoundFeedback.reward(player);
        player.updateInventory();
        open(player);
    }

    private int count(Player player) {
        int amount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.GLASS_BOTTLE) amount += item.getAmount();
        }
        return amount;
    }

    private void removeAll(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item != null && item.getType() == Material.GLASS_BOTTLE) player.getInventory().setItem(slot, null);
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

    private String format(long value) {
        return String.format("%,d", value).replace(',', '.');
    }
}
