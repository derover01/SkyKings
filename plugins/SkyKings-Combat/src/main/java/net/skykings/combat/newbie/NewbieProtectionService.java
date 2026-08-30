package net.skykings.combat.newbie;

import java.util.UUID;

/**
 * Newbie Protection (siehe Auftrag Phase 2, Abschnitt 14): 20 Minuten PvP-Schutz ab
 * {@code PlayerProfile#getCreatedAt()}, permanent beendet durch den ersten eigenen Angriff.
 */
public interface NewbieProtectionService {

    boolean isProtected(UUID uuid);

    /** Beendet den Schutz permanent und persistiert das ueber Core (kein Effekt, falls bereits beendet). */
    void disableProtection(UUID uuid);
}
