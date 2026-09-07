package net.skykings.combat.retention;

import net.skykings.combat.event.EventParticipationService;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiTheme;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Season-XP/PvP-Level 1-100. Nur legitime PvP-/Quest-Aktivitaet gibt XP. */
public final class SeasonProgressService implements Listener {
    private static final int XP_PER_KILL = 100;
    private static final long SAME_VICTIM_COOLDOWN = 10L * 60L * 1000L;
    private static volatile SeasonProgressService active;

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<String, Long> pairCooldown = new LinkedHashMap<String, Long>();

    public SeasonProgressService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "season-progress.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        if (!data.contains("season")) data.set("season", 1);
        active = this;
    }

    public static SeasonProgressService active() { return active; }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) return;
        if (EventParticipationService.global().isInEvent(victim.getUniqueId())
                || EventParticipationService.global().isInEvent(killer.getUniqueId())) return;
        String key = killer.getUniqueId() + ":" + victim.getUniqueId();
        long now = System.currentTimeMillis();
        Long until = pairCooldown.get(key);
        if (until != null && until > now) return;
        pairCooldown.put(key, now + SAME_VICTIM_COOLDOWN);
        if (!addXp(killer, XP_PER_KILL, "PvP Kill")) {
            pairCooldown.remove(key);
            killer.sendMessage(UiTheme.DANGER + "Season-XP konnte nicht sicher gespeichert werden.");
        }
    }

    /**
     * Fuegt XP nur hinzu, wenn der neue Stand synchron und atomar gespeichert werden konnte.
     * Bei Persistenzfehlern wird der In-Memory-Wert zurueckgesetzt und false geliefert, damit
     * uebergeordnete Settlement-Pfade fail-closed reagieren koennen.
     */
    public synchronized boolean addXp(Player player, int amount, String reason) {
        if (player == null || amount <= 0) return false;
        UUID uuid = player.getUniqueId();
        int beforeXp = getXp(uuid);
        int beforeLevel = getLevel(uuid);
        long candidate = (long) beforeXp + amount;
        int afterXp = candidate > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) candidate;
        data.set(path(uuid, "xp"), afterXp);
        if (!saveNow()) {
            data.set(path(uuid, "xp"), beforeXp);
            plugin.getLogger().warning("Season-XP konnte nicht durable gespeichert werden: " + uuid + " / " + reason);
            return false;
        }

        int afterLevel = getLevel(uuid);
        if (afterLevel > beforeLevel) {
            player.sendMessage(UiTheme.PRIMARY + "Level Up");
            player.sendMessage(UiTheme.TEXT.toString() + beforeLevel + UiTheme.MUTED + " → " + UiTheme.TEXT + afterLevel);
            SoundFeedback.levelUp(player);
        }
        return true;
    }

    public synchronized int getXp(UUID uuid) { return Math.max(0, data.getInt(path(uuid, "xp"), 0)); }
    public synchronized int getLevel(UUID uuid) {
        int xp = getXp(uuid);
        int level = 1;
        while (level < 100 && xp >= xpForLevel(level + 1)) level++;
        return level;
    }
    public synchronized int getSeason() { return Math.max(1, data.getInt("season", 1)); }
    public int xpForLevel(int level) {
        if (level <= 1) return 0;
        return (level - 1) * (level - 1) * 250;
    }
    public synchronized int xpToNext(UUID uuid) {
        int level = getLevel(uuid);
        if (level >= 100) return 0;
        return Math.max(0, xpForLevel(level + 1) - getXp(uuid));
    }

    /** Unveraenderlicher Snapshot fuer Season-Finish/Hall-of-Fame. */
    public synchronized Map<UUID, Integer> getAllXp() {
        Map<UUID, Integer> snapshot = new LinkedHashMap<UUID, Integer>();
        ConfigurationSection root = data.getConfigurationSection("players");
        if (root == null) return Collections.unmodifiableMap(snapshot);
        for (String raw : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(raw);
                snapshot.put(uuid, Math.max(0, data.getInt("players." + raw + ".xp", 0)));
            } catch (IllegalArgumentException ignored) { }
        }
        return Collections.unmodifiableMap(snapshot);
    }

    /** Nur fuer den expliziten Season-Finish-Pfad verwenden. Lifetime-Stats bleiben erhalten. */
    public synchronized int advanceSeasonAndResetXp() {
        int previous = getSeason();
        data.set("players", null);
        data.set("season", previous + 1);
        pairCooldown.clear();
        save();
        return previous + 1;
    }

    private String path(UUID uuid, String key) { return "players." + uuid + "." + key; }

    public synchronized void save() { saveNow(); }

    private boolean saveNow() {
        File parent = file.getParentFile();
        File temp = parent == null ? new File(file.getPath() + ".tmp") : new File(parent, file.getName() + ".tmp");
        try {
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Season-Datenordner konnte nicht erstellt werden.");
                return false;
            }
            data.save(temp);
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "season-progress.yml konnte nicht atomar gespeichert werden.", ex);
            if (temp.exists() && !temp.delete()) temp.deleteOnExit();
            return false;
        }
    }
}
