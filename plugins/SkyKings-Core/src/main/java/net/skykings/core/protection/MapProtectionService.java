package net.skykings.core.protection;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Schutz fuer Spawn-/SkyPvP-Welten. Standardspieler koennen dort nichts bauen oder zerstoeren.
 * Berechtigte Staffs koennen ihren persoenlichen Edit-Modus ueber /buildmode toggeln.
 */
public final class MapProtectionService implements Listener {

    public static final String BYPASS_PERMISSION = "skykings.admin.buildmode";

    private final Set<String> protectedWorlds = new HashSet<String>();
    private final Set<UUID> buildMode = new HashSet<UUID>();

    public MapProtectionService() { protectedWorlds.add("skypvp"); }

    public boolean isProtected(World world) {
        return world != null && protectedWorlds.contains(world.getName().toLowerCase());
    }

    public boolean canEdit(Player player) {
        return player != null && player.hasPermission(BYPASS_PERMISSION) && buildMode.contains(player.getUniqueId());
    }

    public boolean toggle(Player player) {
        UUID uuid = player.getUniqueId();
        if (buildMode.remove(uuid)) return false;
        buildMode.add(uuid);
        return true;
    }

    public Set<String> getProtectedWorlds() { return Collections.unmodifiableSet(protectedWorlds); }

    private boolean deny(Player player) {
        if (!isProtected(player.getWorld()) || canEdit(player)) return false;
        player.sendMessage(ChatColor.RED + "Diese Map ist geschuetzt. Staffs koennen /buildmode verwenden.");
        return true;
    }

    @EventHandler public void onBreak(BlockBreakEvent event) { if (deny(event.getPlayer())) event.setCancelled(true); }
    @EventHandler public void onPlace(BlockPlaceEvent event) { if (deny(event.getPlayer())) event.setCancelled(true); }
    @EventHandler public void onBucketEmpty(PlayerBucketEmptyEvent event) { if (deny(event.getPlayer())) event.setCancelled(true); }
    @EventHandler public void onBucketFill(PlayerBucketFillEvent event) { if (deny(event.getPlayer())) event.setCancelled(true); }
    @EventHandler public void onHangingPlace(HangingPlaceEvent event) { if (deny(event.getPlayer())) event.setCancelled(true); }
    @EventHandler public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player && deny((Player) event.getRemover())) event.setCancelled(true);
    }

    /** Spieler/Entities duerfen die Weizenfelder der Produktionsmap nicht zu Erde zertrampeln. */
    @EventHandler(ignoreCancelled = true)
    public void onFarmlandTrample(EntityChangeBlockEvent event) {
        if (!isProtected(event.getBlock().getWorld())) return;
        if (event.getBlock().getType() == Material.SOIL && event.getTo() == Material.DIRT) event.setCancelled(true);
    }

    @EventHandler public void onExplosion(EntityExplodeEvent event) {
        if (isProtected(event.getLocation().getWorld())) event.blockList().clear();
    }

    @EventHandler public void onBurn(BlockBurnEvent event) {
        if (isProtected(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler public void onIgnite(BlockIgniteEvent event) {
        if (isProtected(event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler
    public void onFlow(BlockFromToEvent event) {
        if (!isProtected(event.getBlock().getWorld())) return;
        Material from = event.getBlock().getType();
        if (from == Material.WATER || from == Material.STATIONARY_WATER || from == Material.LAVA || from == Material.STATIONARY_LAVA) {
            event.setCancelled(true);
        }
    }
}
