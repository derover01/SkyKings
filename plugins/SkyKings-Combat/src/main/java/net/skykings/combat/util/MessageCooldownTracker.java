package net.skykings.combat.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Verhindert Nachrichten-Spam (siehe Auftrag Phase 2, Abschnitt 15): eine Nachricht pro Spieler hoechstens alle X ms. */
public final class MessageCooldownTracker {

    private final long intervalMillis;
    private final Map<UUID, Long> lastSentAt = new ConcurrentHashMap<>();

    public MessageCooldownTracker(long intervalMillis) {
        if (intervalMillis < 0) {
            throw new IllegalArgumentException("intervalMillis darf nicht negativ sein: " + intervalMillis);
        }
        this.intervalMillis = intervalMillis;
    }

    /** Liefert {@code true} genau dann, wenn jetzt gesendet werden darf (und merkt sich das). */
    public boolean shouldSend(UUID uuid) {
        long now = System.currentTimeMillis();
        Long last = lastSentAt.get(uuid);
        if (last != null && now - last < intervalMillis) {
            return false;
        }
        lastSentAt.put(uuid, now);
        return true;
    }
}
