package net.skykings.combat.map.builder.v3;

import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

/** Lightweight queued block canvas for the hand-authored V3 map. */
public final class V3Canvas {
    public static final class BlockPlacement {
        public final int x, y, z;
        public final Material material;

        BlockPlacement(int x, int y, int z, Material material) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.material = material;
        }
    }

    private final World world;
    private final List<BlockPlacement> placements = new ArrayList<BlockPlacement>();

    public V3Canvas(World world) {
        this.world = world;
    }

    public World getWorld() {
        return world;
    }

    public List<BlockPlacement> getPlacements() {
        return placements;
    }

    public void set(int x, int y, int z, Material material) {
        if (material == null || y < 1 || y > 254) return;
        placements.add(new BlockPlacement(x, y, z, material));
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++) set(x, y, z, material);
    }

    public void disc(int cx, int y, int cz, int radius, Material material) {
        int rr = radius * radius;
        for (int x = -radius; x <= radius; x++)
            for (int z = -radius; z <= radius; z++)
                if (x * x + z * z <= rr) set(cx + x, y, cz + z, material);
    }

    public void ring(int cx, int y, int cz, int radius, int thickness, Material material) {
        int outer = radius * radius;
        int innerRadius = Math.max(0, radius - thickness);
        int inner = innerRadius * innerRadius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int d = x * x + z * z;
                if (d <= outer && d >= inner) set(cx + x, y, cz + z, material);
            }
        }
    }

    public void column(int x, int y, int z, int height, Material material) {
        for (int i = 0; i < height; i++) set(x, y + i, z, material);
    }

    public void hollowBox(int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ)
                        set(x, y, z, material);
                }
            }
        }
    }
}
