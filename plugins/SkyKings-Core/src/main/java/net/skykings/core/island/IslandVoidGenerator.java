package net.skykings.core.island;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/** Leerer 1.8-kompatibler Generator fuer die private SkyKings-Island-Welt. */
public final class IslandVoidGenerator extends ChunkGenerator {
    @Override
    @SuppressWarnings("deprecation")
    public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomes) {
        return new byte[world.getMaxHeight() / 16][];
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0.5D, 100D, 0.5D);
    }
}
