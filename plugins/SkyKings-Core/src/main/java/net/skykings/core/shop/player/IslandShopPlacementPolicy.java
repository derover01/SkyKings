package net.skykings.core.shop.player;

import net.skykings.core.island.IslandAccessService;
import net.skykings.core.plot.PlotAccessService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** PlayerShops duerfen nur auf eigener privater Insel oder eigenem Plot erstellt/verwaltet werden. */
public final class IslandShopPlacementPolicy implements ShopPlacementPolicy {
    private final IslandAccessService islands;
    private final PlotAccessService plots;

    public IslandShopPlacementPolicy(IslandAccessService islands) { this(islands, null); }
    public IslandShopPlacementPolicy(IslandAccessService islands, PlotAccessService plots) { this.islands = islands; this.plots = plots; }

    @Override
    public boolean canCreateShop(Player player, Location location) {
        if (player == null || location == null) return false;
        return islands.ownsLocation(player.getUniqueId(), location)
                || (plots != null && plots.ownsLocation(player.getUniqueId(), location));
    }

    @Override
    public boolean canManageShop(Player player, PlayerShop shop) {
        if (player == null || shop == null || !player.getUniqueId().equals(shop.getOwner())) return false;
        org.bukkit.World world = Bukkit.getWorld(shop.getWorld());
        if (world == null) return false;
        Location location = new Location(world, shop.getX(), shop.getY(), shop.getZ());
        return islands.ownsLocation(player.getUniqueId(), location)
                || (plots != null && plots.ownsLocation(player.getUniqueId(), location));
    }
}
