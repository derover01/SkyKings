package net.skykings.core.shop.player;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Entkoppelt PlayerShops von konkreten Island-/Plot-Plugins.
 * Phase 7 kann diese Policy durch eine echte Owner/Member-Pruefung ersetzen.
 */
public interface ShopPlacementPolicy {
    boolean canCreateShop(Player player, Location location);
    boolean canManageShop(Player player, PlayerShop shop);
}
