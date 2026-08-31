package net.skykings.combat.map.zone;

import org.bukkit.Location;
import org.bukkit.World;

/** Persistente kreisförmige Gameplay-Zone auf einer Map. */
public final class MapZone {
    private final String id;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final double radius;

    public MapZone(String id, String world, double x, double y, double z, double radius) {
        this.id = id;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = Math.max(1D, radius);
    }

    public String getId() { return id; }
    public String getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getRadius() { return radius; }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) return false;
        if (!world.equalsIgnoreCase(location.getWorld().getName())) return false;
        double dx = location.getX() - x;
        double dz = location.getZ() - z;
        return dx * dx + dz * dz <= radius * radius;
    }

    public Location center(World resolvedWorld) {
        return new Location(resolvedWorld, x, y, z);
    }
}
