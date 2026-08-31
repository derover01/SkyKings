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

/** Blacksmith: repariert das gehaltene Item gegen Coins, abhängig vom tatsächlichen Schaden. */
public final class BlacksmithShopGui {

    private final GuiManager guiManager;
    private final EconomyService economyService;

    public BlacksmithShopGui(GuiManager guiManager, EconomyService economyService) {
        this.guiManager = guiManager;
        this.economyService = economyService;
    }

    public void open(Player player) {
        GuiSession gui = GuiSession.create(player, ChatColor.DARK_GRAY + "SkyKings | Blacksmith", 27);
        ItemStack hand = player.getItemInHand();
        long price = repairPrice(hand);

        gui.setItem(11, named(Material.ANVIL, ChatColor.GOLD + "Gegenstand reparieren",
                hand == null || hand.getType() == Material.AIR
                        ? ChatColor.RED + "Halte einen reparierbaren Gegenstand in der Hand."
                        : ChatColor.GRAY + "Preis: " + ChatColor.YELLOW + format(price) + " Coins",
                ChatColor.YELLOW + "Klicken zum Reparieren"), (p,e,s) -> repair(p));

        gui.setItem(15, named(Material.GOLD_INGOT, ChatColor.YELLOW + "Dein Kontostand",
                ChatColor.GRAY + format(economyService.getBalance(player.getUniqueId())) + " Coins"));
        guiManager.open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void repair(Player player) {
        ItemStack item = player.getItemInHand();
        if (!isRepairable(item)) {
            player.sendMessage(ChatColor.RED + "Halte einen beschädigten reparierbaren Gegenstand in der Hand.");
            SoundFeedback.error(player);
            return;
        }
        long price = repairPrice(item);
        if (!economyService.withdraw(player.getUniqueId(), price, player.getName(), "Blacksmith Repair")) {
            player.sendMessage(ChatColor.RED + "Dir fehlen Coins. Benötigt: " + ChatColor.YELLOW + format(price));
            SoundFeedback.error(player);
            return;
        }
        item.setDurability((short) 0);
        player.setItemInHand(item);
        player.updateInventory();
        player.sendMessage(ChatColor.GREEN + "Der Blacksmith hat deinen Gegenstand für "
                + ChatColor.YELLOW + format(price) + ChatColor.GREEN + " Coins repariert.");
        SoundFeedback.success(player);
        open(player);
    }

    private boolean isRepairable(ItemStack item) {
        return item != null && item.getType() != Material.AIR && item.getType().getMaxDurability() > 0 && item.getDurability() > 0;
    }

    private long repairPrice(ItemStack item) {
        if (!isRepairable(item)) return 0L;
        int max = item.getType().getMaxDurability();
        int damage = item.getDurability();
        double ratio = Math.min(1D, Math.max(0D, (double) damage / (double) max));
        long base;
        String name = item.getType().name();
        if (name.contains("DIAMOND")) base = 120000L;
        else if (name.contains("IRON")) base = 60000L;
        else if (name.contains("GOLD")) base = 45000L;
        else if (name.contains("CHAIN")) base = 40000L;
        else if (name.contains("STONE")) base = 15000L;
        else base = 25000L;
        return Math.max(5000L, Math.round(base * ratio));
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
