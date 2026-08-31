package net.skykings.core.shop.player;

import net.skykings.core.island.IslandAccessService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** PlayerShops duerfen nur auf der eigenen privaten Insel erstellt/verwaltet werden. */
public final class IslandShopPlacementPolicy implements ShopPlacementPolicy {
    private final IslandAccessService islands;

    public IslandShopPlacementPolicy(IslandAccessService islands) { this.islands = islands; }

    @Override
    public boolean canCreateShop(Player player, Location location) {
        return player != null && location != null && islands.ownsLocation(player.getUniqueId(), location);
    }

    @Override
    public boolean canManageShop(Player player, PlayerShop shop) {
        if (player == null || shop == null || !player.getUniqueId().equals(shop.getOwner())) return false;
        org.bukkit.World world = Bukkit.getWorld(shop.getWorld());
        if (world == null) return false;
        Location location = new Location(world, shop.getX(), shop.getY(), shop.getZ());
        return islands.ownsLocation(player.getUniqueId(), location);
    }
}
