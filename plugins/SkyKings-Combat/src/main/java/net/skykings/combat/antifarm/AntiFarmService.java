package net.skykings.combat.antifarm;

import java.util.UUID;

/**
 * Verhindert wiederholtes Farmen desselben Gegners (siehe Auftrag Phase 2, Abschnitt 12).
 * Nicht IP-basiert, keine Alt-Erkennung. Der Farm-Zaehler fuer einen Killer wird
 * zurueckgesetzt, sobald er zwischendurch ein anderes Opfer toetet.
 */
public interface AntiFarmService {

    /**
     * Registriert einen Kill von {@code killer} gegen {@code victim} und liefert den
     * Reward-Multiplikator (1.0 = voller Reward, 0.0 = kein Reward) fuer GENAU diesen Kill.
     * Der Kill selbst (Killstreak etc.) zaehlt unabhaengig vom Multiplikator immer voll.
     */
    double registerKillAndGetMultiplier(UUID killer, UUID victim);

    void clear(UUID killer);
}
