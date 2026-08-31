package net.skykings.crates;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Persistente Anti-Dupe-Sperre fuer Gutscheine. */
public final class VoucherRedemptionStore {
    private final File file;
    private final Logger logger;
    private final ExecutorService executor;
    private final Set<UUID> redeemed = Collections.synchronizedSet(new HashSet<UUID>());
    private volatile boolean ready;

    public VoucherRedemptionStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SkyKings-Voucher-Store");
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
                for (String line : java.nio.file.Files.readAllLines(file.toPath())) {
                    try { redeemed.add(UUID.fromString(line.trim())); } catch (IllegalArgumentException ignored) { }
                }
                ready = true;
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Voucher-Anti-Dupe-Store konnte nicht geladen werden", ex);
                throw new IllegalStateException(ex);
            }
        }, executor);
    }

    public boolean isReady() { return ready; }

    public CompletableFuture<Boolean> redeem(UUID serial) {
        if (!ready || serial == null) return CompletableFuture.completedFuture(false);
        synchronized (redeemed) {
            if (redeemed.contains(serial)) return CompletableFuture.completedFuture(false);
            redeemed.add(serial);
        }
        return CompletableFuture.supplyAsync(() -> {
            try (FileWriter writer = new FileWriter(file, true)) {
                writer.write(serial.toString());
                writer.write(System.lineSeparator());
                writer.flush();
                return true;
            } catch (IOException ex) {
                redeemed.remove(serial);
                logger.log(Level.SEVERE, "Voucher-Seriennummer konnte nicht gespeichert werden: " + serial, ex);
                return false;
            }
        }, executor);
    }

    /**
     * Neue Redemptions werden zuerst gesperrt. Bereits eingereihte Datei-Writes bekommen
     * bis zu fünf Sekunden Zeit, sauber zu Ende zu laufen, damit ein Restart keinen
     * bereits vergebenen Gutschein wieder einloesbar macht.
     */
    public void shutdown() {
        ready = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warning("Voucher-Anti-Dupe-Store hatte beim Shutdown noch ausstehende Writes.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.warning("Warten auf Voucher-Anti-Dupe-Store wurde unterbrochen.");
        }
    }
}
