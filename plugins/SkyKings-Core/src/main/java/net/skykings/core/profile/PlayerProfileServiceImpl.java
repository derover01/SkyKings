package net.skykings.core.profile;

import net.skykings.core.logging.LoggingService;
import net.skykings.core.model.PlayerProfile;
import net.skykings.core.model.Rank;
import net.skykings.core.storage.DataStore;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlayerProfileServiceImpl implements PlayerProfileService {

    private final DataStore dataStore;
    private final ExecutorService dbExecutor;
    private final LoggingService loggingService;
    private final Logger logger;
    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();

    public PlayerProfileServiceImpl(DataStore dataStore, ExecutorService dbExecutor,
                                     LoggingService loggingService, Logger logger) {
        this.dataStore = dataStore;
        this.dbExecutor = dbExecutor;
        this.loggingService = loggingService;
        this.logger = logger;
    }

    @Override
    public PlayerProfile loadOrCreate(UUID uuid, String currentName) {
        PlayerProfile existing = cache.get(uuid);
        if (existing != null) {
            return existing;
        }

        Optional<PlayerProfile> loaded = dataStore.loadProfile(uuid);
        PlayerProfile profile;
        boolean created = false;
        if (loaded.isPresent()) {
            profile = loaded.get();
        } else {
            long now = System.currentTimeMillis();
            profile = new PlayerProfile(uuid, currentName, Rank.SPIELER, 0L, 0L, now, now);
            created = true;
        }
        PlayerProfile raced = cache.putIfAbsent(uuid, profile);
        if (raced != null) profile = raced;

        if (created && raced == null) {
            dataStore.saveProfile(profile);
            loggingService.logProfileCreated(uuid, currentName);
        }
        return profile;
    }

    @Override
    public PlayerProfile getCached(UUID uuid) {
        return cache.get(uuid);
    }

    @Override
    public PlayerProfile loadExisting(UUID uuid) {
        PlayerProfile existing = cache.get(uuid);
        if (existing != null) return existing;

        Optional<PlayerProfile> loaded = dataStore.loadProfile(uuid);
        if (!loaded.isPresent()) return null;

        PlayerProfile profile = loaded.get();
        PlayerProfile raced = cache.putIfAbsent(uuid, profile);
        return raced == null ? profile : raced;
    }

    @Override
    public void updatePresence(UUID uuid, String currentName) {
        PlayerProfile profile = requireCached(uuid);
        synchronized (profile) {
            profile.setLastKnownName(currentName);
            profile.setLastSeen(System.currentTimeMillis());
        }
        save(uuid);
    }

    @Override
    public void save(UUID uuid) {
        PlayerProfile profile = cache.get(uuid);
        if (profile == null) {
            return;
        }
        dbExecutor.execute(() -> {
            try {
                dataStore.saveProfile(profile);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Konnte PlayerProfile nicht speichern: " + uuid, e);
            }
        });
    }

    @Override
    public boolean saveNow(UUID uuid) {
        PlayerProfile profile = cache.get(uuid);
        if (profile == null) return false;
        try {
            dataStore.saveProfile(profile);
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Konnte PlayerProfile nicht synchron speichern: " + uuid, e);
            return false;
        }
    }

    @Override
    public void saveAndUnload(UUID uuid) {
        PlayerProfile profile = cache.remove(uuid);
        if (profile == null) {
            return;
        }
        try {
            dataStore.saveProfile(profile);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Konnte PlayerProfile beim Quit nicht speichern: " + uuid, e);
        }
    }

    @Override
    public void saveAll() {
        for (PlayerProfile profile : cache.values()) {
            try {
                dataStore.saveProfile(profile);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Konnte PlayerProfile beim Shutdown nicht speichern: " + profile.getUuid(), e);
            }
        }
    }

    private PlayerProfile requireCached(UUID uuid) {
        PlayerProfile profile = cache.get(uuid);
        if (profile == null) {
            throw new IllegalStateException("Kein geladenes PlayerProfile fuer " + uuid
                    + " - loadOrCreate() muss zuerst aufgerufen werden.");
        }
        return profile;
    }
}
