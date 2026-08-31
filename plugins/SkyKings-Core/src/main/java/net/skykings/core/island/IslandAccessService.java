package net.skykings.core.island;

import org.bukkit.Location;

import java.util.UUID;

/** Kleine API fuer Besitz-/Baurechtspruefungen, z. B. PlayerShops. */
public interface IslandAccessService {
    boolean isIslandWorld(Location location);
    boolean hasIsland(UUID owner);
    boolean canBuild(UUID player, Location location);
    boolean ownsLocation(UUID player, Location location);
}
