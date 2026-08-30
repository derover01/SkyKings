package net.skykings.combat.newbie;

import net.skykings.core.model.PlayerProfile;
import net.skykings.core.model.Rank;
import net.skykings.core.profile.PlayerProfileService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Minimales In-Memory-Test-Double, analog zu SkyKings-Core's eigenem FakePlayerProfileService
 * (nicht direkt wiederverwendbar, da Core keinen Test-JAR-Artefakt exportiert).
 */
final class FakePlayerProfileService implements PlayerProfileService {

    private final Map<UUID, PlayerProfile> cache = new HashMap<>();
    private int saveCallCount;

    void put(PlayerProfile profile) {
        cache.put(profile.getUuid(), profile);
    }

    int getSaveCallCount() {
        return saveCallCount;
    }

    @Override
    public PlayerProfile loadOrCreate(UUID uuid, String currentName) {
        return cache.computeIfAbsent(uuid, u -> new PlayerProfile(u, currentName, Rank.SPIELER, 0L, 0L, 0L, 0L));
    }

    @Override
    public PlayerProfile getCached(UUID uuid) {
        return cache.get(uuid);
    }

    @Override
    public void updatePresence(UUID uuid, String currentName) {
        PlayerProfile profile = cache.get(uuid);
        if (profile != null) {
            profile.setLastKnownName(currentName);
            profile.setLastSeen(System.currentTimeMillis());
        }
    }

    @Override
    public void save(UUID uuid) {
        saveCallCount++;
    }

    @Override
    public void saveAndUnload(UUID uuid) {
        cache.remove(uuid);
    }

    @Override
    public void saveAll() {
        // no-op fuer Tests
    }
}
