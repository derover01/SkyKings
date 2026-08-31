package net.skykings.core.plot;

import org.bukkit.ChatColor;
import org.bukkit.Location;
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

public final class PlotProtectionListener implements Listener {
    private final PlotService plots;
    public PlotProtectionListener(PlotService plots) { this.plots = plots; }

    @EventHandler(ignoreCancelled = true) public void onBreak(BlockBreakEvent e) { if (plots.isPlotWorld(e.getBlock().getLocation()) && !allowed(e.getPlayer(), e.getBlock().getLocation())) { e.setCancelled(true); deny(e.getPlayer()); } }
    @EventHandler(ignoreCancelled = true) public void onPlace(BlockPlaceEvent e) { if (plots.isPlotWorld(e.getBlock().getLocation()) && !allowed(e.getPlayer(), e.getBlock().getLocation())) { e.setCancelled(true); deny(e.getPlayer()); } }
    @EventHandler(ignoreCancelled = true) public void onEmpty(PlayerBucketEmptyEvent e) { if (plots.isPlotWorld(e.getBlockClicked().getLocation()) && !allowed(e.getPlayer(), e.getBlockClicked().getLocation())) e.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) public void onFill(PlayerBucketFillEvent e) { if (plots.isPlotWorld(e.getBlockClicked().getLocation()) && !allowed(e.getPlayer(), e.getBlockClicked().getLocation())) e.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) public void onExplode(EntityExplodeEvent e) { e.blockList().removeIf(b -> plots.isPlotWorld(b.getLocation())); }
    @EventHandler(ignoreCancelled = true) public void onBlockExplode(BlockExplodeEvent e) { e.blockList().removeIf(b -> plots.isPlotWorld(b.getLocation())); }
    @EventHandler(ignoreCancelled = true) public void onBurn(BlockBurnEvent e) { if (plots.isPlotWorld(e.getBlock().getLocation())) e.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) public void onIgnite(BlockIgniteEvent e) { if (plots.isPlotWorld(e.getBlock().getLocation())) e.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) public void onFlow(BlockFromToEvent e) {
        if (!plots.isPlotWorld(e.getBlock().getLocation())) return;
        PlotService.PlotData a = plots.findAt(e.getBlock().getLocation()), b = plots.findAt(e.getToBlock().getLocation());
        if (a == null || b == null || !a.owner.equals(b.owner)) e.setCancelled(true);
    }

    private boolean allowed(Player p, Location l) { return p.hasPermission("skykings.admin.plot.bypass") || plots.canBuild(p.getUniqueId(), l); }
    private void deny(Player p) { p.sendMessage(ChatColor.RED + "Du darfst auf diesem Plot nicht bauen."); }
}
