package net.skykings.crates;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persistentes Issued-Registry: Eine gueltig formatierte Lore/NBT-Struktur allein reicht nicht.
 * Crate-Batches und Gutscheine muessen vom Server selbst ausgegeben worden sein, bevor die
 * Codecs sie akzeptieren. Neue stackbare Gutscheine teilen sich pro Typ/Ziel eine stabile
 * Serial; maxClaims zaehlt deshalb die serverseitig tatsaechlich ausgegebenen Exemplare.
 */
public final class IssuedItemStore {

    private static volatile IssuedItemStore active;

    private static final class Entry {
        final char kind;
        final String type;
        final String target;
        final int maxClaims;

        Entry(char kind, String type, String target, int maxClaims) {
            this.kind = kind;
            this.type = normalize(type);
            this.target = normalize(target);
            this.maxClaims = maxClaims;
        }
    }

    private final File file;
    private final Logger logger;
    private final Map<UUID, Entry> entries = new HashMap<UUID, Entry>();

    public IssuedItemStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
        load();
        active = this;
    }

    /** Produktion: von Default-Codecs genutzt. Tests ohne Plugin erhalten weiterhin null. */
    public static IssuedItemStore active() { return active; }

    /**
     * Registriert genau ein neu ausgegebenes Exemplar. Fuer v2-Stack-Voucher ist die Serial
     * absichtlich stabil, daher wird bei identischem Typ/Ziel die Claim-Grenze atomar erhoeht.
     * Legacy-Serials bleiben mit ihrem bisherigen Einzel-Claim voll kompatibel.
     */
    public synchronized boolean issueVoucher(UUID serial, VoucherItemCodec.VoucherType type, String target) {
        if (serial == null || type == null) return false;
        Entry existing = entries.get(serial);
        if (existing != null) {
            if (existing.kind != 'V' || !existing.type.equals(normalize(type.name()))
                    || !existing.target.equals(normalize(target))) return false;
            if (existing.maxClaims == Integer.MAX_VALUE) return false;
            Entry incremented = new Entry('V', type.name(), target, existing.maxClaims + 1);
            if (!appendLine(serial, new Entry('V', type.name(), target, 1))) return false;
            entries.put(serial, incremented);
            return true;
        }
        Entry entry = new Entry('V', type.name(), target, 1);
        if (!appendLine(serial, entry)) return false;
        entries.put(serial, entry);
        return true;
    }

    public synchronized boolean issueCrate(UUID serial, String crateId, int maxClaims) {
        if (serial == null || crateId == null || maxClaims < 1 || maxClaims > 64) return false;
        Entry entry = new Entry('C', crateId, "", maxClaims);
        Entry existing = entries.get(serial);
        if (existing != null) return same(existing, entry);
        if (!appendLine(serial, entry)) return false;
        entries.put(serial, entry);
        return true;
    }

    public synchronized boolean isIssuedVoucher(UUID serial, VoucherItemCodec.VoucherType type, String target) {
        Entry entry = entries.get(serial);
        return entry != null && entry.kind == 'V' && type != null
                && entry.type.equals(normalize(type.name())) && entry.target.equals(normalize(target));
    }

    public synchronized int getVoucherMaxClaims(UUID serial, VoucherItemCodec.VoucherType type, String target) {
        Entry entry = entries.get(serial);
        if (entry == null || entry.kind != 'V' || type == null
                || !entry.type.equals(normalize(type.name())) || !entry.target.equals(normalize(target))) return 0;
        return Math.max(0, entry.maxClaims);
    }

    public synchronized boolean isIssuedCrate(UUID serial, String crateId, int maxClaims) {
        Entry entry = entries.get(serial);
        return entry != null && entry.kind == 'C' && entry.type.equals(normalize(crateId))
                && entry.maxClaims == maxClaims;
    }

    public synchronized int size() { return entries.size(); }

    private boolean appendLine(UUID serial, Entry entry) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!file.exists()) file.createNewFile();
            try (FileWriter writer = new FileWriter(file, true)) {
                writer.write(entry.kind);
                writer.write('|');
                writer.write(serial.toString());
                writer.write('|');
                writer.write(escape(entry.type));
                writer.write('|');
                writer.write(escape(entry.target));
                writer.write('|');
                writer.write(Integer.toString(entry.maxClaims));
                writer.write(System.lineSeparator());
                writer.flush();
            }
            return true;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Issued-Serial konnte nicht gespeichert werden: " + serial, ex);
            return false;
        }
    }

    private void load() {
        entries.clear();
        if (!file.exists()) return;
        try {
            for (String raw : java.nio.file.Files.readAllLines(file.toPath())) {
                if (raw == null || raw.trim().isEmpty()) continue;
                String[] parts = raw.split("\\|", -1);
                if (parts.length != 5 || parts[0].length() != 1) continue;
                try {
                    char kind = parts[0].charAt(0);
                    UUID serial = UUID.fromString(parts[1]);
                    String type = unescape(parts[2]);
                    String target = unescape(parts[3]);
                    int maxClaims = Integer.parseInt(parts[4]);
                    if (kind != 'V' && kind != 'C' || maxClaims < 1) continue;
                    Entry incoming = new Entry(kind, type, target, maxClaims);
                    Entry existing = entries.get(serial);
                    if (existing == null) {
                        entries.put(serial, incoming);
                    } else if (kind == 'V' && existing.kind == 'V'
                            && existing.type.equals(incoming.type) && existing.target.equals(incoming.target)) {
                        long combined = (long) existing.maxClaims + incoming.maxClaims;
                        entries.put(serial, new Entry('V', type, target,
                                combined > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) combined));
                    } else if (same(existing, incoming)) {
                        // Legacy-Dateien koennen durch Wiederholungen identische Crate-Zeilen enthalten.
                    }
                } catch (RuntimeException ignored) { }
            }
            logger.info("Issued Crate/Voucher Serials geladen: " + entries.size());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Issued-Serial-Registry konnte nicht geladen werden", ex);
        }
    }

    private static boolean same(Entry a, Entry b) {
        return a.kind == b.kind && a.type.equals(b.type) && a.target.equals(b.target) && a.maxClaims == b.maxClaims;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("%", "%25").replace("|", "%7C").replace("\r", "").replace("\n", "");
    }

    private static String unescape(String value) {
        return value == null ? "" : value.replace("%7C", "|").replace("%25", "%");
    }
}
