package net.skykings.core.shop.player;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Entkoppelt PlayerShops von konkreten Claim-/Rental-Systemen. */
public interface ShopPlacementPolicy {
    boolean canCreateShop(Player player, Location location);
    boolean canManageShop(Player player, PlayerShop shop);
    boolean canSellFromShop(PlayerShop shop);
}
