package net.skykings.core.logging;

import net.skykings.core.storage.DataStore;

import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Persistiert jedes Audit-Event asynchron in die Datenbank (siehe SkyKings-Core-DB-Executor). */
public final class PersistentAuditSink implements AuditSink {

    private final DataStore dataStore;
    private final ExecutorService dbExecutor;
    private final Logger logger;

    public PersistentAuditSink(DataStore dataStore, ExecutorService dbExecutor, Logger logger) {
        this.dataStore = dataStore;
        this.dbExecutor = dbExecutor;
        this.logger = logger;
    }

    @Override
    public void handle(AuditEvent event) {
        dbExecutor.execute(() -> {
            try {
                dataStore.appendAuditEvent(event);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Konnte Audit-Event nicht persistieren", e);
            }
        });
    }
}
