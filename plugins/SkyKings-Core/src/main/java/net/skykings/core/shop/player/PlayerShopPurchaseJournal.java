package net.skykings.core.shop.player;

import org.bukkit.Material;
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

/**
 * Write-ahead journal fuer PlayerShop-Kaeufe ueber Shop-YAML, Economy-SQLite und player.dat.
 * Jeder Eintrag wird vor der ersten Mutation als IN_PROGRESS gespeichert und erst nach allen
 * durable Commits auf COMPLETED gesetzt. Ein Hard-Crash dazwischen bleibt dadurch sichtbar.
 */
public final class PlayerShopPurchaseJournal {
    public static final String FILE_NAME = "player-shop-purchase-journal.yml";
    private static volatile PlayerShopPurchaseJournal active;

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public PlayerShopPurchaseJournal(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        this.data = YamlConfiguration.loadConfiguration(file);
        cleanupCompleted();
        active = this;
    }

    public static PlayerShopPurchaseJournal active() { return active; }

    public synchronized UUID begin(UUID shopId, int offerIndex, UUID buyer, UUID owner,
                                   Material material, short itemData, int top, int middle,
                                   long price, long sellerRevenue, long previousRevenue) {
        if (shopId == null || buyer == null || owner == null || material == null) return null;
        UUID transaction = UUID.randomUUID();
        String base = "transactions." + transaction + ".";
        data.set(base + "status", "IN_PROGRESS");
        data.set(base + "shop", shopId.toString());
        data.set(base + "offer", offerIndex);
        data.set(base + "buyer", buyer.toString());
        data.set(base + "owner", owner.toString());
        data.set(base + "material", material.name());
        data.set(base + "data", itemData);
        data.set(base + "top", top);
        data.set(base + "middle", middle);
        data.set(base + "price", price);
        data.set(base + "seller-revenue", sellerRevenue);
        data.set(base + "previous-revenue", previousRevenue);
        data.set(base + "created-at", System.currentTimeMillis());
        if (commit()) return transaction;
        data.set("transactions." + transaction, null);
        return null;
    }

    public synchronized void noteFailure(UUID transaction, String reason) {
        if (transaction == null) return;
        String base = "transactions." + transaction + ".";
        if (!data.contains(base + "status")) return;
        data.set(base + "reason", reason == null ? "UNKNOWN" : reason);
        data.set(base + "failed-at", System.currentTimeMillis());
        commit();
    }

    /**
     * Zweistufiger Abschluss: Erst wird ein dauerhafter COMPLETED-Tombstone geschrieben. Erst
     * danach wird der Eintrag aufgeraumt. Scheitert nur das Aufraeumen, ist beim Restart klar,
     * dass niemals Recovery/Review fuer einen bereits fertig committed Kauf noetig ist.
     */
    public synchronized boolean complete(UUID transaction) {
        if (transaction == null) return false;
        String base = "transactions." + transaction + ".";
        if (!data.contains(base + "status")) return false;
        data.set(base + "status", "COMPLETED");
        data.set(base + "completed-at", System.currentTimeMillis());
        if (!commit()) {
            data.set(base + "status", "IN_PROGRESS");
            data.set(base + "completed-at", null);
            return false;
        }
        data.set("transactions." + transaction, null);
        if (!commit()) {
            // Auf Disk liegt der zuvor erfolgreich geschriebene COMPLETED-Tombstone.
            return true;
        }
        return true;
    }

    public synchronized int reviewRequiredCount() {
        ConfigurationSection root = data.getConfigurationSection("transactions");
        if (root == null) return 0;
        int count = 0;
        for (String key : root.getKeys(false)) {
            String status = data.getString("transactions." + key + ".status", "IN_PROGRESS");
            if (!"COMPLETED".equalsIgnoreCase(status)) count++;
        }
        return count;
    }

    private void cleanupCompleted() {
        ConfigurationSection root = data.getConfigurationSection("transactions");
        if (root == null) return;
        List<String> completed = new ArrayList<String>();
        for (String key : root.getKeys(false)) {
            if ("COMPLETED".equalsIgnoreCase(data.getString("transactions." + key + ".status", ""))) {
                completed.add(key);
            }
        }
        if (completed.isEmpty()) return;
        for (String key : completed) data.set("transactions." + key, null);
        if (!commit()) {
            plugin.getLogger().warning("PlayerShop Purchase-Journal konnte alte COMPLETED-Tombstones nicht aufraeumen.");
        }
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
            plugin.getLogger().log(Level.SEVERE, "PlayerShop Purchase-Journal konnte nicht gespeichert werden.", ex);
            if (temp.exists() && !temp.delete()) temp.deleteOnExit();
            return false;
        }
    }
}
