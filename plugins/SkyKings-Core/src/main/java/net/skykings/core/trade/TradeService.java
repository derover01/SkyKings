package net.skykings.core.trade;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Verwaltet Trade-Anfragen und aktive Sitzungen; GUI/Inventartransfer sitzt bewusst darueber. */
public final class TradeService {
    private final Map<UUID, UUID> pendingRequests = new ConcurrentHashMap<UUID, UUID>();
    private final Map<UUID, TradeSession> activeByPlayer = new ConcurrentHashMap<UUID, TradeSession>();

    public TradeService() {
        // In Unit-Tests existiert keine Bukkit-Runtime. Auf dem echten Server wird das Journal
        // direkt mit dem TradeService gebootstrapped, damit ACTIVE Escrow-Snapshots bereits vor
        // dem ersten Spieler-Join fuer Hard-Crash-Recovery bereitstehen.
        try {
            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(TradeService.class);
            if (plugin != null && Bukkit.getPluginManager() != null) {
                TradeEscrowJournal journal = new TradeEscrowJournal(plugin);
                Bukkit.getPluginManager().registerEvents(new TradeEscrowJournalListener(plugin, this, journal), plugin);
            }
        } catch (Throwable ignored) {
            // Erwartet fuer isolierte Unit-Tests ohne CraftBukkit/Server-Kontext.
        }
    }

    /** Check + Request-Eintrag sind eine atomare Zustandsaenderung. */
    public synchronized boolean request(UUID sender, UUID target) {
        if (sender == null || target == null || sender.equals(target)) return false;
        if (activeByPlayer.containsKey(sender) || activeByPlayer.containsKey(target)) return false;
        pendingRequests.put(target, sender);
        return true;
    }

    /** Verhindert zwei parallele Accepts fuer denselben Spieler. */
    public synchronized TradeSession accept(UUID target, UUID sender) {
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

    public synchronized void deny(UUID target) { if (target != null) pendingRequests.remove(target); }

    public TradeSession get(UUID player) { return activeByPlayer.get(player); }

    /** Eindeutiger Snapshot aller laufenden Sessions; jede Session steht intern fuer zwei Spieler. */
    public synchronized Collection<TradeSession> activeSessionsSnapshot() {
        return new ArrayList<TradeSession>(new LinkedHashSet<TradeSession>(activeByPlayer.values()));
    }

    public synchronized void finish(TradeSession session) {
        if (session == null) return;
        session.setFinished(true);
        activeByPlayer.remove(session.getLeft().getPlayer(), session);
        activeByPlayer.remove(session.getRight().getPlayer(), session);
    }

    public synchronized void cancel(UUID player) {
        TradeSession session = activeByPlayer.get(player);
        if (session != null) finish(session);
        pendingRequests.remove(player);
    }
}
