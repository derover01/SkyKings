package net.skykings.core.cooldown;

import net.skykings.core.storage.DataStore;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Haelt aktive Cooldowns im Speicher (schneller Zugriff) und schreibt jede Aenderung
 * asynchron in die Datenbank, damit sie einen Serverneustart uebersteht.
 *
 * <p>Da Cooldown-Keys erst von spaeteren Modulen (Kits, Faehigkeiten) definiert werden und es
 * daher keine Moeglichkeit gibt, "alle Keys eines Spielers" vorab zu kennen, wird ein Key beim
 * ersten Zugriff pro Session einmalig synchron aus der Datenbank nachgeladen und danach aus
 * dem Cache bedient.
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
        cache.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>());
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
        Long expiresAt = resolve(uuid, key);
        if (expiresAt == null) {
            return 0L;
        }
        return Math.max(0L, expiresAt - System.currentTimeMillis());
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

    private Long resolve(UUID uuid, String key) {
        Map<String, Long> playerCooldowns = cache.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>());
        Long cached = playerCooldowns.get(key);
        if (cached != null) {
            return cached;
        }
        Optional<Long> fromDb = dataStore.loadCooldown(uuid, key);
        if (fromDb.isPresent()) {
            playerCooldowns.put(key, fromDb.get());
            return fromDb.get();
        }
        return null;
    }
}
