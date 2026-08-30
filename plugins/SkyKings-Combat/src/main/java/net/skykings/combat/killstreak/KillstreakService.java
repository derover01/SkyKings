package net.skykings.combat.killstreak;

import java.util.UUID;

/** Laufende Killstreaks; Persistenz wird durch den PvP-Stats-Service gespiegelt. */
public interface KillstreakService {

    int getStreak(UUID uuid);

    void reset(UUID uuid);

    /** Stellt eine persistierte Streak nach Serverstart wieder her. */
    void restore(UUID uuid, int streak);

    /** Erhöht den Streak des Killers um 1 und berechnet Reward inkl. Meilenstein-Bonus. */
    KillstreakResult recordKill(UUID killerUuid);
}
