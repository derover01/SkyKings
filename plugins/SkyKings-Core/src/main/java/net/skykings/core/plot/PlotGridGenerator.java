package net.skykings.core.plot;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/**
 * 1.8-kompatibler flacher Plot-Generator: 65x65 Baugrundstuecke mit 7 Block breiten Strassen.
 * Dadurch fuehlt sich SkyPlots wie eine klassische PlotSquared-Welt an statt wie einzelne Void-Inseln.
 */
public final class PlotGridGenerator extends ChunkGenerator {

    @Override
    @SuppressWarnings("deprecation")
    public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomes) {
        byte[][] result = new byte[world.getMaxHeight() / 16][];
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int globalX = chunkX * 16 + localX;
                int globalZ = chunkZ * 16 + localZ;
                boolean road = isRoad(globalX, globalZ);
                set(result, localX, 60, localZ, Material.BEDROCK);
                set(result, localX, 61, localZ, road ? Material.STONE : Material.DIRT);
                set(result, localX, 62, localZ, road ? Material.STONE : Material.DIRT);
                set(result, localX, 63, localZ, road ? Material.STONE : Material.DIRT);
                set(result, localX, 64, localZ, road ? Material.SMOOTH_BRICK : Material.GRASS);
            }
        }
        return result;
    }

    private boolean isRoad(int x, int z) {
        int lx = Math.floorMod(x, PlotService.SPACING);
        int lz = Math.floorMod(z, PlotService.SPACING);
        return lx >= PlotService.PLOT_SIZE || lz >= PlotService.PLOT_SIZE;
    }

    @SuppressWarnings("deprecation")
    private void set(byte[][] sections, int x, int y, int z, Material material) {
        int section = y >> 4;
        if (sections[section] == null) sections[section] = new byte[4096];
        int index = ((y & 0xF) << 8) | (z << 4) | x;
        sections[section][index] = (byte) material.getId();
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, PlotService.PLOT_SIZE / 2.0D, 65D, PlotService.PLOT_SIZE / 2.0D);
    }
}
