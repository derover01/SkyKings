package net.skykings.combat.event;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zentrale Runtime-Zuordnung fuer Spieler, die sich gerade in einem isolierten Serverevent
 * befinden. Normale SkyPvP-Listener koennen dadurch Event-Kaempfe von Stats, Bounties,
 * Streaks und anderen Open-World-Systemen trennen.
 */
public final class EventParticipationService {

    private static final EventParticipationService GLOBAL = new EventParticipationService();

    public enum Type {
        DUEL,
        LMS,
        CLAN_WAR
    }

    public static final class Participation {
        private final Type type;
        private final String sessionId;

        Participation(Type type, String sessionId) {
            this.type = type;
            this.sessionId = sessionId;
        }

        public Type getType() { return type; }
        public String getSessionId() { return sessionId; }
    }

    private final Map<UUID, Participation> active = new ConcurrentHashMap<UUID, Participation>();

    public static EventParticipationService global() { return GLOBAL; }

    public boolean join(UUID player, Type type, String sessionId) {
        if (player == null || type == null || sessionId == null || sessionId.trim().isEmpty()) return false;
        return active.putIfAbsent(player, new Participation(type, sessionId)) == null;
    }

    public void leave(UUID player) {
        if (player != null) active.remove(player);
    }

    public Participation get(UUID player) {
        return player == null ? null : active.get(player);
    }

    public boolean isInEvent(UUID player) {
        return player != null && active.containsKey(player);
    }

    public boolean isSameSession(UUID first, UUID second) {
        Participation a = get(first), b = get(second);
        return a != null && b != null && a.type == b.type && a.sessionId.equals(b.sessionId);
    }

    public void clear() { active.clear(); }

    public Map<UUID, Participation> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<UUID, Participation>(active));
    }
}
