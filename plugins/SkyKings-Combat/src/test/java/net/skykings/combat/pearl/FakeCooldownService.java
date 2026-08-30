package net.skykings.combat.pearl;

import net.skykings.core.cooldown.CooldownService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Minimales In-Memory-Test-Double fuer {@link CooldownService} (kein DataStore noetig). */
final class FakeCooldownService implements CooldownService {

    private final Map<UUID, Map<String, Long>> expiries = new HashMap<>();

    @Override
    public void loadForPlayer(UUID uuid) {
        expiries.computeIfAbsent(uuid, u -> new HashMap<>());
    }

    @Override
    public void unloadForPlayer(UUID uuid) {
        expiries.remove(uuid);
    }

    @Override
    public boolean isActive(UUID uuid, String key) {
        return getRemainingMillis(uuid, key) > 0;
    }

    @Override
    public long getRemainingMillis(UUID uuid, String key) {
        Map<String, Long> perPlayer = expiries.get(uuid);
        if (perPlayer == null) {
            return 0L;
        }
        Long expiresAt = perPlayer.get(key);
        if (expiresAt == null) {
            return 0L;
        }
        return Math.max(0L, expiresAt - System.currentTimeMillis());
    }

    @Override
    public void set(UUID uuid, String key, long durationMillis) {
        expiries.computeIfAbsent(uuid, u -> new HashMap<>()).put(key, System.currentTimeMillis() + durationMillis);
    }

    @Override
    public void remove(UUID uuid, String key) {
        Map<String, Long> perPlayer = expiries.get(uuid);
        if (perPlayer != null) {
            perPlayer.remove(key);
        }
    }
}
