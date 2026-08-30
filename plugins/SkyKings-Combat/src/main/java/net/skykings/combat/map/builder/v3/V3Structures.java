package net.skykings.combat.map.builder.v3;

import org.bukkit.Material;

/** Architecture and nature modules for the hand-authored V3 world. */
public final class V3Structures {
    private V3Structures() {}

    public static void medievalHouse(V3Canvas c, int cx, int y, int cz, int width, int depth, boolean chimney) {
        int hx = width / 2;
        int hz = depth / 2;
        c.fill(cx - hx, y, cz - hz, cx + hx, y, cz + hz, Material.COBBLESTONE);

        // timber frame shell
        for (int dy = 1; dy <= 5; dy++) {
            for (int x = cx - hx; x <= cx + hx; x++) {
                c.set(x, y + dy, cz - hz, dy <= 2 ? Material.COBBLESTONE : Material.WOOD);
                c.set(x, y + dy, cz + hz, dy <= 2 ? Material.COBBLESTONE : Material.WOOD);
            }
            for (int z = cz - hz; z <= cz + hz; z++) {
                c.set(cx - hx, y + dy, z, dy <= 2 ? Material.COBBLESTONE : Material.WOOD);
                c.set(cx + hx, y + dy, z, dy <= 2 ? Material.COBBLESTONE : Material.WOOD);
            }
        }
        // vertical timber beams
        for (int dy = 1; dy <= 6; dy++) {
            c.set(cx - hx, y + dy, cz - hz, Material.LOG);
            c.set(cx + hx, y + dy, cz - hz, Material.LOG);
            c.set(cx - hx, y + dy, cz + hz, Material.LOG);
            c.set(cx + hx, y + dy, cz + hz, Material.LOG);
        }

        // door and windows
        c.set(cx, y + 1, cz - hz, Material.AIR);
        c.set(cx, y + 2, cz - hz, Material.AIR);
        c.set(cx, y + 3, cz - hz, Material.LOG);
        if (hx >= 3) {
            c.set(cx - 2, y + 3, cz - hz, Material.THIN_GLASS);
            c.set(cx + 2, y + 3, cz - hz, Material.THIN_GLASS);
        }

        // steep layered roof
        for (int layer = 0; layer <= hz + 1; layer++) {
            int zRadius = Math.max(0, hz + 1 - layer);
            c.fill(cx - hx - 1, y + 6 + layer, cz - zRadius,
                    cx + hx + 1, y + 6 + layer, cz + zRadius, Material.WOOD);
        }
        c.fill(cx - hx - 1, y + 6, cz - hz - 1, cx + hx + 1, y + 6, cz + hz + 1, Material.WOOD);

        // interior details
        c.set(cx - hx + 2, y + 1, cz + hz - 2, Material.CHEST);
        c.set(cx + hx - 2, y + 1, cz + hz - 2, Material.WORKBENCH);
        c.set(cx + hx - 2, y + 1, cz - hz + 2, Material.FURNACE);
        c.set(cx - hx + 2, y + 1, cz - hz + 2, Material.BOOKSHELF);

        if (chimney) {
            int x = cx + hx - 1;
            int z = cz + hz - 1;
            c.column(x, y + 1, z, 10, Material.BRICK);
            c.set(x, y + 11, z, Material.NETHERRACK);
            c.set(x, y + 12, z, Material.FIRE);
        }
    }

    public static void ruinedHouse(V3Canvas c, int cx, int y, int cz, int width, int depth) {
        medievalHouse(c, cx, y, cz, width, depth, false);
        int hx = width / 2;
        int hz = depth / 2;
        // deliberate broken walls and roof holes
        for (int dy = 3; dy <= 8; dy++) {
            c.set(cx + hx, y + dy, cz + hz - 1, Material.AIR);
            if (dy % 2 == 0) c.set(cx + hx - 1, y + dy, cz + hz, Material.AIR);
        }
        for (int i = 0; i < 5; i++) c.set(cx - 2 + i, y + 7, cz, Material.AIR);
        c.set(cx + hx + 1, y, cz + hz + 1, Material.COBBLESTONE);
        c.set(cx + hx + 2, y, cz + hz + 2, Material.COBBLESTONE);
    }

