package net.skykings.core.shop.rent;

import org.bukkit.Location;

import java.util.UUID;

/** Kleine Schnittstelle fuer PlayerShop-Placement ohne harte Kopplung an den Rental-Controller. */
public interface ShopRentalAccess {
    boolean hasActiveRental(UUID uuid, Location location);
    boolean isCurrentOrPreviousTenant(UUID uuid, Location location);
}
