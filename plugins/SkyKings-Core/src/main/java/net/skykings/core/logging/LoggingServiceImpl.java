package net.skykings.core.logging;

import net.skykings.core.model.Rank;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LoggingServiceImpl implements LoggingService {

    private final List<AuditSink> sinks;
    private final Logger fallbackLogger;

    public LoggingServiceImpl(List<AuditSink> sinks, Logger fallbackLogger) {
        this.sinks = new ArrayList<>(sinks);
        this.fallbackLogger = fallbackLogger;
    }

    @Override
    public void log(AuditEvent event) {
        for (AuditSink sink : sinks) {
            try {
                sink.handle(event);
            } catch (Exception e) {
                fallbackLogger.log(Level.SEVERE, "Audit-Sink " + sink.getClass().getSimpleName() + " ist fehlgeschlagen", e);
            }
        }
    }

    @Override
    public void logEconomyDeposit(UUID target, long amount, long newBalance, String actor, String reason) {
        log(new AuditEvent(AuditEventType.ECONOMY_DEPOSIT, target, actor, amount, describe(newBalance, reason)));
    }

    @Override
    public void logEconomyWithdraw(UUID target, long amount, long newBalance, String actor, String reason) {
        log(new AuditEvent(AuditEventType.ECONOMY_WITHDRAW, target, actor, amount, describe(newBalance, reason)));
    }

    @Override
    public void logEconomySet(UUID target, long oldBalance, long newBalance, String actor, String reason) {
        String details = "alterKontostand=" + oldBalance + ", neuerKontostand=" + newBalance
                + (reason != null ? ", grund=" + reason : "");
        log(new AuditEvent(AuditEventType.ECONOMY_SET, target, actor, newBalance - oldBalance, details));
    }

    @Override
    public void logRankChange(UUID target, Rank oldRank, Rank newRank, String actor) {
        log(new AuditEvent(AuditEventType.RANK_CHANGE, target, actor, null,
                "alterRang=" + oldRank + ", neuerRang=" + newRank));
    }

    @Override
    public void logProfileCreated(UUID target, String name) {
        log(new AuditEvent(AuditEventType.PROFILE_CREATED, target, "SYSTEM", null, "name=" + name));
    }

    private String describe(long newBalance, String reason) {
        return "neuerKontostand=" + newBalance + (reason != null ? ", grund=" + reason : "");
    }
}
