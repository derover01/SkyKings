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
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/** Schutz und Flags fuer die PlotSquared-inspirierte SkyPlots-Welt. */
public final class PlotProtectionListener implements Listener {
    private final PlotService plots;
    public PlotProtectionListener(PlotService plots) { this.plots = plots; }

    @EventHandler(ignoreCancelled = true) public void onBreak(BlockBreakEvent e) {
        if (plots.isPlotWorld(e.getBlock().getLocation()) && !allowed(e.getPlayer(), e.getBlock().getLocation())) { e.setCancelled(true); deny(e.getPlayer()); }
    }
    @EventHandler(ignoreCancelled = true) public void onPlace(BlockPlaceEvent e) {
        if (plots.isPlotWorld(e.getBlock().getLocation()) && !allowed(e.getPlayer(), e.getBlock().getLocation())) { e.setCancelled(true); deny(e.getPlayer()); }
    }
    @EventHandler(ignoreCancelled = true) public void onEmpty(PlayerBucketEmptyEvent e) {
        if (plots.isPlotWorld(e.getBlockClicked().getLocation()) && !allowed(e.getPlayer(), e.getBlockClicked().getLocation())) e.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true) public void onFill(PlayerBucketFillEvent e) {
        if (plots.isPlotWorld(e.getBlockClicked().getLocation()) && !allowed(e.getPlayer(), e.getBlockClicked().getLocation())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!plots.isPlotWorld(e.getTo())) return;
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;
        if (plots.canEnter(e.getPlayer().getUniqueId(), e.getTo()) || e.getPlayer().hasPermission("skykings.admin.plot.bypass")) return;
        e.setTo(e.getFrom());
        e.getPlayer().sendMessage(ChatColor.RED + "Du bist von diesem Plot ausgeschlossen.");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player) || !(e.getEntity() instanceof Player)) return;
        if (!plots.isPlotWorld(e.getEntity().getLocation())) return;
        if (!plots.isPvpAllowed(e.getEntity().getLocation())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (!plots.isPlotWorld(e.getLocation())) return;
        PlotService.PlotData plot = plots.findAt(e.getLocation());
        if (plot != null && !plot.isMobSpawning()) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> plots.isPlotWorld(b.getLocation()) && !plots.areExplosionsAllowed(b.getLocation()));
    }
    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> plots.isPlotWorld(b.getLocation()) && !plots.areExplosionsAllowed(b.getLocation()));
    }
    @EventHandler(ignoreCancelled = true) public void onBurn(BlockBurnEvent e) { if (plots.isPlotWorld(e.getBlock().getLocation())) e.setCancelled(true); }
    @EventHandler(ignoreCancelled = true) public void onIgnite(BlockIgniteEvent e) { if (plots.isPlotWorld(e.getBlock().getLocation())) e.setCancelled(true); }
    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        if (!plots.isPlotWorld(e.getBlock().getLocation())) return;
        PlotService.PlotData a = plots.findAt(e.getBlock().getLocation()), b = plots.findAt(e.getToBlock().getLocation());
        if (a == null || b == null || !a.owner.equals(b.owner)) e.setCancelled(true);
    }

    private boolean allowed(Player p, Location l) { return p.hasPermission("skykings.admin.plot.bypass") || plots.canBuild(p.getUniqueId(), l); }
    private void deny(Player p) { p.sendMessage(ChatColor.RED + "Du darfst auf diesem Plot nicht bauen."); }
}
