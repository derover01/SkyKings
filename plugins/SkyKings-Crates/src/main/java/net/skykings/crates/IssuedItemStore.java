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
 * Persistentes Issued-Registry: Eine gueltig formatierte Lore allein reicht nicht.
 * Crate-Batches und Gutscheine muessen mit ihrer Serial vom Server selbst ausgegeben
 * worden sein, bevor die Codecs sie akzeptieren.
 */
public final class IssuedItemStore {

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
    }

    public synchronized boolean issueVoucher(UUID serial, VoucherItemCodec.VoucherType type, String target) {
        if (serial == null || type == null) return false;
        Entry entry = new Entry('V', type.name(), target, 1);
        return append(serial, entry);
    }

    public synchronized boolean issueCrate(UUID serial, String crateId, int maxClaims) {
        if (serial == null || crateId == null || maxClaims < 1 || maxClaims > 64) return false;
        Entry entry = new Entry('C', crateId, "", maxClaims);
        return append(serial, entry);
    }

    public synchronized boolean isIssuedVoucher(UUID serial, VoucherItemCodec.VoucherType type, String target) {
        Entry entry = entries.get(serial);
        return entry != null && entry.kind == 'V' && type != null
                && entry.type.equals(normalize(type.name())) && entry.target.equals(normalize(target));
    }

    public synchronized boolean isIssuedCrate(UUID serial, String crateId, int maxClaims) {
        Entry entry = entries.get(serial);
        return entry != null && entry.kind == 'C' && entry.type.equals(normalize(crateId))
                && entry.maxClaims == maxClaims;
    }

    public synchronized int size() { return entries.size(); }

    private boolean append(UUID serial, Entry entry) {
        Entry existing = entries.get(serial);
        if (existing != null) return same(existing, entry);
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
            entries.put(serial, entry);
            return true;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Issued-Serial konnte nicht gespeichert werden: " + serial, ex);
            return false;
        }
    }

    private void load() {
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
                    if (kind != 'V' && kind != 'C') continue;
                    entries.put(serial, new Entry(kind, type, target, maxClaims));
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
