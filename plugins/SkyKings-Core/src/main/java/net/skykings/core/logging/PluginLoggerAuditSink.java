package net.skykings.core.logging;

import java.util.logging.Logger;

/** Schreibt jedes Audit-Event strukturiert in das Plugin-Log (Phase 1A: lokales Logging). */
public final class PluginLoggerAuditSink implements AuditSink {

    private final Logger logger;

    public PluginLoggerAuditSink(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void handle(AuditEvent event) {
        logger.info(String.format(
                "[AUDIT] type=%s target=%s actor=%s amount=%s details=%s",
                event.getType(),
                event.getTargetUuid(),
                event.getActor(),
                event.getAmount() != null ? event.getAmount() : "-",
                event.getDetails() != null ? event.getDetails() : "-"));
    }
}
