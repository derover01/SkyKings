package net.skykings.core.plot;

import net.skykings.core.protection.MapProtectionService;
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

/** Schutz der SkyPlots-Welt. Staff-Bypass ist nur mit bewusst aktiviertem /buildmode möglich. */
public final class PlotProtectionListener implements Listener {
    private final PlotService plots;
    private final MapProtectionService mapProtection;

    public PlotProtectionListener(PlotService plots, MapProtectionService mapProtection) {
        this.plots = plots;
        this.mapProtection = mapProtection;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (canBypass(event.getPlayer())) return;
        if (!plots.canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (canBypass(event.getPlayer())) return;
        if (!plots.canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (canBypass(event.getPlayer())) return;
        if (event.getClickedBlock() == null) return;
        Material type = event.getClickedBlock().getType();
        if (!protectedInteraction(type)) return;
        if (!plots.canBuild(event.getPlayer(), event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent event) {
        if (canBypass(event.getPlayer())) return;
        if (!plots.canBuild(event.getPlayer(), event.getBlockClicked().getLocation())) {
            event.setCancelled(true);
            deny(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!plots.isPlotWorld(event.getLocation().getWorld())) return;
        if (plots.getPlotAt(event.getLocation()) == null || !plots.isExplosionsAllowed(event.getLocation())) {
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (!plots.isPlotWorld(event.getBlock().getWorld())) return;
        if (!plots.isFireAllowed(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (!plots.isPlotWorld(event.getBlock().getWorld())) return;
        if (!plots.isFireAllowed(event.getBlock().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (!plots.isPlotWorld(event.getLocation().getWorld())) return;
        if (plots.getPlotAt(event.getLocation()) == null || !plots.isMobSpawningAllowed(event.getLocation())) event.setCancelled(true);
    }

    private boolean canBypass(Player player) {
        return player != null && player.hasPermission("skykings.admin.plot.bypass") && mapProtection.canEdit(player);
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
