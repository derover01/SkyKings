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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

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
        addXp(killer, XP_PER_KILL, "PvP Kill");
    }

    public void addXp(Player player, int amount, String reason) {
        if (player == null || amount <= 0) return;
        UUID uuid = player.getUniqueId();
        int before = getLevel(uuid);
        data.set(path(uuid, "xp"), getXp(uuid) + amount);
        int after = getLevel(uuid);
        save();
        if (after > before) {
            player.sendMessage(UiTheme.PRIMARY + "Level Up");
            player.sendMessage(UiTheme.TEXT.toString() + before + UiTheme.MUTED + " → " + UiTheme.TEXT + after);
            SoundFeedback.levelUp(player);
        }
    }

    public int getXp(UUID uuid) { return Math.max(0, data.getInt(path(uuid, "xp"), 0)); }
    public int getLevel(UUID uuid) {
        int xp = getXp(uuid);
        int level = 1;
        while (level < 100 && xp >= xpForLevel(level + 1)) level++;
        return level;
    }
    public int getSeason() { return Math.max(1, data.getInt("season", 1)); }
    public int xpForLevel(int level) {
        if (level <= 1) return 0;
        return (level - 1) * (level - 1) * 250;
    }
    public int xpToNext(UUID uuid) {
        int level = getLevel(uuid);
        if (level >= 100) return 0;
        return Math.max(0, xpForLevel(level + 1) - getXp(uuid));
    }

    /** Unveraenderlicher Snapshot fuer Season-Finish/Hall-of-Fame. */
    public Map<UUID, Integer> getAllXp() {
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
    public int advanceSeasonAndResetXp() {
        int previous = getSeason();
        data.set("players", null);
        data.set("season", previous + 1);
        pairCooldown.clear();
        save();
        return previous + 1;
    }

    private String path(UUID uuid, String key) { return "players." + uuid + "." + key; }

    public void save() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            data.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("season-progress.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
