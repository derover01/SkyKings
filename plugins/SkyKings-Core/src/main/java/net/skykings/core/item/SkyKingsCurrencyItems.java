package net.skykings.core.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Map;

/** Zentrale Item-Identitaet fuer die physische SkyKings-Waehrung. */
public final class SkyKingsCurrencyItems {
    private SkyKingsCurrencyItems() { }

    public static ItemStack star(int amount) {
        int safe = Math.max(1, Math.min(64, amount));
        ItemStack item = new ItemStack(Material.NETHER_STAR, safe);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA.toString() + ChatColor.BOLD + "SkyKings " + ChatColor.WHITE + ChatColor.BOLD + "Stern");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Die physische Waehrung des Koenigreichs.",
                ChatColor.GRAY + "Verdiene sie im PvP, bei Events und Quests.",
                "",
                ChatColor.DARK_AQUA + "SkyKings Currency " + ChatColor.DARK_GRAY + "• handelbar"
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static void give(Player player, long amount) {
        long remaining = Math.max(0L, amount);
        while (remaining > 0L) {
            int stack = (int) Math.min(64L, remaining);
            Map<Integer, ItemStack> left = player.getInventory().addItem(star(stack));
            for (ItemStack item : left.values()) player.getWorld().dropItemNaturally(player.getLocation(), item);
            remaining -= stack;
        }
        player.updateInventory();
    }
}
