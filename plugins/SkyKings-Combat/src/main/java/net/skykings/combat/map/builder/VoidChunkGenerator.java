package net.skykings.combat.map.builder;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/** Leerer 1.8-kompatibler ChunkGenerator fuer die generierte SkyKings-SkyPvP-Welt. */
public final class VoidChunkGenerator extends ChunkGenerator {

    @Override
    @SuppressWarnings("deprecation")
    public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomes) {
        return new byte[world.getMaxHeight() / 16][];
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0.5D, 106.0D, 0.5D);
    }
}
