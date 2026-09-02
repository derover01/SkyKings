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

/** Persistente Anti-Dupe-Sperre fuer Gutscheine mit Claim-Limits pro serverseitig ausgegebenem Stack-Typ. */
public final class VoucherRedemptionStore {
    private final File file;
    private final Logger logger;
    private final ExecutorService executor;
    private final Map<UUID, Integer> redeemedClaims = new HashMap<UUID, Integer>();
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
                synchronized (redeemedClaims) {
                    redeemedClaims.clear();
                    for (String line : java.nio.file.Files.readAllLines(file.toPath())) {
                        try {
                            UUID serial = UUID.fromString(line.trim());
                            int current = redeemedClaims.containsKey(serial) ? redeemedClaims.get(serial) : 0;
                            if (current < Integer.MAX_VALUE) redeemedClaims.put(serial, current + 1);
                        } catch (IllegalArgumentException ignored) { }
                    }
                }
                ready = true;
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Voucher-Anti-Dupe-Store konnte nicht geladen werden", ex);
                throw new IllegalStateException(ex);
            }
        }, executor);
    }

    public boolean isReady() { return ready; }

    /** Legacy-Einzelvoucher bleiben exakt einmal einloesbar. */
    public CompletableFuture<Boolean> redeem(UUID serial) { return redeem(serial, 1); }

    /**
     * Asynchroner Kompatibilitaetspfad fuer Tests/Background-Aufrufer. Die eigentliche
     * Reservierung + Datei-Persistenz geschieht gemeinsam in {@link #redeemSync(UUID, int)}.
     */
    public CompletableFuture<Boolean> redeem(final UUID serial, final int maxClaims) {
        if (!ready || serial == null || maxClaims < 1) return CompletableFuture.completedFuture(false);
        return CompletableFuture.supplyAsync(() -> redeemSync(serial, maxClaims), executor);
    }

    /**
     * Reserviert und persistiert genau einen Claim als eine synchrone Operation.
     *
     * Der Live-Spielerpfad verwendet diese Methode auf dem Bukkit-Hauptthread, damit zwischen
     * "Claim sicher gespeichert" und anschliessender Reward-Vergabe kein separater Async-Callback-
     * Tick liegt, in dem der Spieler disconnecten kann. Die Dateioperation ist nur ein append einer
     * UUID-Zeile; Rapid-Clicks bleiben durch dieselbe synchronisierte Claim-Grenze fail-closed.
     */
    public boolean redeemSync(UUID serial, int maxClaims) {
        if (!ready || serial == null || maxClaims < 1) return false;
        synchronized (redeemedClaims) {
            int current = redeemedClaims.containsKey(serial) ? redeemedClaims.get(serial) : 0;
            if (current >= maxClaims) return false;
            redeemedClaims.put(serial, current + 1);
            try (FileWriter writer = new FileWriter(file, true)) {
                writer.write(serial.toString());
                writer.write(System.lineSeparator());
                writer.flush();
                return true;
            } catch (IOException ex) {
                int reserved = redeemedClaims.containsKey(serial) ? redeemedClaims.get(serial) : 0;
                if (reserved <= 1) redeemedClaims.remove(serial);
                else redeemedClaims.put(serial, reserved - 1);
                logger.log(Level.SEVERE, "Voucher-Claim konnte nicht gespeichert werden: " + serial, ex);
                return false;
            }
        }
    }

    public int getRedeemedClaims(UUID serial) {
        if (serial == null) return 0;
        synchronized (redeemedClaims) { return redeemedClaims.containsKey(serial) ? redeemedClaims.get(serial) : 0; }
    }

    /**
     * Neue Redemptions werden zuerst gesperrt. Bereits eingereihte Background-Aufrufe bekommen
     * bis zu fünf Sekunden Zeit, sauber zu Ende zu laufen.
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
