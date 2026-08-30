package net.skykings.combat.map.builder.v3;

import org.bukkit.Material;

import java.util.Random;

/** Hand-shaped terrain primitives for SkyKings V3. */
public final class V3Terrain {
    private V3Terrain() {}

    public static void island(V3Canvas c, int cx, int topY, int cz, int rx, int rz, int depth, long seed,
                              Material top, Material soil, Material core) {
        Random r = new Random(seed);
        for (int dx = -rx - 5; dx <= rx + 5; dx++) {
            for (int dz = -rz - 5; dz <= rz + 5; dz++) {
                double nx = dx / (double) rx;
                double nz = dz / (double) rz;
                double dist = Math.sqrt(nx * nx + nz * nz);
                double edgeNoise = Math.sin((dx + seed) * 0.17D) * 0.08D
                        + Math.cos((dz - seed) * 0.21D) * 0.07D
                        + Math.sin((dx + dz) * 0.09D) * 0.05D
                        + (r.nextDouble() - 0.5D) * 0.06D;
                if (dist > 1.0D + edgeNoise) continue;

                int surface = topY
                        + (int) Math.round(Math.sin(dx * 0.08D) * 2.2D)
                        + (int) Math.round(Math.cos(dz * 0.11D) * 1.8D)
                        + r.nextInt(3) - 1;
                double body = Math.max(0.0D, 1.0D - Math.pow(Math.min(1.0D, dist), 1.35D));
                int thickness = Math.max(5, (int) Math.round(5 + depth * body + r.nextDouble() * 4.0D));

                for (int y = surface; y >= surface - thickness; y--) {
                    int below = surface - y;
                    Material mat = below == 0 ? top : (below <= 3 ? soil : core);
                    if (below > 4 && r.nextInt(8) == 0) mat = Material.COBBLESTONE;
                    c.set(cx + dx, y, cz + dz, mat);
                }
            }
        }
    }

    public static void cliff(V3Canvas c, int cx, int y, int cz, int radius, int height, long seed) {
        Random r = new Random(seed);
        for (int dy = 0; dy < height; dy++) {
            int rr = Math.max(2, radius - dy / 3 + r.nextInt(3) - 1);
            c.disc(cx + r.nextInt(3) - 1, y + dy, cz + r.nextInt(3) - 1, rr,
                    dy % 5 == 0 ? Material.COBBLESTONE : Material.STONE);
        }
    }

    public static void rock(V3Canvas c, int cx, int y, int cz, int radius, long seed) {
        Random r = new Random(seed);
        for (int dy = 0; dy <= radius; dy++) {
            int rr = Math.max(1, radius - dy / 2 - r.nextInt(2));
            c.disc(cx + r.nextInt(3) - 1, y + dy, cz + r.nextInt(3) - 1, rr,
                    r.nextInt(4) == 0 ? Material.COBBLESTONE : Material.STONE);
        }
    }

    public static void waterfall(V3Canvas c, int x, int y, int z, int length) {
        for (int i = 0; i < length; i++) c.set(x, y - i, z, Material.WATER);
    }
}
