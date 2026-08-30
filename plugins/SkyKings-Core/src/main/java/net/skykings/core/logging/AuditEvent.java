package net.skykings.core.logging;

import java.util.Objects;
import java.util.UUID;

/** Unveraenderlicher Audit-Log-Eintrag. */
public final class AuditEvent {

    private final AuditEventType type;
    private final UUID targetUuid;
    private final String actor;
    private final Long amount;
    private final String details;
    private final long timestamp;

    public AuditEvent(AuditEventType type, UUID targetUuid, String actor, Long amount, String details) {
        this.type = Objects.requireNonNull(type, "type");
        this.targetUuid = targetUuid;
        this.actor = actor != null ? actor : "SYSTEM";
        this.amount = amount;
        this.details = details;
        this.timestamp = System.currentTimeMillis();
    }

    public AuditEventType getType() {
        return type;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getActor() {
        return actor;
    }

    public Long getAmount() {
        return amount;
    }

    public String getDetails() {
        return details;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