    public static void watchTower(V3Canvas c, int cx, int y, int cz, int radius, int height) {
        for (int dy = 0; dy < height; dy++) {
            int rr = radius;
            for (int x = -rr; x <= rr; x++) {
                for (int z = -rr; z <= rr; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d >= rr - 1.15D && d <= rr + 0.35D)
                        c.set(cx + x, y + dy, cz + z, dy % 5 == 0 ? Material.MOSSY_COBBLESTONE : Material.SMOOTH_BRICK);
                }
            }
        }
        c.disc(cx, y + height - 1, cz, radius, Material.SMOOTH_BRICK);
        c.ring(cx, y + height, cz, radius + 1, 2, Material.SMOOTH_BRICK);
        // crenellations
        for (int x = -radius; x <= radius; x += 2) {
            c.set(cx + x, y + height + 1, cz - radius, Material.SMOOTH_BRICK);
            c.set(cx + x, y + height + 1, cz + radius, Material.SMOOTH_BRICK);
        }
        for (int z = -radius; z <= radius; z += 2) {
            c.set(cx - radius, y + height + 1, cz + z, Material.SMOOTH_BRICK);
            c.set(cx + radius, y + height + 1, cz + z, Material.SMOOTH_BRICK);
        }
        // entrance
        c.set(cx, y + 1, cz - radius, Material.AIR);
        c.set(cx, y + 2, cz - radius, Material.AIR);
    }

    public static void brokenArch(V3Canvas c, int cx, int y, int cz, int width, int height) {
        int half = width / 2;
        c.fill(cx - half, y, cz, cx - half + 1, y + height, cz, Material.SMOOTH_BRICK);
        c.fill(cx + half - 1, y, cz, cx + half, y + height - 2, cz, Material.SMOOTH_BRICK);
        c.fill(cx - half, y + height, cz, cx + 1, y + height + 1, cz, Material.SMOOTH_BRICK);
        c.set(cx + 2, y + height, cz, Material.COBBLESTONE);
    }

    public static void marketStall(V3Canvas c, int cx, int y, int cz, boolean wide) {
        int hx = wide ? 4 : 3;
        c.fill(cx - hx, y, cz - 2, cx + hx, y, cz + 2, Material.WOOD);
        c.column(cx - hx, y + 1, cz - 2, 4, Material.FENCE);
        c.column(cx + hx, y + 1, cz - 2, 4, Material.FENCE);
        c.column(cx - hx, y + 1, cz + 2, 4, Material.FENCE);
        c.column(cx + hx, y + 1, cz + 2, 4, Material.FENCE);
        c.fill(cx - hx - 1, y + 5, cz - 3, cx + hx + 1, y + 5, cz + 3, Material.WOOD);
        c.set(cx - 1, y + 1, cz, Material.CHEST);
        c.set(cx + 1, y + 1, cz, Material.WORKBENCH);
    }

    public static void customTree(V3Canvas c, int x, int y, int z, int height) {
        c.column(x, y, z, height, Material.LOG);
        c.column(x + 1, y + 1, z, Math.max(2, height - 2), Material.LOG);
        c.column(x, y + 2, z + 1, Math.max(2, height - 3), Material.LOG);
        int crownY = y + height;
        c.disc(x, crownY, z, 4, Material.LEAVES);
        c.disc(x + 2, crownY + 1, z, 3, Material.LEAVES);
        c.disc(x - 2, crownY + 1, z + 1, 3, Material.LEAVES);
        c.disc(x, crownY + 2, z - 2, 3, Material.LEAVES);
        c.disc(x, crownY + 3, z, 2, Material.LEAVES);
        // roots
        c.fill(x - 4, y, z, x - 1, y, z, Material.LOG);
        c.fill(x + 1, y, z, x + 4, y, z, Material.LOG);
        c.fill(x, y, z - 4, x, y, z - 1, Material.LOG);
        c.fill(x, y, z + 1, x, y, z + 4, Material.LOG);
    }

    public static void stoneBridge(V3Canvas c, int x1, int y1, int z1, int x2, int y2, int z2, int width) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0D : i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            int half = width / 2;
            for (int dx = -half; dx <= half; dx++)
                for (int dz = -half; dz <= half; dz++) c.set(x + dx, y, z + dz, Material.SMOOTH_BRICK);
            if (i % 5 == 0) c.set(x, y - 1, z, Material.COBBLESTONE);
        }
    }
}
