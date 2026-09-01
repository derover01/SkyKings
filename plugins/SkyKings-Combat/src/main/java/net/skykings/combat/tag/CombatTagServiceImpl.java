package net.skykings.combat.tag;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Haelt pro Spieler nur einen Ablaufzeitstempel - kein Bukkit-Task, keine Zustandsmaschine. */
public final class CombatTagServiceImpl implements CombatTagService {

    private static volatile CombatTagService liveInstance;

    private final long durationMillis;
    private final Map<UUID, Long> expiryTimestamps = new ConcurrentHashMap<UUID, Long>();

    public CombatTagServiceImpl(long durationMillis) {
        if (durationMillis <= 0) {
            throw new IllegalArgumentException("durationMillis muss positiv sein: " + durationMillis);
        }
        this.durationMillis = durationMillis;
        liveInstance = this;
    }

    /** Liefert die aktuell vom Combat-Modul erzeugte Instanz, ohne Bukkit im Service selbst zu benoetigen. */
    public static CombatTagService liveInstance() {
        return liveInstance;
    }

    @Override
    public void tag(UUID uuid) {
        expiryTimestamps.put(uuid, System.currentTimeMillis() + durationMillis);
    }

    @Override
    public void tagBoth(UUID a, UUID b) {
        tag(a);
        tag(b);
    }

    @Override
    public boolean isTagged(UUID uuid) {
        return getRemainingMillis(uuid) > 0;
    }

    @Override
    public long getRemainingMillis(UUID uuid) {
        Long expiry = expiryTimestamps.get(uuid);
        if (expiry == null) {
            return 0L;
        }
        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0) {
            expiryTimestamps.remove(uuid, expiry);
            return 0L;
        }
        return remaining;
    }

    @Override
    public void clear(UUID uuid) {
        expiryTimestamps.remove(uuid);
    }
}
