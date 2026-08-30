package net.skykings.core.cooldown;

import java.util.UUID;

/** Generischer, persistenter Cooldown-Service (UUID + String-Key -> Endzeitpunkt). */
public interface CooldownService {

    /** Lifecycle-Hook: vom PlayerLifecycleListener beim (Async-)Login aufgerufen. */
    void loadForPlayer(UUID uuid);

    /** Lifecycle-Hook: vom PlayerLifecycleListener bei PlayerQuit aufgerufen. */
    void unloadForPlayer(UUID uuid);

    boolean isActive(UUID uuid, String key);

    long getRemainingMillis(UUID uuid, String key);

    void set(UUID uuid, String key, long durationMillis);

    void remove(UUID uuid, String key);
}
