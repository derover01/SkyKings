package net.skykings.admin.casino;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Write-ahead journal fuer Casino-Runden ueber PlayerProfile- und GUI-Grenzen hinweg. */
public final class CasinoSettlementJournal {
    public static final String FILE_NAME = "casino-settlement-journal.yml";
    private static volatile CasinoSettlementJournal active;

    private final Logger logger;
    private final File file;
    private final YamlConfiguration data;

    public CasinoSettlementJournal(JavaPlugin plugin) {
        this(plugin.getDataFolder(), plugin.getLogger());
    }

    CasinoSettlementJournal(File dataFolder, Logger logger) {
        this.logger = logger;
        this.file = new File(dataFolder, FILE_NAME);
        this.data = YamlConfiguration.loadConfiguration(file);
        cleanupCompleted();
        active = this;
    }

    public static CasinoSettlementJournal active() { return active; }

    public synchronized UUID begin(UUID player, String currency, String game,
                                   long balanceBefore, long bet, long payout, long expectedBalance) {
        if (player == null || currency == null || game == null || bet <= 0L || balanceBefore < 0L || payout < 0L || expectedBalance < 0L) {
            return null;
        }
        UUID transaction = UUID.randomUUID();
        String base = "settlements." + transaction + ".";
        data.set(base + "status", "IN_PROGRESS");
        data.set(base + "player", player.toString());
        data.set(base + "currency", currency);
        data.set(base + "game", game);
        data.set(base + "balance-before", balanceBefore);
        data.set(base + "bet", bet);
        data.set(base + "payout", payout);
        data.set(base + "expected-balance", expectedBalance);
        data.set(base + "created-at", System.currentTimeMillis());
        if (commit()) return transaction;
        data.set("settlements." + transaction, null);
        return null;
    }

    public synchronized void noteFailure(UUID transaction, String reason) {
        if (transaction == null) return;
        String base = "settlements." + transaction + ".";
        if (!data.contains(base + "status")) return;
        data.set(base + "reason", reason == null ? "UNKNOWN" : reason);
        data.set(base + "failed-at", System.currentTimeMillis());
        commit();
    }

    public synchronized boolean complete(UUID transaction) {
        if (transaction == null) return false;
        String base = "settlements." + transaction + ".";
        if (!data.contains(base + "status")) return false;
        data.set(base + "status", "COMPLETED");
        data.set(base + "completed-at", System.currentTimeMillis());
        if (!commit()) {
            data.set(base + "status", "IN_PROGRESS");
            data.set(base + "completed-at", null);
            return false;
        }
        data.set("settlements." + transaction, null);
        commit();
        return true;
    }

    public synchronized boolean hasPendingFor(UUID player) {
        if (player == null) return false;
        ConfigurationSection root = data.getConfigurationSection("settlements");
        if (root == null) return false;
        for (String key : root.getKeys(false)) {
            String base = "settlements." + key + ".";
            if ("COMPLETED".equalsIgnoreCase(data.getString(base + "status", "IN_PROGRESS"))) continue;
            if (player.toString().equals(data.getString(base + "player", ""))) return true;
        }
        return false;
    }

    public synchronized int reviewRequiredCount() {
        ConfigurationSection root = data.getConfigurationSection("settlements");
        if (root == null) return 0;
        int count = 0;
        for (String key : root.getKeys(false)) {
            if (!"COMPLETED".equalsIgnoreCase(data.getString("settlements." + key + ".status", "IN_PROGRESS"))) count++;
        }
        return count;
    }

    private void cleanupCompleted() {
        ConfigurationSection root = data.getConfigurationSection("settlements");
        if (root == null) return;
        List<String> completed = new ArrayList<String>();
        for (String key : root.getKeys(false)) {
            if ("COMPLETED".equalsIgnoreCase(data.getString("settlements." + key + ".status", ""))) completed.add(key);
        }
        if (completed.isEmpty()) return;
        for (String key : completed) data.set("settlements." + key, null);
        if (!commit()) logger.warning("Casino-Settlement-Journal konnte COMPLETED-Tombstones nicht aufraeumen.");
    }

    private boolean commit() {
        File parent = file.getParentFile();
        File temp = parent == null ? new File(file.getPath() + ".tmp") : new File(parent, file.getName() + ".tmp");
        try {
            if (parent != null && !parent.exists() && !parent.mkdirs()) return false;
            data.save(temp);
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException ex) {
            logger.log(Level.SEVERE, "Casino-Settlement-Journal konnte nicht gespeichert werden.", ex);
            if (temp.exists() && !temp.delete()) temp.deleteOnExit();
            return false;
        }
    }
}
