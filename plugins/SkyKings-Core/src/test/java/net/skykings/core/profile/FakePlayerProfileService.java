package net.skykings.core.profile;

import net.skykings.core.model.PlayerProfile;
import net.skykings.core.model.Rank;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-Memory-Test-Double ohne Persistenz - fuer isolierte Unit-Tests von RankService/EconomyService.
 * Bewusst nicht {@code final}, damit einzelne Tests (z. B. PlayerLifecycleListenerTest) gezielt
 * einen Fehlerfall ueberschreiben koennen, ohne ein komplett neues Test-Double zu schreiben.
 */
public class FakePlayerProfileService implements PlayerProfileService {

    private final Map<UUID, PlayerProfile> cache = new HashMap<>();

    public void put(PlayerProfile profile) {
        cache.put(profile.getUuid(), profile);
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
        // no-op: Tests pruefen den In-Memory-Zustand direkt.
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
