package net.skykings.core.perk;

import net.skykings.core.item.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;

/** Hält kostenlose /blöcke-Items auch nach Platzieren/Abbauen von der Economy getrennt. */
public final class BuildBlockWorldListener implements Listener {

    private final BuildBlockStore store;

    public BuildBlockWorldListener(BuildBlockStore store) {
        this.store = store;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack hand = event.getItemInHand();
        if (!BuildBlocksGui.isNoSellBuildBlock(hand)) return;
        Block block = event.getBlockPlaced();
        if (!store.putNow(block.getLocation(), block.getType(), block.getData())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Der Gratis-Baublock konnte nicht sicher gespeichert werden. Platzierung abgebrochen.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        BuildBlockStore.Entry entry = store.get(event.getBlock().getLocation());
        if (entry == null) return;
        event.setCancelled(true);

        Block block = event.getBlock();
        Material previousType = block.getType();
        byte previousData = block.getData();
        block.setType(Material.AIR);
        try {
            block.getWorld().save();
        } catch (RuntimeException ex) {
            block.setType(previousType);
            block.setData(previousData);
            event.getPlayer().sendMessage(ChatColor.RED + "Der Gratis-Baublock konnte nicht sicher abgebaut werden.");
            return;
        }

        // Marker erst entfernen, nachdem der Blockzustand durable AIR ist. Bei Store-Fehler gibt es
        // deshalb bewusst noch keinen Reward: Ghost-Marker ist sicherer als ein unmarkierter Gratisblock.
        if (!store.removeNow(block.getLocation())) {
            event.getPlayer().sendMessage(ChatColor.RED + "Der Gratis-Baublock wurde entfernt, aber der Marker braucht Staff-Pruefung.");
            return;
        }

        ItemStack item = new ItemBuilder(entry.getMaterial(), 1)
                .durability(entry.getData())
                .lore(BuildBlocksGui.NO_SELL_LORE)
                .build();
        Player player = event.getPlayer();
        if (!player.getInventory().addItem(item).isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
        player.sendMessage(ChatColor.GRAY + "Kostenloser Baublock bleibt als SkyKings-Baublöcke markiert.");
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        protect(event.blockList().iterator());
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        protect(event.blockList().iterator());
    }

    private void protect(Iterator<Block> blocks) {
        while (blocks.hasNext()) {
            if (store.contains(blocks.next().getLocation())) blocks.remove();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (store.contains(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        Block moved = event.getRetractLocation().getBlock();
        if (store.contains(moved.getLocation())) event.setCancelled(true);
    }
}
