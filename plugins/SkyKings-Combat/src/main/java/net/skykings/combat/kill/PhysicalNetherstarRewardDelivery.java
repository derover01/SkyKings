package net.skykings.combat.kill;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/** Gibt PvP-Nethersterne als echte Items ins Inventar; Rest wird beim Spieler gedroppt. */
public final class PhysicalNetherstarRewardDelivery implements NetherstarRewardDelivery {

    @Override
    public void give(Player player, long amount) {
        if (player == null || amount <= 0L) return;

        long remaining = amount;
        while (remaining > 0L) {
            int stackAmount = (int) Math.min(64L, remaining);
            ItemStack stack = new ItemStack(Material.NETHER_STAR, stackAmount);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            remaining -= stackAmount;
        }
        player.updateInventory();
    }
}
