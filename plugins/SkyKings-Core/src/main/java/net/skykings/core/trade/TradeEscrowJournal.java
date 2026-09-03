package net.skykings.core.trade;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persistentes Fail-Closed-Journal fuer Items, die ein Spieler bereits in einen /trade gelegt hat.
 *
 * ACTIVE bedeutet: Die Item-Entnahme wurde bereits mit Player#saveData() persistiert und der
 * aktuelle Escrow-Snapshot wurde danach atomar committed. Nur ACTIVE darf nach einem Hard-Crash
 * automatisch an den urspruenglichen Besitzer recovered werden.
 *
 * PREPARED / RETURNING / SETTLING / RECOVERING sind absichtlich transiente Zustande. Findet ein
 * Neustart einen solchen Zustand, wird er zu REVIEW_REQUIRED statt automatisch Items/Coins zu
 * bewegen. Damit kann ein Crash zwischen zwei Persistenzschritten keinen automatischen Dupe
 * erzeugen; Staff kann den gespeicherten Snapshot anschliessend gezielt pruefen.
 */
public final class TradeEscrowJournal {
    public static final String FILE_NAME = "trade-escrow.yml";

    enum State {
        ACTIVE,
        PREPARED,
        RETURNING,
        SETTLING,
        RECOVERING,
        REVIEW_REQUIRED
    }

    private static volatile TradeEscrowJournal active;

    private final File file;
    private final Logger logger;
    private YamlConfiguration data;

    public TradeEscrowJournal(JavaPlugin plugin) {
        this(new File(plugin.getDataFolder(), FILE_NAME), plugin.getLogger());
    }

    TradeEscrowJournal(File file, Logger logger) {
        this.file = file;
        this.logger = logger == null ? Logger.getLogger("SkyKings-Core") : logger;
        this.data = YamlConfiguration.loadConfiguration(file);
        active = this;
        normalizeAfterStartup();
    }

    public static TradeEscrowJournal active() {
        return active;
    }

    public synchronized boolean prepareInbound(TradeSession session, UUID player, int sourceSlot, ItemStack item) {
        if (session == null || player == null || item == null) return false;
        YamlConfiguration next = copyData();
        if (next == null) return false;
        writeSession(next, session, State.PREPARED);
        String base = sessionPath(session.getId()) + ".pending";
        next.set(base + ".player", player.toString());
        next.set(base + ".source-slot", sourceSlot);
        next.set(base + ".item", item.clone());
        next.set(sessionPath(session.getId()) + ".reason", "INBOUND_ITEM_NOT_YET_COMMITTED");
        return commit(next, "Trade-Escrow PREPARED " + session.getId());
    }

    /** Snapshot nach erfolgreichem Player#saveData(); dieser Zustand ist crash-recoverbar. */
    public synchronized boolean saveActive(TradeSession session) {
        if (session == null) return false;
        YamlConfiguration next = copyData();
        if (next == null) return false;
        writeSession(next, session, State.ACTIVE);
        String path = sessionPath(session.getId());
        next.set(path + ".pending", null);
        next.set(path + ".recovery", null);
        next.set(path + ".reason", null);
        if (session.getLeft().getItems().isEmpty() && session.getRight().getItems().isEmpty()
                && session.getLeft().getCoins() <= 0L && session.getRight().getCoins() <= 0L) {
            next.set(path, null);
        }
        return commit(next, "Trade-Escrow ACTIVE " + session.getId());
    }

    public synchronized boolean markReturning(TradeSession session, String reason) {
        return mark(session, State.RETURNING, reason == null ? "RETURNING_ITEMS" : reason);
    }

    public synchronized boolean markSettling(TradeSession session) {
        return mark(session, State.SETTLING, "TRADE_SETTLEMENT_STARTED");
    }

    public synchronized boolean markReviewRequired(UUID sessionId, String reason) {
        if (sessionId == null) return false;
        String path = sessionPath(sessionId);
        if (!data.contains(path)) return true;
        YamlConfiguration next = copyData();
        if (next == null) return false;
        next.set(path + ".state", State.REVIEW_REQUIRED.name());
        next.set(path + ".reason", reason == null ? "MANUAL_REVIEW_REQUIRED" : reason);
        next.set(path + ".recovery", null);
        return commit(next, "Trade-Escrow REVIEW_REQUIRED " + sessionId);
    }

    public synchronized boolean clear(UUID sessionId) {
        if (sessionId == null) return true;
        YamlConfiguration next = copyData();
        if (next == null) return false;
        next.set(sessionPath(sessionId), null);
        return commit(next, "Trade-Escrow clear " + sessionId);
    }

