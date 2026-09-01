package net.skykings.core.plot;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/** Schutz der SkyPlots-Welt. Fremde Claims werden auch fuer OP/Wildcard-Spieler nicht still umgangen. */
public final class PlotProtectionListener implements Listener {
    private final PlotService plots;

    public PlotProtectionListener(PlotService plots) {
        this.plots = plots;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!plots.canBuild(event.getPlayer().getUniqueId(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!plots.canBuild(event.getPlayer().getUniqueId(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Material type = event.getClickedBlock().getType();
        if (!protectedInteraction(type)) return;
        if (!plots.canBuild(event.getPlayer().getUniqueId(), event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent event) {
        if (!plots.canBuild(event.getPlayer().getUniqueId(), event.getBlockClicked().getLocation())) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!plots.isPlotWorld(event.getLocation())) return;
        if (plots.findAt(event.getLocation()) == null || !plots.areExplosionsAllowed(event.getLocation())) {
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (!plots.isPlotWorld(event.getBlock().getLocation())) return;
        if (!plots.isFireAllowed(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (!plots.isPlotWorld(event.getBlock().getLocation())) return;
        if (!plots.isFireAllowed(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (!plots.isPlotWorld(event.getLocation())) return;
        PlotService.PlotData plot = plots.findAt(event.getLocation());
        if (plot == null || !plot.isMobSpawning()) event.setCancelled(true);
    }

    private boolean protectedInteraction(Material material) {
        String n = material.name();
        return n.contains("CHEST") || n.contains("DOOR") || n.contains("GATE") || n.contains("TRAP_DOOR")
                || n.contains("BUTTON") || n.contains("LEVER") || n.contains("PRESSURE_PLATE")
                || material == Material.FURNACE || material == Material.BURNING_FURNACE
                || material == Material.ANVIL || material == Material.BREWING_STAND
                || material == Material.ENCHANTMENT_TABLE || material == Material.HOPPER
                || material == Material.DISPENSER || material == Material.DROPPER;
    }

    private void deny(Player player) {
        player.sendMessage(ChatColor.RED + "Du hast auf diesem Plot keine Baurechte.");
    }
}
