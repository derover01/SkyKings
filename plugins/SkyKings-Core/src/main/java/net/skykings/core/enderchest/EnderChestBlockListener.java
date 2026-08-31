package net.skykings.core.enderchest;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/** Oeffnet beim Rechtsklick auf eine normale Enderchest immer die mehrseitige SkyKings-Enderchest. */
public final class EnderChestBlockListener implements Listener {

    private final EnderChestService enderChestService;

    public EnderChestBlockListener(EnderChestService enderChestService) {
        this.enderChestService = enderChestService;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENDER_CHEST) return;

        Player player = event.getPlayer();
        event.setCancelled(true);
        enderChestService.open(player);
    }
}
