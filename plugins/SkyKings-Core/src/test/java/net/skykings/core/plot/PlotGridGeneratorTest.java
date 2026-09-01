package net.skykings.core.plot;

import org.bukkit.Material;
import org.bukkit.World;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Regression fuer das sichtbare 65x65-Plot + 7-Block-Road-Raster. */
public class PlotGridGeneratorTest {

    @Test
    @SuppressWarnings("deprecation")
    public void roadStartsAfterBlock64AndNextPlotStartsAt72() {
        World world = mock(World.class);
        when(world.getMaxHeight()).thenReturn(256);
        PlotGridGenerator generator = new PlotGridGenerator();

        // Chunk 4 deckt global X 64..79 ab.
        byte[][] sections = generator.generateBlockSections(world, null, 4, 0, null);

        assertEquals(Material.WOOD_STEP.getId(), block(sections, 0, 64, 10));  // global x=64: letzter Plotblock/Rand
        assertEquals(Material.SMOOTH_BRICK.getId(), block(sections, 1, 64, 10)); // global x=65: Road 1/7
        assertEquals(Material.SMOOTH_BRICK.getId(), block(sections, 7, 64, 10)); // global x=71: Road 7/7
        assertEquals(Material.WOOD_STEP.getId(), block(sections, 8, 64, 10));  // global x=72: naechster Plotrand
        assertEquals(Material.GRASS.getId(), block(sections, 9, 64, 10));      // global x=73: Plot-Innenflaeche
    }

    @Test
    @SuppressWarnings("deprecation")
    public void crossingIsRoadAndPlotCornersRemainWoodBorder() {
        World world = mock(World.class);
        when(world.getMaxHeight()).thenReturn(256);
        PlotGridGenerator generator = new PlotGridGenerator();

        // Chunk 4/4 deckt global X/Z 64..79 ab.
        byte[][] sections = generator.generateBlockSections(world, null, 4, 4, null);

        assertEquals(Material.WOOD_STEP.getId(), block(sections, 0, 64, 0));
        assertEquals(Material.SMOOTH_BRICK.getId(), block(sections, 1, 64, 1));
        assertEquals(Material.SMOOTH_BRICK.getId(), block(sections, 7, 64, 7));
        assertEquals(Material.WOOD_STEP.getId(), block(sections, 8, 64, 8));
    }

    private int block(byte[][] sections, int x, int y, int z) {
        int section = y >> 4;
        int index = ((y & 0xF) << 8) | (z << 4) | x;
        return sections[section][index] & 0xFF;
    }
}
