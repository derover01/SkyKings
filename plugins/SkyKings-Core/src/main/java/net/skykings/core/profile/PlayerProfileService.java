package net.skykings.core.profile;

import net.skykings.core.model.PlayerProfile;

import java.util.UUID;

/**
 * Verwaltet den Lifecycle von {@link PlayerProfile}-Instanzen.
 *
 * <p>Phase-1A-Grenze: RankService/EconomyService/CooldownService arbeiten nur auf bereits
 * geladenen (= online) Profilen (siehe {@link #getCached(UUID)}). Offline-Bearbeitung
 * (z. B. ein spaeterer Admin-Befehl "/eco give OfflinePlayer") ist ein bewusst offener
 * Punkt fuer eine spaetere Phase, sobald es dafuer echte Aufrufer gibt.
 */
public interface PlayerProfileService {

    /** Laedt das Profil aus der Datenbank oder legt bei erstem Login ein neues an. */
    PlayerProfile loadOrCreate(UUID uuid, String currentName);

    /** Liefert das gecachte Profil eines online geladenen Spielers, sonst {@code null}. */
    PlayerProfile getCached(UUID uuid);

    /** Aktualisiert Namen + lastSeen (PlayerJoin) und speichert. */
    void updatePresence(UUID uuid, String currentName);

    /** Persistiert das gecachte Profil asynchron. */
    void save(UUID uuid);

    /** Speichert synchron und entfernt das Profil aus dem Cache (PlayerQuit). */
    void saveAndUnload(UUID uuid);

    /** Speichert synchron alle aktuell gecachten Profile (Plugin-Shutdown). */
    void saveAll();
}
