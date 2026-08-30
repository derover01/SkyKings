package net.skykings.combat.tag;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LastAttackerServiceImpl implements LastAttackerService {

    private static final class Entry {
        private final UUID attacker;
        private final long expiresAt;

        Entry(UUID attacker, long expiresAt) {
            this.attacker = attacker;
            this.expiresAt = expiresAt;
        }
    }

    private final long validityMillis;
    private final Map<UUID, Entry> lastAttackers = new ConcurrentHashMap<>();

    public LastAttackerServiceImpl(long validityMillis) {
        if (validityMillis <= 0) {
            throw new IllegalArgumentException("validityMillis muss positiv sein: " + validityMillis);
        }
        this.validityMillis = validityMillis;
    }

    @Override
    public void recordAttack(UUID victim, UUID attacker) {
        lastAttackers.put(victim, new Entry(attacker, System.currentTimeMillis() + validityMillis));
    }

    @Override
    public UUID getLastAttacker(UUID victim) {
        Entry entry = lastAttackers.get(victim);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() >= entry.expiresAt) {
            lastAttackers.remove(victim, entry);
            return null;
        }
        return entry.attacker;
    }

    @Override
    public void clear(UUID victim) {
        lastAttackers.remove(victim);
    }
}
