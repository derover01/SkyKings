package net.skykings.combat.killstreak;

import java.util.UUID;

/**
 * Killstreak pro Spieler (siehe Auftrag Phase 2, Abschnitt 11). Nicht ueber Neustarts
 * persistiert (bewusste Phase-2-Entscheidung, siehe Abschlussbericht) - der In-Memory-Zustand
 * ist waehrend einer laufenden Server-Session ausreichend und vermeidet einen unnoetig grossen
 * Umbau; eine spaetere Persistenz ueber das Core-DataStore-Muster ist ohne Aenderung dieses
 * Interfaces moeglich.
 */
public interface KillstreakService {

    int getStreak(UUID uuid);

    void reset(UUID uuid);

    /** Erhoeht den Streak des Killers um 1 und berechnet den Reward inkl. evtl. Meilenstein-Bonus. */
    KillstreakResult recordKill(UUID killerUuid);
}
