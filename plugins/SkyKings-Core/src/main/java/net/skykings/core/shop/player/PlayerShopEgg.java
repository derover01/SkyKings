package net.skykings.core.shop.player;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Stackbares Villager-Spawn-Ei zum Platzieren eines PlayerShops. */
public final class PlayerShopEgg {
    private static final String MARKER = ChatColor.BLACK + "skykings:playershop-egg:";
    private static final String STACK_MARKER = MARKER + "v2";

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.MONSTER_EGG, 1, (short) 120);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD.toString() + ChatColor.BOLD + "SkyKings " + ChatColor.YELLOW + ChatColor.BOLD + "Haendler-Ei");
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Erschafft deinen persoenlichen PlayerShop.");
        lore.add("");
        lore.add(ChatColor.AQUA + "• Nur auf eigener Insel / eigenem Plot");
        lore.add(ChatColor.AQUA + "• Rechtsklick auf einen Block zum Platzieren");
        lore.add(ChatColor.AQUA + "• Einmalig verbrauchbar");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "SkyKings Handelslizenz");
        // Keine per-Item UUID: identische Haendler-Eier muessen normal stacken koennen.
        // Alte UUID-Marker bleiben durch startsWith(MARKER) weiterhin voll kompatibel.
        lore.add(STACK_MARKER);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isShopEgg(ItemStack item) {
        if (item == null || item.getType() != Material.MONSTER_EGG || item.getDurability() != 120 || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        for (String line : meta.getLore()) if (line != null && line.startsWith(MARKER)) return true;
        return false;
    }
}
