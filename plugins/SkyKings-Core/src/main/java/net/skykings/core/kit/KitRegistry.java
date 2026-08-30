package net.skykings.core.kit;

import java.util.Collection;
import java.util.Optional;

/**
 * Rein technische Registry fuer {@link KitDefinition}s (Phase 1B). Enthaelt bewusst keine
 * Grant-/Vergabelogik und keine Kit-Inhalte - das ist Aufgabe einer spaeteren Phase.
 */
public interface KitRegistry {

    /** Registriert ein Kit. Wirft bei einer bereits vergebenen ID (siehe {@link KitDefinition#getId()}). */
    void register(KitDefinition kit);

    Optional<KitDefinition> get(String id);

    boolean contains(String id);

    Collection<KitDefinition> getAll();

    /** Entfernt ein registriertes Kit, falls vorhanden. */
    void unregister(String id);
}