    synchronized List<RecoveryEntry> recoveriesFor(UUID player) {
        if (player == null) return Collections.emptyList();
        List<RecoveryEntry> out = new ArrayList<RecoveryEntry>();
        for (String id : sessionIds(data)) {
            UUID sessionId = parseUuid(id);
            if (sessionId == null || state(data, sessionId) != State.ACTIVE) continue;
            String path = sessionPath(sessionId);
            UUID left = parseUuid(data.getString(path + ".left.player", ""));
            UUID right = parseUuid(data.getString(path + ".right.player", ""));
            if (player.equals(left)) {
                List<ItemStack> items = itemList(data, path + ".left.items");
                if (!items.isEmpty()) out.add(new RecoveryEntry(sessionId, player, items));
            } else if (player.equals(right)) {
                List<ItemStack> items = itemList(data, path + ".right.items");
                if (!items.isEmpty()) out.add(new RecoveryEntry(sessionId, player, items));
            }
        }
        return out;
    }

    synchronized boolean beginRecovery(UUID sessionId, UUID player) {
        if (sessionId == null || player == null || state(data, sessionId) != State.ACTIVE) return false;
        String path = sessionPath(sessionId);
        UUID left = parseUuid(data.getString(path + ".left.player", ""));
        UUID right = parseUuid(data.getString(path + ".right.player", ""));
        if (!player.equals(left) && !player.equals(right)) return false;

        YamlConfiguration next = copyData();
        if (next == null) return false;
        next.set(path + ".state", State.RECOVERING.name());
        next.set(path + ".reason", "AUTO_RECOVERY_IN_PROGRESS");
        next.set(path + ".recovery.player", player.toString());
        return commit(next, "Trade-Escrow RECOVERING " + sessionId + " -> " + player);
    }

    synchronized boolean completeRecovery(UUID sessionId, UUID player) {
        if (sessionId == null || player == null || state(data, sessionId) != State.RECOVERING) return false;
        String path = sessionPath(sessionId);
        UUID recovering = parseUuid(data.getString(path + ".recovery.player", ""));
        if (!player.equals(recovering)) return false;

        YamlConfiguration next = copyData();
        if (next == null) return false;
        UUID left = parseUuid(next.getString(path + ".left.player", ""));
        UUID right = parseUuid(next.getString(path + ".right.player", ""));
        if (player.equals(left)) next.set(path + ".left.items", new ArrayList<ItemStack>());
        else if (player.equals(right)) next.set(path + ".right.items", new ArrayList<ItemStack>());
        else return false;

        next.set(path + ".left.coins", 0L);
        next.set(path + ".right.coins", 0L);
        next.set(path + ".recovery", null);
        next.set(path + ".reason", null);

        boolean empty = itemList(next, path + ".left.items").isEmpty()
                && itemList(next, path + ".right.items").isEmpty();
        if (empty) next.set(path, null);
        else next.set(path + ".state", State.ACTIVE.name());
        return commit(next, "Trade-Escrow recovery complete " + sessionId + " -> " + player);
    }

    public synchronized int reviewRequiredCount() {
        int count = 0;
        for (String id : sessionIds(data)) {
            UUID sessionId = parseUuid(id);
            if (sessionId != null && state(data, sessionId) == State.REVIEW_REQUIRED) count++;
        }
        return count;
    }

    public synchronized int recoverableSessionCount() {
        int count = 0;
        for (String id : sessionIds(data)) {
            UUID sessionId = parseUuid(id);
            if (sessionId == null || state(data, sessionId) != State.ACTIVE) continue;
            String path = sessionPath(sessionId);
            if (!itemList(data, path + ".left.items").isEmpty()
                    || !itemList(data, path + ".right.items").isEmpty()) count++;
        }
        return count;
    }

    synchronized State stateOf(UUID sessionId) {
        return state(data, sessionId);
    }

    private boolean mark(TradeSession session, State state, String reason) {
        if (session == null) return false;
        YamlConfiguration next = copyData();
        if (next == null) return false;
        writeSession(next, session, state);
        String path = sessionPath(session.getId());
        next.set(path + ".pending", null);
        next.set(path + ".recovery", null);
        next.set(path + ".reason", reason);
        return commit(next, "Trade-Escrow " + state + " " + session.getId());
    }

