package net.skykings.combat.collection;

import net.skykings.combat.event.SkyKingsPlayerKillEvent;
import net.skykings.combat.stats.PvpStatsService;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.gui.GuiSession;
import net.skykings.core.pvp.PvpStatsSnapshot;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiItems;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistent collection of unique players defeated by each player. */
public final class HeadCollectionService implements Listener {
    public static final class Entry {
        long firstKill;
        long lastKill;
        int kills;
        String killType;
        int streak;
    }

    private final JavaPlugin plugin;
    private final File file;
    private final KillContextService context;
    private final PvpStatsService stats;
    private final Map<UUID, Map<UUID, Entry>> entries = new HashMap<UUID, Map<UUID, Entry>>();
    private volatile boolean dirty;

    public HeadCollectionService(JavaPlugin plugin, KillContextService context, PvpStatsService stats) {
        this.plugin = plugin;
        this.context = context;
        this.stats = stats;
        this.file = new File(plugin.getDataFolder(), "head-collection.yml");
        load();
        Bukkit.getScheduler().runTaskTimer(plugin, this::saveIfDirty, 200L, 200L);
    }

    @EventHandler
    public synchronized void onKill(SkyKingsPlayerKillEvent event) {
        UUID killer = event.getKillerUuid();
        UUID victim = event.getVictimUuid();
        Map<UUID, Entry> collection = entries.computeIfAbsent(killer, ignored -> new HashMap<UUID, Entry>());
        Entry entry = collection.get(victim);
        boolean first = entry == null;
        if (entry == null) {
            entry = new Entry();
            entry.firstKill = System.currentTimeMillis();
            collection.put(victim, entry);
        }
        entry.lastKill = System.currentTimeMillis();
        entry.kills++;
        entry.killType = context.consume(victim);
        entry.streak = event.getNewKillstreak();
        dirty = true;

        if (first) {
            Player player = Bukkit.getPlayer(killer);
            OfflinePlayer target = Bukkit.getOfflinePlayer(victim);
            if (player != null) {
                player.sendMessage(UiTheme.PRIMARY + "Collection erweitert");
                player.sendMessage(UiTheme.TEXT + displayName(target, victim) + UiTheme.MUTED + " wurde freigeschaltet.");
                SoundFeedback.reward(player);
            }
        }
    }

    public synchronized boolean hasCollected(UUID collector, UUID target) {
        Map<UUID, Entry> map = entries.get(collector);
        return map != null && map.containsKey(target);
    }

    public synchronized int collectedCount(UUID collector) {
        Map<UUID, Entry> map = entries.get(collector);
        return map == null ? 0 : map.size();
    }

