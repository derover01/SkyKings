package net.skykings.core.logging;

import java.util.ArrayList;
import java.util.List;

/** Test-Double: sammelt Audit-Events statt sie irgendwohin zu schreiben. */
public final class RecordingAuditSink implements AuditSink {

    private final List<AuditEvent> events = new ArrayList<>();

    @Override
    public void handle(AuditEvent event) {
        events.add(event);
    }

    public List<AuditEvent> getEvents() {
        return events;
    }
}
