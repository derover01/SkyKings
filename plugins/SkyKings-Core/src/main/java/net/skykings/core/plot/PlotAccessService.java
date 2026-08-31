package net.skykings.core.plot;

import org.bukkit.Location;

import java.util.UUID;

public interface PlotAccessService {
    boolean isPlotWorld(Location location);
    boolean hasPlot(UUID owner);
    boolean canBuild(UUID player, Location location);
    boolean ownsLocation(UUID player, Location location);
}
