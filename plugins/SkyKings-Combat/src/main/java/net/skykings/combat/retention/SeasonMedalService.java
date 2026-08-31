package net.skykings.combat.retention;

import net.skykings.combat.event.KingAltarCaptureEvent;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Permanente kosmetische Medaillen und sichere Archivierung abgeschlossener Seasons. */
public final class SeasonMedalService implements Listener {
    private final JavaPlugin plugin;
    private final SeasonProgressService progress;
    private final PvpStatsService stats;
    private final LegacyHallService hall;
    private final File file;
    private final YamlConfiguration data;
    private boolean finishing;

    public SeasonMedalService(JavaPlugin plugin, SeasonProgressService progress, PvpStatsService stats, LegacyHallService hall) {
        this.plugin = plugin;
        this.progress = progress;
        this.stats = stats;
        this.hall = hall;
        this.file = new File(plugin.getDataFolder(), "season-medals.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    @EventHandler
    public void onKill(SkyKingsPlayerKillEvent event) {
        PvpStatsSnapshot snapshot = stats.getStats(event.getKillerUuid());
        if (snapshot.getKills() >= 5000L) award(event.getKillerUuid(), "5000_KILLS");
    }

    @EventHandler
    public void onKoth(KingAltarCaptureEvent event) {
        String path = "koth." + progress.getSeason() + "." + event.getPlayerUuid();
        data.set(path, data.getInt(path, 0) + 1);
        save();
    }

    public boolean award(UUID uuid, String medal) {
        if (uuid == null || medal == null || medal.trim().isEmpty()) return false;
        List<String> current = data.getStringList("players." + uuid + ".medals");
        if (current.contains(medal)) return false;
        current.add(medal);
        data.set("players." + uuid + ".medals", current);
        save();
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendMessage(UiTheme.PRIMARY + "Medal unlocked");
            player.sendMessage(UiTheme.TEXT + display(medal));
            SoundFeedback.reward(player);
        }
        return true;
    }

    public List<String> getMedals(UUID uuid) {
        List<String> medals = new ArrayList<String>(data.getStringList("players." + uuid + ".medals"));
        if (stats.getStats(uuid).getKills() >= 5000L && !medals.contains("5000_KILLS")) {
            award(uuid, "5000_KILLS");
            medals = new ArrayList<String>(data.getStringList("players." + uuid + ".medals"));
        }
        return medals;
    }

    /** Archive -> Medal-Awards -> Reset. Reset passiert nur, wenn vorher ein Ranking-Snapshot existiert. */
    public synchronized boolean finishSeason() {
        if (finishing) return false;
        finishing = true;
        try {
            final int season = progress.getSeason();
            List<Map.Entry<UUID, Integer>> ranking = new ArrayList<Map.Entry<UUID, Integer>>(progress.getAllXp().entrySet());
            Collections.sort(ranking, new Comparator<Map.Entry<UUID, Integer>>() {
                @Override public int compare(Map.Entry<UUID, Integer> a, Map.Entry<UUID, Integer> b) {
                    int xpCompare = Integer.compare(b.getValue(), a.getValue());
                    if (xpCompare != 0) return xpCompare;
                    return a.getKey().toString().compareTo(b.getKey().toString());
                }
            });
            if (ranking.isEmpty()) return false;

            hall.archive(season, ranking);
            for (int i = 0; i < Math.min(10, ranking.size()); i++) {
                UUID uuid = ranking.get(i).getKey();
                award(uuid, "SEASON_" + season + "_TOP_10");
                if (i < 3) award(uuid, "SEASON_" + season + "_TOP_3");
                if (i == 0) award(uuid, "SEASON_" + season + "_CHAMPION");
            }

            UUID kothChampion = topKoth(season);
            if (kothChampion != null) award(kothChampion, "SEASON_" + season + "_KOTH_CHAMPION");

            data.set("finished-seasons." + season + ".completed-at", System.currentTimeMillis());
            data.set("koth." + season, null);
            save();
            progress.advanceSeasonAndResetXp();
            Bukkit.broadcastMessage(UiTheme.LEGENDARY + "Season " + season + " abgeschlossen");
            Bukkit.broadcastMessage(UiTheme.MUTED + "Legacy Hall und permanente Medaillen wurden gespeichert.");
            return true;
        } finally {
            finishing = false;
        }
    }

    private UUID topKoth(int season) {
        ConfigurationSection root = data.getConfigurationSection("koth." + season);
        if (root == null) return null;
        UUID best = null;
        int bestValue = 0;
        for (String raw : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(raw);
                int value = root.getInt(raw, 0);
                if (value > bestValue) { best = uuid; bestValue = value; }
            } catch (IllegalArgumentException ignored) { }
        }
        return best;
    }

