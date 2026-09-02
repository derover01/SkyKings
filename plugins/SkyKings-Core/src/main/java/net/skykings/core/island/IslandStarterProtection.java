package net.skykings.core.island;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** Verhindert dauerhaftes Starterloot-Farming ueber /is delete -> /is create. */
public final class IslandStarterProtection {
    private static final Object LOCK = new Object();
    private static final Set<UUID> claimed = new HashSet<UUID>();
    private static boolean loaded;

    private IslandStarterProtection() { }

    public static boolean create(IslandService islands, Player player) {
        if (islands == null || player == null) return false;
        UUID playerId = player.getUniqueId();
        boolean alreadyClaimed = hasClaimed(playerId);
        if (!islands.create(player)) return false;

        IslandService.IslandData created = islands.get(playerId);
        if (alreadyClaimed) {
            clearStarterChest(created);
            return true;
        }

        try {
            markClaimed(playerId);
        } catch (IllegalStateException ex) {
            // Fail closed: Die Insel darf bestehen bleiben, aber kein unregistriertes Starterloot.
            clearStarterChest(created);
            throw ex;
        }
        return true;
    }

    /** Vor einer Loeschung aufrufen, damit auch Bestandsinseln aus aelteren Versionen migriert sind. */
    public static void markClaimed(UUID playerId) {
        if (playerId == null) return;
        synchronized (LOCK) {
            ensureLoaded();
            if (!claimed.add(playerId)) return;
            File file = file();
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                if (!file.exists()) file.createNewFile();
                try (FileWriter writer = new FileWriter(file, true)) {
                    writer.write(playerId.toString());
                    writer.write(System.lineSeparator());
                    writer.flush();
                }
            } catch (IOException ex) {
                claimed.remove(playerId);
                plugin().getLogger().log(Level.SEVERE,
                        "Island-Starterclaim konnte nicht gespeichert werden: " + playerId, ex);
                throw new IllegalStateException("Starterclaim konnte nicht sicher persistiert werden", ex);
            }
        }
    }

    public static boolean hasClaimed(UUID playerId) {
        if (playerId == null) return false;
        synchronized (LOCK) {
            ensureLoaded();
            return claimed.contains(playerId);
        }
    }

    private static void clearStarterChest(IslandService.IslandData island) {
        if (island == null || island.getHome().getWorld() == null) return;
        Block block = island.getHome().getWorld().getBlockAt(island.centerX + 2, IslandService.Y, island.centerZ);
        if (block.getType() == Material.CHEST && block.getState() instanceof Chest) {
            Chest chest = (Chest) block.getState();
            chest.getBlockInventory().clear();
            chest.update(true);
        }
    }

    private static void ensureLoaded() {
        if (loaded) return;
        File file = file();
        if (file.exists()) {
            try {
                for (String line : java.nio.file.Files.readAllLines(file.toPath())) {
                    try { claimed.add(UUID.fromString(line.trim())); }
                    catch (IllegalArgumentException ignored) { }
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Island-Starterclaims konnten nicht geladen werden", ex);
            }
        }
        loaded = true;
    }

    private static File file() { return new File(plugin().getDataFolder(), "island-starter-claims.txt"); }
    private static JavaPlugin plugin() { return JavaPlugin.getProvidingPlugin(IslandStarterProtection.class); }
}