    private void normalizeAfterStartup() {
        YamlConfiguration next = copyData();
        if (next == null) return;
        boolean changed = false;
        int review = 0;
        int recoverable = 0;
        for (String id : sessionIds(next)) {
            UUID sessionId = parseUuid(id);
            if (sessionId == null) {
                next.set("sessions." + id, null);
                changed = true;
                continue;
            }
            String path = sessionPath(sessionId);
            State current = state(next, sessionId);
            if (current == null) {
                next.set(path + ".state", State.REVIEW_REQUIRED.name());
                next.set(path + ".reason", "UNKNOWN_OR_MISSING_STATE_AT_STARTUP");
                current = State.REVIEW_REQUIRED;
                changed = true;
            }
            if (current != State.ACTIVE && current != State.REVIEW_REQUIRED) {
                next.set(path + ".state", State.REVIEW_REQUIRED.name());
                next.set(path + ".reason", "INTERRUPTED_" + current.name());
                next.set(path + ".recovery", null);
                current = State.REVIEW_REQUIRED;
                changed = true;
            }
            if (current == State.ACTIVE) {
                // Coin-Angebote sind im ACTIVE-Zustand noch nie abgebucht. Nach Crash werden daher
                // nur echte Escrow-Items recovered; Coin-Werte sind nicht Teil der Recovery.
                next.set(path + ".left.coins", 0L);
                next.set(path + ".right.coins", 0L);
                boolean noItems = itemList(next, path + ".left.items").isEmpty()
                        && itemList(next, path + ".right.items").isEmpty();
                if (noItems) {
                    next.set(path, null);
                    changed = true;
                    continue;
                }
                recoverable++;
            } else {
                review++;
            }
        }
        if (changed && !commit(next, "Trade-Escrow startup normalization")) {
            logger.severe("Trade-Escrow konnte Startup-Recovery-Status nicht persistent normalisieren.");
        }
        if (review > 0) {
            logger.severe("Trade-Escrow: " + review + " Session(s) benoetigen REVIEW_REQUIRED. /skcheck verwenden.");
        }
        if (recoverable > 0) {
            logger.warning("Trade-Escrow: " + recoverable + " ACTIVE Session(s) warten auf automatische Item-Rueckgabe beim Spieler-Join.");
        }
    }

    private void writeSession(YamlConfiguration target, TradeSession session, State state) {
        String path = sessionPath(session.getId());
        target.set(path + ".state", state.name());
        target.set(path + ".updated-at", System.currentTimeMillis());
        writeOffer(target, path + ".left", session.getLeft());
        writeOffer(target, path + ".right", session.getRight());
    }

    private void writeOffer(YamlConfiguration target, String path, TradeOffer offer) {
        target.set(path + ".player", offer.getPlayer().toString());
        target.set(path + ".coins", offer.getCoins());
        List<ItemStack> items = new ArrayList<ItemStack>();
        for (ItemStack item : offer.getItems()) if (item != null) items.add(item.clone());
        target.set(path + ".items", items);
    }

    private YamlConfiguration copyData() {
        try {
            YamlConfiguration copy = new YamlConfiguration();
            copy.loadFromString(data.saveToString());
            return copy;
        } catch (InvalidConfigurationException ex) {
            logger.log(Level.SEVERE, "Trade-Escrow konnte In-Memory-YAML nicht kopieren.", ex);
            return null;
        }
    }

    private boolean commit(YamlConfiguration next, String operation) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
            logger.severe(operation + " fehlgeschlagen: Datenordner konnte nicht erstellt werden.");
            return false;
        }
        Path target = file.toPath();
        Path temp = new File(file.getParentFile(), file.getName() + ".tmp").toPath();
        byte[] bytes = next.saveToString().getBytes(StandardCharsets.UTF_8);
        try {
            FileOutputStream out = new FileOutputStream(temp.toFile(), false);
            try {
                out.write(bytes);
                out.flush();
                out.getFD().sync();
            } finally {
                out.close();
            }
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            data = next;
            return true;
        } catch (IOException ex) {
            try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            logger.log(Level.SEVERE, operation + " fehlgeschlagen; Trade wird fail-closed behandelt.", ex);
            return false;
        }
    }

    private State state(YamlConfiguration source, UUID sessionId) {
        if (sessionId == null) return null;
        String raw = source.getString(sessionPath(sessionId) + ".state", "");
        try { return State.valueOf(raw); }
        catch (IllegalArgumentException ex) { return null; }
    }

    private Set<String> sessionIds(YamlConfiguration source) {
        if (source.getConfigurationSection("sessions") == null) return Collections.emptySet();
        return source.getConfigurationSection("sessions").getKeys(false);
    }

    private List<ItemStack> itemList(YamlConfiguration source, String path) {
        List<ItemStack> out = new ArrayList<ItemStack>();
        List<?> raw = source.getList(path);
        if (raw == null) return out;
        for (Object value : raw) if (value instanceof ItemStack) out.add(((ItemStack) value).clone());
        return out;
    }

    private String sessionPath(UUID sessionId) {
        return "sessions." + sessionId;
    }

    private UUID parseUuid(String raw) {
        try { return UUID.fromString(raw); }
        catch (Exception ex) { return null; }
    }

    static final class RecoveryEntry {
        final UUID sessionId;
        final UUID player;
        final List<ItemStack> items;

        RecoveryEntry(UUID sessionId, UUID player, List<ItemStack> items) {
            this.sessionId = sessionId;
            this.player = player;
            List<ItemStack> copy = new ArrayList<ItemStack>();
            for (ItemStack item : items) if (item != null) copy.add(item.clone());
            this.items = Collections.unmodifiableList(copy);
        }
    }
}
