package net.skykings.combat.stats;

import net.skykings.core.pvp.PvpStatsProvider;

import java.util.UUID;

public interface PvpStatsTracker extends PvpStatsProvider {
    void recordDeath(UUID victimUuid);
    void recordKill(UUID killerUuid, int newStreak);
}
