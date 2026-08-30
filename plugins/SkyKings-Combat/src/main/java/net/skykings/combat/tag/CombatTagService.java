package net.skykings.combat.tag;

import java.util.UUID;

/**
 * Combat Tag (siehe Auftrag Phase 2, Abschnitt 5). Zeitstempel-basiert - kein Task pro Spieler.
 */
public interface CombatTagService {

    void tag(UUID uuid);

    void tagBoth(UUID a, UUID b);

    boolean isTagged(UUID uuid);

    long getRemainingMillis(UUID uuid);

    void clear(UUID uuid);
}
