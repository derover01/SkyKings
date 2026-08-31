package net.skykings.crates;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Persistente Anti-Dupe-Sperre fuer Batch-Seriennummern stackbarer Crates. */
public final class CrateRedemptionStore {
    private final File file;
    private final Logger logger;
    private final ExecutorService executor;
    private final Map<UUID, Integer> redeemedCounts = new HashMap<UUID, Integer>();
    private volatile boolean ready;

    public CrateRedemptionStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SkyKings-Crates-Store");
            t.setDaemon(true);
            return t;
        });
    }

    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                if (!file.exists()) file.createNewFile();
                for (String raw : java.nio.file.Files.readAllLines(file.toPath())) {
                    String line = raw == null ? "" : raw.trim();
                    if (line.isEmpty()) continue;
                    try {
                        String[] parts = line.split(",", 2);
                        UUID serial = UUID.fromString(parts[0]);
                        int count = parts.length == 2 ? Math.max(1, Integer.parseInt(parts[1])) : 1;
                        synchronized (redeemedCounts) {
                            int old = redeemedCounts.containsKey(serial) ? redeemedCounts.get(serial) : 0;
                            if (count > old) redeemedCounts.put(serial, count);
                        }
                    } catch (RuntimeException ignored) { }
                }
                ready = true;
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Crate-Anti-Dupe-Store konnte nicht geladen werden", ex);
                throw new IllegalStateException(ex);
            }
        }, executor);
    }

    public boolean isReady() { return ready; }

    /** Reserviert genau einen Claim der Batch. Insgesamt sind hoechstens maxClaims erlaubt. */
    public CompletableFuture<Boolean> redeem(UUID serial, int maxClaims) {
        if (!ready || serial == null || maxClaims < 1) return CompletableFuture.completedFuture(false);
        final int newCount;
        synchronized (redeemedCounts) {
            int current = redeemedCounts.containsKey(serial) ? redeemedCounts.get(serial) : 0;
            if (current >= maxClaims) return CompletableFuture.completedFuture(false);
            newCount = current + 1;
            redeemedCounts.put(serial, newCount);
        }
        return CompletableFuture.supplyAsync(() -> {
            try (FileWriter writer = new FileWriter(file, true)) {
                writer.write(serial.toString());
                writer.write(",");
                writer.write(Integer.toString(newCount));
                writer.write(System.lineSeparator());
                writer.flush();
                return true;
            } catch (IOException ex) {
                synchronized (redeemedCounts) {
                    int current = redeemedCounts.containsKey(serial) ? redeemedCounts.get(serial) : 0;
                    if (current == newCount) {
                        if (newCount <= 1) redeemedCounts.remove(serial);
                        else redeemedCounts.put(serial, newCount - 1);
                    }
                }
                logger.log(Level.SEVERE, "Crate-Claim konnte nicht gespeichert werden: " + serial, ex);
                return false;
            }
        }, executor);
    }

    /** Verhindert, dass ein Restart bereits reservierte Claims vor dem Datei-Flush verliert. */
    public void shutdown() {
        ready = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warning("Crate-Anti-Dupe-Store hatte beim Shutdown noch ausstehende Writes.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.warning("Warten auf Crate-Anti-Dupe-Store wurde unterbrochen.");
        }
    }
}
