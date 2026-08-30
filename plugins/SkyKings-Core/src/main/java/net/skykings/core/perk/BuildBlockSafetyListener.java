package net.skykings.core.perk;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

/** Verhindert, dass kostenlose /blöcke-Items in normale Economy-Items umgecraftet werden. */
public final class BuildBlockSafetyListener implements Listener {

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        if (matrix == null) return;
        for (ItemStack item : matrix) {
            if (!BuildBlocksGui.isNoSellBuildBlock(item)) continue;
            event.getInventory().setResult(null);
            if (event.getView().getPlayer() instanceof Player) {
                ((Player) event.getView().getPlayer()).sendMessage(ChatColor.RED
                        + "Kostenlose SkyKings-Baublöcke können nicht umgecraftet werden.");
            }
            return;
        }
    }
}
