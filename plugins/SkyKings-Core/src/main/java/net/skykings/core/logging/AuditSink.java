package net.skykings.core.logging;

/**
 * Ziel fuer Audit-Events (z. B. Plugin-Log, Datenbank, spaeter Discord-Webhook).
 * Neue Ziele lassen sich hinzufuegen, ohne die aufrufende Business-Logik zu aendern.
 */
public interface AuditSink {

    void handle(AuditEvent event);
}
