package net.skykings.core.island;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;

/** Besitzschutz fuer die SkyIslands-Welt. */
public final class IslandProtectionListener implements Listener {
    private final IslandService islands;

    public IslandProtectionListener(IslandService islands) { this.islands = islands; }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!islands.isIslandWorld(event.getBlock().getLocation())) return;
        if (allowed(event.getPlayer(), event.getBlock().getLocation())) return;
        event.setCancelled(true);
        deny(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!islands.isIslandWorld(event.getBlock().getLocation())) return;
        if (allowed(event.getPlayer(), event.getBlock().getLocation())) return;
        event.setCancelled(true);
        deny(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEmpty(PlayerBucketEmptyEvent event) {
        if (!islands.isIslandWorld(event.getBlockClicked().getLocation())) return;
        if (allowed(event.getPlayer(), event.getBlockClicked().getLocation())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFill(PlayerBucketFillEvent event) {
        if (!islands.isIslandWorld(event.getBlockClicked().getLocation())) return;
        if (allowed(event.getPlayer(), event.getBlockClicked().getLocation())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player) || !islands.isIslandWorld(event.getEntity().getLocation())) return;
        Player player = (Player) event.getRemover();
        if (allowed(player, event.getEntity().getLocation())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> islands.isIslandWorld(block.getLocation()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> islands.isIslandWorld(block.getLocation()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (islands.isIslandWorld(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (islands.isIslandWorld(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent event) {
        if (!islands.isIslandWorld(event.getBlock().getLocation())) return;
        IslandService.IslandData from = islands.findAt(event.getBlock().getLocation());
        IslandService.IslandData to = islands.findAt(event.getToBlock().getLocation());
        if (from == null || to == null || !from.owner.equals(to.owner)) event.setCancelled(true);
    }

    private boolean allowed(Player player, Location location) {
        return player.hasPermission("skykings.admin.island.bypass") || islands.canBuild(player.getUniqueId(), location);
    }

    private void deny(Player player) {
        player.sendMessage(ChatColor.RED + "Du darfst auf dieser Insel nicht bauen.");
    }
}
