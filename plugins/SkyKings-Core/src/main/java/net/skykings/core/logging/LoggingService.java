package net.skykings.core.logging;

import net.skykings.core.model.Rank;

import java.util.UUID;

/**
 * Zentraler Audit-/Logging-Service (siehe docs/GAMEPLAY.md "Logging" und CLAUDE.md Punkt 10).
 * Phase 1A: lokales strukturiertes Logging + Persistenz. Discord-Anbindung folgt spaeter als
 * zusaetzlicher {@link AuditSink}, ohne dass Aufrufer dieser Schnittstelle sich aendern muessen.
 */
public interface LoggingService {

    void log(AuditEvent event);

    void logEconomyDeposit(UUID target, long amount, long newBalance, String actor, String reason);

    void logEconomyWithdraw(UUID target, long amount, long newBalance, String actor, String reason);

    void logEconomySet(UUID target, long oldBalance, long newBalance, String actor, String reason);

    void logRankChange(UUID target, Rank oldRank, Rank newRank, String actor);

    void logProfileCreated(UUID target, String name);
}
