package net.skykings.core.shop.player;

import net.skykings.core.island.IslandAccessService;
import net.skykings.core.plot.PlotAccessService;
import net.skykings.core.shop.rent.ShopRentalAccess;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

/** PlayerShops duerfen auf eigener Island, eigenem Plot oder aktiv gemietetem Marktstand stehen. */
public final class IslandShopPlacementPolicy implements ShopPlacementPolicy {
    private final IslandAccessService islands;
    private final PlotAccessService plots;
    private final ShopRentalAccess rentals;

    public IslandShopPlacementPolicy(IslandAccessService islands) { this(islands, null, null); }
    public IslandShopPlacementPolicy(IslandAccessService islands, PlotAccessService plots) { this(islands, plots, null); }
    public IslandShopPlacementPolicy(IslandAccessService islands, PlotAccessService plots, ShopRentalAccess rentals) {
        this.islands = islands;
        this.plots = plots;
        this.rentals = rentals;
    }

    @Override
    public boolean canCreateShop(Player player, Location location) {
        if (player == null || location == null) return false;
        UUID uuid = player.getUniqueId();
        return ownsPrivate(uuid, location) || (rentals != null && rentals.hasActiveRental(uuid, location));
    }

    @Override
    public boolean canManageShop(Player player, PlayerShop shop) {
        if (player == null || shop == null || !player.getUniqueId().equals(shop.getOwner())) return false;
        Location location = location(shop);
        if (location == null) return false;
        return ownsPrivate(player.getUniqueId(), location)
                || (rentals != null && rentals.isCurrentOrPreviousTenant(player.getUniqueId(), location));
    }

    @Override
    public boolean canSellFromShop(PlayerShop shop) {
        if (shop == null) return false;
        Location location = location(shop);
        if (location == null) return false;
        return ownsPrivate(shop.getOwner(), location)
                || (rentals != null && rentals.hasActiveRental(shop.getOwner(), location));
    }

    private boolean ownsPrivate(UUID uuid, Location location) {
        return islands.ownsLocation(uuid, location)
                || (plots != null && plots.ownsLocation(uuid, location));
    }

    private Location location(PlayerShop shop) {
        World world = Bukkit.getWorld(shop.getWorld());
        return world == null ? null : new Location(world, shop.getX(), shop.getY(), shop.getZ());
    }
}