    public void open(Player viewer, UUID targetUuid) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        String targetName = target.getName() == null ? targetUuid.toString().substring(0, 8) : target.getName();
        List<String> medals = getMedals(targetUuid);
        GuiSession gui = GuiSession.create(viewer, UiTheme.title("Medals"), 54);
        gui.setItem(4, UiItems.head(targetName, UiTheme.TEXT + targetName,
                UiTheme.MUTED + "Permanente Auszeichnungen", UiTheme.TEXT.toString() + medals.size() + UiTheme.MUTED + " Medaillen"));
        int slot = 9;
        for (String medal : medals) {
            if (slot >= 45) break;
            gui.setItem(slot++, medalItem(medal));
        }
        if (medals.isEmpty()) gui.setItem(22, UiItems.empty("Keine Medaillen", "Permanente Auszeichnungen erscheinen hier."));
        gui.setItem(UiTheme.NAV_HOME, UiItems.item(Material.NETHER_STAR, UiTheme.PRIMARY + "Medals",
                UiTheme.MUTED + "Bleiben ueber Season-Resets erhalten."));
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(viewer);
    }

    private org.bukkit.inventory.ItemStack medalItem(String medal) {
        Material material = medal.contains("CHAMPION") ? Material.GOLD_INGOT
                : medal.contains("TOP_3") ? Material.DIAMOND
                : medal.contains("TOP_10") ? Material.EMERALD
                : Material.IRON_INGOT;
        return UiItems.item(material,
                medal.contains("CHAMPION") ? UiTheme.LEGENDARY + display(medal) : UiTheme.TEXT + display(medal),
                UiTheme.MUTED + description(medal),
                "",
                UiTheme.DISABLED + "PERMANENT");
    }

    public String display(String medal) {
        if ("5000_KILLS".equals(medal)) return "5.000 Kills";
        if (medal.matches("SEASON_\\d+_KOTH_CHAMPION")) return "KOTH Champion • Season " + seasonNumber(medal);
        if (medal.matches("SEASON_\\d+_CHAMPION")) return "Season " + seasonNumber(medal) + " Champion";
        if (medal.matches("SEASON_\\d+_TOP_3")) return "Season " + seasonNumber(medal) + " Top 3";
        if (medal.matches("SEASON_\\d+_TOP_10")) return "Season " + seasonNumber(medal) + " Top 10";
        return medal.replace('_', ' ');
    }

    private String description(String medal) {
        if ("5000_KILLS".equals(medal)) return "5.000 legitime Lifetime-PvP-Kills erreicht.";
        if (medal.contains("KOTH_CHAMPION")) return "Meiste King-Altar-Captures der Season.";
        if (medal.endsWith("_CHAMPION")) return "Platz 1 nach Season-XP.";
        if (medal.endsWith("_TOP_3")) return "Top 3 nach Season-XP.";
        if (medal.endsWith("_TOP_10")) return "Top 10 nach Season-XP.";
        return "Permanente SkyKings-Auszeichnung.";
    }

    private int seasonNumber(String medal) {
        try { return Integer.parseInt(medal.split("_")[1]); }
        catch (RuntimeException ex) { return 0; }
    }

    public void save() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            data.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("season-medals.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