    public void open(Player player, int page) {
        List<OfflinePlayer> players = knownPlayers(player.getUniqueId());
        int pageSize = 45;
        int pages = Math.max(1, (players.size() + pageSize - 1) / pageSize);
        int current = Math.max(1, Math.min(page, pages));

        GuiSession gui = GuiSession.create(player, UiTheme.title("Collection"), 54);
        int from = (current - 1) * pageSize;
        int to = Math.min(players.size(), from + pageSize);
        for (int index = from; index < to; index++) {
            OfflinePlayer target = players.get(index);
            UUID targetId = target.getUniqueId();
            Entry entry;
            synchronized (this) {
                Map<UUID, Entry> map = entries.get(player.getUniqueId());
                entry = map == null ? null : map.get(targetId);
            }
            int slot = index - from;
            if (entry == null) {
                gui.setItem(slot, UiItems.item(Material.SKULL_ITEM, (short) 0,
                        UiTheme.DISABLED + displayName(target, targetId),
                        UiTheme.MUTED + "Noch nicht besiegt.", "", UiTheme.STATUS_LOCKED));
            } else {
                final UUID selected = targetId;
                final int backPage = current;
                gui.setItem(slot, collectedHead(target, targetId, entry), (p,e,s) -> openDetail(p, selected, backPage));
            }
        }

        if (players.isEmpty()) gui.setItem(22, UiItems.empty("Keine Spieler", "Noch keine bekannten Spieler fuer die Collection."));
        if (current > 1) gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> { SoundFeedback.back(p); open(p, current - 1); });
        else gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> { SoundFeedback.back(p); Bukkit.dispatchCommand(p, "profile"); });
        gui.setItem(UiTheme.NAV_HOME, UiItems.item(Material.BOOK,
                UiTheme.PRIMARY + "Collection",
                UiTheme.MUTED + "Freigeschaltet",
                UiTheme.TEXT.toString() + collectedCount(player.getUniqueId()) + UiTheme.DISABLED + " / " + players.size(),
                "", UiTheme.MUTED + "Seite " + current + " / " + pages));
        if (current < pages) gui.setItem(UiTheme.NAV_NEXT, UiItems.next(), (p,e,s) -> open(p, current + 1));

        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void openDetail(Player player, UUID targetId, int backPage) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        Entry entry;
        synchronized (this) {
            Map<UUID, Entry> map = entries.get(player.getUniqueId());
            entry = map == null ? null : map.get(targetId);
        }
        if (entry == null) { open(player, backPage); return; }

        GuiSession gui = GuiSession.create(player, UiTheme.title("Collection Detail"), 27);
        gui.setItem(13, collectedHead(target, targetId, entry));
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> { SoundFeedback.back(p); open(p, backPage); });
        PvpStatsSnapshot targetStats = stats.getStats(targetId);
        gui.setItem(22, UiItems.item(Material.DIAMOND_SWORD,
                UiTheme.TEXT + "Combat",
                UiTheme.MUTED + "Server-Kills " + UiTheme.TEXT + UiFormat.number(targetStats.getKills()),
                UiTheme.MUTED + "Beststreak " + UiTheme.TEXT + targetStats.getBestStreak(),
                rarityLine(targetId)));
        GuiManager.active().open(gui);
        SoundFeedback.click(player);
    }

    private ItemStack collectedHead(OfflinePlayer target, UUID targetId, Entry entry) {
        String name = displayName(target, targetId);
        return UiItems.head(name,
                UiTheme.TEXT + name,
                UiTheme.MUTED + "Besiegt " + UiTheme.TEXT + UiFormat.number(entry.kills) + "x",
                UiTheme.MUTED + "Erster Kill " + UiTheme.TEXT + date(entry.firstKill),
                UiTheme.MUTED + "Killart " + UiTheme.TEXT + safe(entry.killType),
                UiTheme.MUTED + "Streak " + UiTheme.TEXT + entry.streak,
                rarityLine(targetId));
    }

    private String rarityLine(UUID targetId) {
        int rank = killRank(targetId);
        if (rank > 0 && rank <= 3) return UiTheme.LEGENDARY + "LEGENDARY TARGET  #" + rank;
        if (rank > 0 && rank <= 10) return UiTheme.PRIMARY + "TOP PLAYER  #" + rank;
        return UiTheme.DISABLED + "COLLECTED";
    }

    private int killRank(UUID target) {
        List<Map.Entry<UUID, PvpStatsSnapshot>> list = new ArrayList<Map.Entry<UUID, PvpStatsSnapshot>>(stats.getAllStats().entrySet());
        Collections.sort(list, (a,b) -> Long.compare(b.getValue().getKills(), a.getValue().getKills()));
        for (int i = 0; i < list.size(); i++) if (list.get(i).getKey().equals(target)) return i + 1;
        return -1;
    }

    private List<OfflinePlayer> knownPlayers(UUID viewer) {
        Set<UUID> ids = new LinkedHashSet<UUID>();
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) if (offline != null) ids.add(offline.getUniqueId());
        ids.addAll(stats.getAllStats().keySet());
        ids.remove(viewer);
        List<OfflinePlayer> result = new ArrayList<OfflinePlayer>();
        for (UUID id : ids) result.add(Bukkit.getOfflinePlayer(id));
        Collections.sort(result, new Comparator<OfflinePlayer>() {
            @Override public int compare(OfflinePlayer a, OfflinePlayer b) {
                return displayName(a, a.getUniqueId()).compareToIgnoreCase(displayName(b, b.getUniqueId()));
            }
        });
        return result;
    }

    private String displayName(OfflinePlayer player, UUID uuid) {
        if (player != null && player.getName() != null) return player.getName();
        return uuid.toString().substring(0, 8);
    }
    private String date(long timestamp) { return new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(new Date(timestamp)); }
    private String safe(String value) { return value == null || value.trim().isEmpty() ? "PvP" : value; }

    private synchronized void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) return;
        for (String collectorRaw : players.getKeys(false)) {
            try {
                UUID collector = UUID.fromString(collectorRaw);
                ConfigurationSection targets = players.getConfigurationSection(collectorRaw);
                if (targets == null) continue;
                Map<UUID, Entry> map = new HashMap<UUID, Entry>();
                for (String targetRaw : targets.getKeys(false)) {
                    try {
                        UUID target = UUID.fromString(targetRaw);
                        String base = "players." + collectorRaw + "." + targetRaw + ".";
                        Entry entry = new Entry();
                        entry.firstKill = yaml.getLong(base + "first-kill", 0L);
                        entry.lastKill = yaml.getLong(base + "last-kill", 0L);
                        entry.kills = Math.max(1, yaml.getInt(base + "kills", 1));
                        entry.killType = yaml.getString(base + "kill-type", "PvP");
                        entry.streak = Math.max(0, yaml.getInt(base + "streak", 0));
                        map.put(target, entry);
                    } catch (IllegalArgumentException ignored) { }
                }
                entries.put(collector, map);
            } catch (IllegalArgumentException ignored) { }
        }
    }

    private synchronized void saveIfDirty() { if (dirty) save(); }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<UUID, Entry>> collector : entries.entrySet()) {
            for (Map.Entry<UUID, Entry> target : collector.getValue().entrySet()) {
                String base = "players." + collector.getKey() + "." + target.getKey() + ".";
                Entry entry = target.getValue();
                yaml.set(base + "first-kill", entry.firstKill);
                yaml.set(base + "last-kill", entry.lastKill);
                yaml.set(base + "kills", entry.kills);
                yaml.set(base + "kill-type", entry.killType);
                yaml.set(base + "streak", entry.streak);
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file); dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("head-collection.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
