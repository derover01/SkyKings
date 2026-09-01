package net.skykings.core.shop.rent;

import net.skykings.core.shop.player.PlayerShop;
import net.skykings.core.shop.player.ShopPlacementPolicy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** Erweitert die bestehende Island/Plot-Policy additiv um aktive Marktstand-Mieten. */
public final class RentalAwareShopPlacementPolicy implements ShopPlacementPolicy {
    private final ShopPlacementPolicy delegate;
    private final ShopRentalAccess rentals;

    public RentalAwareShopPlacementPolicy(ShopPlacementPolicy delegate, ShopRentalAccess rentals) {
        this.delegate = delegate;
        this.rentals = rentals;
    }

    @Override
    public boolean canCreateShop(Player player, Location location) {
        if (delegate != null && delegate.canCreateShop(player, location)) return true;
        return player != null && rentals != null && rentals.hasActiveRental(player.getUniqueId(), location);
    }

    @Override
    public boolean canManageShop(Player player, PlayerShop shop) {
        if (delegate != null && delegate.canManageShop(player, shop)) return true;
        if (player == null || shop == null || !player.getUniqueId().equals(shop.getOwner()) || rentals == null) return false;
        Location location = location(shop);
        return location != null && rentals.isCurrentOrPreviousTenant(player.getUniqueId(), location);
    }

    @Override
    public boolean canSellFromShop(PlayerShop shop) {
        if (delegate != null && delegate.canSellFromShop(shop)) return true;
        if (shop == null || rentals == null) return false;
        Location location = location(shop);
        return location != null && rentals.hasActiveRental(shop.getOwner(), location);
    }

    private Location location(PlayerShop shop) {
        if (shop.getWorld() == null) return null;
        World world = Bukkit.getWorld(shop.getWorld());
        return world == null ? null : new Location(world, shop.getX(), shop.getY(), shop.getZ());
    }
}
