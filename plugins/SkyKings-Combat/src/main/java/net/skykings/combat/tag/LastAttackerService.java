package net.skykings.combat.tag;

import java.util.UUID;

/**
 * Merkt sich, wer ein Opfer zuletzt im PvP getroffen hat - unabhaengig von Bukkits eigenem,
 * kurzem {@code Player#getKiller()}-Zeitfenster (~5s). Wird fuer Combat-Logout gebraucht, da
 * das Combat-Tag-Fenster (Standard 15s) laenger sein kann (siehe Auftrag Phase 2, Abschnitt 6).
 */
public interface LastAttackerService {

    void recordAttack(UUID victim, UUID attacker);

    /** Liefert den zuletzt bekannten Angreifer, oder {@code null} falls unbekannt/abgelaufen. */
    UUID getLastAttacker(UUID victim);

    void clear(UUID victim);
}
