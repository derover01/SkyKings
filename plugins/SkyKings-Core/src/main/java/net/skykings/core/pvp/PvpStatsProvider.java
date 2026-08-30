package net.skykings.core.pvp;

import java.util.UUID;

/** Service contract implemented by SkyKings-Combat for scoreboard/other modules. */
public interface PvpStatsProvider {
    PvpStatsSnapshot getStats(UUID uuid);
}
