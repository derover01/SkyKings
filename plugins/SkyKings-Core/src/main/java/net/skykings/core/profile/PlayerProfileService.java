package net.skykings.core.profile;

import net.skykings.core.model.PlayerProfile;

import java.util.UUID;

/** Verwaltet den Lifecycle von PlayerProfile-Instanzen. */
public interface PlayerProfileService {

    /** Laedt das Profil aus der Datenbank oder legt bei erstem Login ein neues an. */
    PlayerProfile loadOrCreate(UUID uuid, String currentName);

    /** Liefert das aktuell gecachte Profil, sonst null. */
    PlayerProfile getCached(UUID uuid);

    /**
     * Liefert ein bereits vorhandenes Profil aus Cache oder Persistenz, ohne einen neuen
     * Datensatz anzulegen. Einfache Test-Doubles bleiben standardmaessig cache-only.
     */
    default PlayerProfile loadExisting(UUID uuid) {
        return getCached(uuid);
    }

    /** Aktualisiert Namen + lastSeen (PlayerJoin) und speichert. */
    void updatePresence(UUID uuid, String currentName);

    /** Persistiert das gecachte Profil asynchron. */
    void save(UUID uuid);

    /**
     * Persistiert das aktuell gecachte Profil synchron und meldet, ob der Commit erfolgreich war.
     * Der Default bleibt fuer einfache Test-Doubles kompatibel; die Produktionsimplementierung
     * ueberschreibt diese Methode mit einem echten synchronen DataStore-Write.
     */
    default boolean saveNow(UUID uuid) {
        save(uuid);
        return true;
    }

    /** Speichert synchron und entfernt das Profil aus dem Cache (PlayerQuit). */
    void saveAndUnload(UUID uuid);

    /** Speichert synchron alle aktuell gecachten Profile (Plugin-Shutdown). */
    void saveAll();
}
