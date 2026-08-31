package net.skykings.core.trade;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Verwaltet Trade-Anfragen und aktive Sitzungen; GUI/Inventartransfer sitzt bewusst darueber. */
public final class TradeService {
    private final Map<UUID, UUID> pendingRequests = new ConcurrentHashMap<UUID, UUID>();
    private final Map<UUID, TradeSession> activeByPlayer = new ConcurrentHashMap<UUID, TradeSession>();

    public boolean request(UUID sender, UUID target) {
        if (sender == null || target == null || sender.equals(target)) return false;
        if (activeByPlayer.containsKey(sender) || activeByPlayer.containsKey(target)) return false;
        pendingRequests.put(target, sender);
        return true;
    }

    public TradeSession accept(UUID target, UUID sender) {
        if (target == null || sender == null) return null;
        UUID requestedBy = pendingRequests.get(target);
        if (!sender.equals(requestedBy)) return null;
        if (activeByPlayer.containsKey(sender) || activeByPlayer.containsKey(target)) return null;
        pendingRequests.remove(target);
        TradeSession session = new TradeSession(sender, target);
        activeByPlayer.put(sender, session);
        activeByPlayer.put(target, session);
        return session;
    }

    public void deny(UUID target) { if (target != null) pendingRequests.remove(target); }

    public TradeSession get(UUID player) { return activeByPlayer.get(player); }

    public void finish(TradeSession session) {
        if (session == null) return;
        session.setFinished(true);
        activeByPlayer.remove(session.getLeft().getPlayer());
        activeByPlayer.remove(session.getRight().getPlayer());
    }

    public void cancel(UUID player) {
        TradeSession session = activeByPlayer.get(player);
        if (session != null) finish(session);
        pendingRequests.remove(player);
    }
}
