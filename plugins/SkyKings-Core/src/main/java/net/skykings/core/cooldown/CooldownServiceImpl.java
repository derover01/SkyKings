package net.skykings.core.cooldown;

import net.skykings.core.storage.DataStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Haelt Cooldowns ausschliesslich im Speicher, solange ein Spieler online ist.
 *
 * <p>{@link #loadForPlayer(UUID)} laedt beim (Async-)Login ALLE persistierten Cooldowns eines
 * Spielers auf einmal (siehe {@link DataStore#loadCooldowns(UUID)}). Danach greifen
 * {@link #isActive(UUID, String)} und {@link #getRemainingMillis(UUID, String)} ausschliesslich
 * auf den Cache zu - kein synchroner Datenbank-Read mehr im Gameplay-Pfad, auch nicht fuer
 * bislang unbekannte Keys. {@link #set(UUID, String, long)} und {@link #remove(UUID, String)}
 * schreiben weiterhin asynchron durch, damit ein Serverneustart ueberstanden wird.
 *
 * <p>Bereits beim Laden abgelaufene Cooldowns werden nicht in den Cache uebernommen und
 * asynchron aus der Datenbank entfernt; waehrend der Session abgelaufene Eintraege werden bei
 * Zugriff lazy aus dem Cache entfernt (kein zusaetzlicher DB-Zugriff dafuer noetig).
 */
public final class CooldownServiceImpl implements CooldownService {

    private final DataStore dataStore;
    private final ExecutorService dbExecutor;
    private final Logger logger;
    private final Map<UUID, Map<String, Long>> cache = new ConcurrentHashMap<>();

    public CooldownServiceImpl(DataStore dataStore, ExecutorService dbExecutor, Logger logger) {
        this.dataStore = dataStore;
        this.dbExecutor = dbExecutor;
        this.logger = logger;
    }

    @Override
    public void loadForPlayer(UUID uuid) {
        Map<String, Long> persisted;
        try {
            persisted = dataStore.loadCooldowns(uuid);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Konnte Cooldowns nicht laden: " + uuid, e);
            persisted = Collections.emptyMap();
        }

        long now = System.currentTimeMillis();
        Map<String, Long> active = new ConcurrentHashMap<>();
        List<String> expiredKeys = new ArrayList<>();
        for (Map.Entry<String, Long> entry : persisted.entrySet()) {
            if (entry.getValue() > now) {
                active.put(entry.getKey(), entry.getValue());
            } else {
                expiredKeys.add(entry.getKey());
            }
        }
        cache.put(uuid, active);

        if (!expiredKeys.isEmpty()) {
            dbExecutor.execute(() -> {
                for (String key : expiredKeys) {
                    try {
                        dataStore.deleteCooldown(uuid, key);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Konnte abgelaufenen Cooldown nicht bereinigen: " + uuid + "/" + key, e);
                    }
                }
            });
        }
    }

    @Override
    public void unloadForPlayer(UUID uuid) {
        cache.remove(uuid);
    }

    @Override
    public boolean isActive(UUID uuid, String key) {
        return getRemainingMillis(uuid, key) > 0;
    }

    @Override
    public long getRemainingMillis(UUID uuid, String key) {
        Map<String, Long> playerCooldowns = cache.get(uuid);
        if (playerCooldowns == null) {
            return 0L;
        }
        Long expiresAt = playerCooldowns.get(key);
        if (expiresAt == null) {
            return 0L;
        }
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0) {
            playerCooldowns.remove(key);
            return 0L;
        }
        return remaining;
    }

    @Override
    public void set(UUID uuid, String key, long durationMillis) {
        if (durationMillis <= 0) {
            throw new IllegalArgumentException("Cooldown-Dauer muss positiv sein: " + durationMillis);
        }
        long expiresAt = System.currentTimeMillis() + durationMillis;
        cache.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>()).put(key, expiresAt);
        dbExecutor.execute(() -> {
            try {
                dataStore.saveCooldown(uuid, key, expiresAt);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Konnte Cooldown nicht speichern: " + uuid + "/" + key, e);
            }
        });
    }

    @Override
    public void remove(UUID uuid, String key) {
        Map<String, Long> playerCooldowns = cache.get(uuid);
        if (playerCooldowns != null) {
            playerCooldowns.remove(key);
        }
        dbExecutor.execute(() -> {
            try {
                dataStore.deleteCooldown(uuid, key);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Konnte Cooldown nicht entfernen: " + uuid + "/" + key, e);
            }
        });
    }
}
