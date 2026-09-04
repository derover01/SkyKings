package net.skykings.core.cooldown;

import java.util.UUID;

/**
 * Generischer, persistenter Cooldown-Service (UUID + String-Key -> Endzeitpunkt).
 *
 * <p>{@link #isActive(UUID, String)} und {@link #getRemainingMillis(UUID, String)} lesen
 * ausschliesslich aus dem In-Memory-Cache und loesen KEINEN Datenbank-Zugriff aus - dafuer muss
 * {@link #loadForPlayer(UUID)} vorher aufgerufen worden sein (passiert automatisch beim Login
 * ueber den PlayerLifecycleListener). Ohne vorheriges Laden liefern beide Methoden einfach
 * "kein aktiver Cooldown" statt einen Fehler zu werfen.
 */
public interface CooldownService {

    /** Lifecycle-Hook: vom PlayerLifecycleListener beim (Async-)Login aufgerufen, laedt alle Cooldowns. */
    void loadForPlayer(UUID uuid);

    /** Lifecycle-Hook: vom PlayerLifecycleListener bei PlayerQuit aufgerufen. */
    void unloadForPlayer(UUID uuid);

    boolean isActive(UUID uuid, String key);

    long getRemainingMillis(UUID uuid, String key);

    void set(UUID uuid, String key, long durationMillis);

    /**
     * Setzt und persistiert einen Cooldown synchron. Nur fuer Transaktionsgrenzen wie Kit-Claims,
     * bei denen Item-Vergabe und Cooldown nach einem Hard-Crash nicht auseinanderlaufen duerfen.
     */
    default boolean setNow(UUID uuid, String key, long durationMillis) {
        set(uuid, key, durationMillis);
        return true;
    }

    void remove(UUID uuid, String key);
}
