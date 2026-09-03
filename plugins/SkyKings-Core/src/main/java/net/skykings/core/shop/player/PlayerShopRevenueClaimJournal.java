package net.skykings.core.shop.player;

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

/** Write-ahead journal fuer PlayerShop-Erlös-Claims ueber YAML und Economy-SQLite. */
public final class PlayerShopRevenueClaimJournal {
    public static final String FILE_NAME = "player-shop-revenue-claim-journal.yml";
    private static volatile PlayerShopRevenueClaimJournal active;

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public PlayerShopRevenueClaimJournal(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        this.data = YamlConfiguration.loadConfiguration(file);
        cleanupCompleted();
        active = this;
    }

    public static PlayerShopRevenueClaimJournal active() { return active; }

    public synchronized UUID begin(UUID shopId, UUID owner, long amount) {
        if (shopId == null || owner == null || amount <= 0L) return null;
        UUID transaction = UUID.randomUUID();
        String base = "claims." + transaction + ".";
        data.set(base + "status", "IN_PROGRESS");
        data.set(base + "shop", shopId.toString());
        data.set(base + "owner", owner.toString());
        data.set(base + "amount", amount);
        data.set(base + "created-at", System.currentTimeMillis());
        if (commit()) return transaction;
        data.set("claims." + transaction, null);
        return null;
    }

    public synchronized void noteFailure(UUID transaction, String reason) {
        if (transaction == null) return;
        String base = "claims." + transaction + ".";
        if (!data.contains(base + "status")) return;
        data.set(base + "reason", reason == null ? "UNKNOWN" : reason);
        data.set(base + "failed-at", System.currentTimeMillis());
        commit();
    }

    public synchronized boolean complete(UUID transaction) {
        if (transaction == null) return false;
        String base = "claims." + transaction + ".";
        if (!data.contains(base + "status")) return false;
        data.set(base + "status", "COMPLETED");
        data.set(base + "completed-at", System.currentTimeMillis());
        if (!commit()) {
            data.set(base + "status", "IN_PROGRESS");
            data.set(base + "completed-at", null);
            return false;
        }
        data.set("claims." + transaction, null);
        if (!commit()) return true;
        return true;
    }

    public synchronized int reviewRequiredCount() {
        ConfigurationSection root = data.getConfigurationSection("claims");
        if (root == null) return 0;
        int count = 0;
        for (String key : root.getKeys(false)) {
            if (!"COMPLETED".equalsIgnoreCase(data.getString("claims." + key + ".status", "IN_PROGRESS"))) count++;
        }
        return count;
    }

    private void cleanupCompleted() {
        ConfigurationSection root = data.getConfigurationSection("claims");
        if (root == null) return;
        List<String> completed = new ArrayList<String>();
        for (String key : root.getKeys(false)) {
            if ("COMPLETED".equalsIgnoreCase(data.getString("claims." + key + ".status", ""))) completed.add(key);
        }
        if (completed.isEmpty()) return;
        for (String key : completed) data.set("claims." + key, null);
        if (!commit()) plugin.getLogger().warning("PlayerShop Revenue-Claim-Journal konnte COMPLETED-Tombstones nicht aufraeumen.");
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
            plugin.getLogger().log(Level.SEVERE, "PlayerShop Revenue-Claim-Journal konnte nicht gespeichert werden.", ex);
            if (temp.exists() && !temp.delete()) temp.deleteOnExit();
            return false;
        }
    }
}
