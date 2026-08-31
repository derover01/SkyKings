package net.skykings.combat.stats;

import net.skykings.combat.kill.BountyService;
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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Gemeinsame Leaderboard-Oberflaeche fuer Kills, Beststreak, K/D und aktive Kopfgelder. */
public final class TopCommand implements CommandExecutor, Listener {
    private final PvpStatsService statsService;

    public TopCommand(PvpStatsService statsService) {
        this.statsService = statsService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (label.equalsIgnoreCase("kopfgeld") || label.equalsIgnoreCase("bounty")) {
            openBounties(player);
        } else {
            openRoot(player);
        }
        return true;
    }

    private void openRoot(Player player) {
        GuiSession gui = GuiSession.create(player, UiTheme.title("Leaderboards"), 27);
        gui.setItem(10, UiItems.item(Material.DIAMOND_SWORD,
                UiTheme.PRIMARY + "Kills",
                UiTheme.MUTED + "Die meisten legitimen PvP-Kills.",
                "",
                UiItems.action("Klicken zum Oeffnen")), (p,e,s) -> openLeaderboard(p, Metric.KILLS));
        gui.setItem(12, UiItems.item(Material.BLAZE_POWDER,
                UiTheme.PRIMARY + "Beststreak",
                UiTheme.MUTED + "Die hoechsten Killstreaks.",
                "",
                UiItems.action("Klicken zum Oeffnen")), (p,e,s) -> openLeaderboard(p, Metric.STREAK));
        gui.setItem(14, UiItems.item(Material.NETHER_STAR,
                UiTheme.PRIMARY + "K/D",
                UiTheme.MUTED + "Die staerksten K/D-Werte.",
                "",
                UiItems.action("Klicken zum Oeffnen")), (p,e,s) -> openLeaderboard(p, Metric.KD));
        gui.setItem(16, UiItems.item(Material.SKULL_ITEM, (short) 3,
                UiTheme.WARNING + "Kopfgelder",
                UiTheme.MUTED + "Aktive Streak-Ziele auf dem Server.",
                "",
                UiItems.action("Bounty Board oeffnen")), (p,e,s) -> openBounties(p));
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> Bukkit.dispatchCommand(p, "profile"));
        gui.setItem(UiTheme.NAV_HOME, UiItems.item(Material.COMPASS,
                UiTheme.TEXT + "Leaderboards",
                UiTheme.MUTED + "Combat-Ranglisten und Bounties"));
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void openLeaderboard(Player player, Metric metric) {
        GuiSession gui = GuiSession.create(player, UiTheme.title("Top " + metric.label), 54);
        List<Map.Entry<UUID, PvpStatsSnapshot>> entries = new ArrayList<Map.Entry<UUID, PvpStatsSnapshot>>(statsService.getAllStats().entrySet());
        entries.sort(metric.comparator());
        int limit = Math.min(45, entries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<UUID, PvpStatsSnapshot> entry = entries.get(i);
            OfflinePlayer offline = Bukkit.getOfflinePlayer(entry.getKey());
            String name = offline.getName() == null ? entry.getKey().toString().substring(0, 8) : offline.getName();
            final String selected = name;
            gui.setItem(i, playerHead(entry.getKey(), entry.getValue(), i + 1, metric),
                    (p,e,s) -> Bukkit.dispatchCommand(p, "profile " + selected));
        }
        if (entries.isEmpty()) gui.setItem(22, UiItems.empty("Keine Daten", "Noch keine PvP-Stats vorhanden."));
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> { SoundFeedback.back(p); openRoot(p); });
        gui.setItem(UiTheme.NAV_HOME, UiItems.item(Material.PAPER,
                UiTheme.PRIMARY + metric.label,
                UiTheme.MUTED + "Sortiert nach " + metric.description,
                UiTheme.TEXT.toString() + entries.size() + UiTheme.MUTED + " Spieler gewertet"));
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void openBounties(Player player) {
        GuiSession gui = GuiSession.create(player, UiTheme.title("Kopfgelder"), 54);
        List<Player> targets = new ArrayList<Player>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            PvpStatsSnapshot value = statsService.getStats(online.getUniqueId());
            if (BountyService.getCoinBounty(value.getCurrentStreak()) > 0L) targets.add(online);
        }
        targets.sort((a, b) -> Integer.compare(
                statsService.getStats(b.getUniqueId()).getCurrentStreak(),
                statsService.getStats(a.getUniqueId()).getCurrentStreak()));

        int slot = 0;
        for (Player target : targets) {
            if (slot >= 45) break;
            PvpStatsSnapshot value = statsService.getStats(target.getUniqueId());
            int streak = value.getCurrentStreak();
            long coins = BountyService.getCoinBounty(streak);
            long stars = BountyService.getStarBounty(streak);
            final String name = target.getName();
            gui.setItem(slot++, UiItems.head(name,
                    UiTheme.WARNING + name,
                    UiTheme.MUTED + "Aktuelle Streak " + UiTheme.TEXT + streak,
                    UiTheme.MUTED + "Kopfgeld " + UiTheme.TEXT + UiFormat.coins(coins),
                    UiTheme.MUTED + "+ " + UiTheme.TEXT + UiFormat.number(stars) + UiTheme.MUTED + " SkyKings Sterne",
                    "",
                    UiTheme.STATUS_READY,
                    UiItems.action("Profile oeffnen")), (p,e,s) -> Bukkit.dispatchCommand(p, "profile " + name));
        }
        if (targets.isEmpty()) {
            gui.setItem(22, UiItems.empty("Keine aktiven Kopfgelder", "Noch hat niemand eine 5er Streak."));
        }
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> { SoundFeedback.back(p); openRoot(p); });
        gui.setItem(UiTheme.NAV_HOME, UiItems.item(Material.GOLD_INGOT,
                UiTheme.WARNING + "Bounty Board",
                UiTheme.MUTED + "Nur aktuell jagdbare Online-Ziele.",
                UiTheme.TEXT.toString() + targets.size() + UiTheme.MUTED + " aktive Kopfgelder"));
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    private org.bukkit.inventory.ItemStack playerHead(UUID uuid, PvpStatsSnapshot value, int position, Metric metric) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline.getName() == null ? uuid.toString().substring(0, 8) : offline.getName();
        String rankColor = position == 1 ? UiTheme.LEGENDARY.toString()
                : position == 2 ? UiTheme.PRIMARY.toString()
                : position == 3 ? UiTheme.WARNING.toString() : UiTheme.TEXT.toString();
        return UiItems.head(name,
                rankColor + "#" + position + " " + UiTheme.TEXT + name,
                metric.line(value),
                UiTheme.MUTED + "Kills " + UiTheme.TEXT + UiFormat.number(value.getKills()),
                UiTheme.MUTED + "K/D " + UiTheme.TEXT + kd(value),
                UiTheme.MUTED + "Beststreak " + UiTheme.TEXT + value.getBestStreak(),
                "",
                UiItems.action("Profile oeffnen"));
    }

    private static String kd(PvpStatsSnapshot value) {
        return String.format(Locale.GERMANY, "%.2f", value.getKd());
    }

    private enum Metric {
        KILLS("Kills", "Lifetime Kills") {
            @Override Comparator<Map.Entry<UUID, PvpStatsSnapshot>> comparator() {
                return Comparator.comparingLong((Map.Entry<UUID, PvpStatsSnapshot> e) -> e.getValue().getKills()).reversed();
            }
            @Override String line(PvpStatsSnapshot s) { return UiTheme.PRIMARY + UiFormat.number(s.getKills()) + " Kills"; }
        },
        STREAK("Beststreak", "Beststreak") {
            @Override Comparator<Map.Entry<UUID, PvpStatsSnapshot>> comparator() {
                return Comparator.comparingInt((Map.Entry<UUID, PvpStatsSnapshot> e) -> e.getValue().getBestStreak()).reversed();
            }
            @Override String line(PvpStatsSnapshot s) { return UiTheme.PRIMARY.toString() + s.getBestStreak() + " Beststreak"; }
        },
        KD("K/D", "Kill/Death Ratio") {
            @Override Comparator<Map.Entry<UUID, PvpStatsSnapshot>> comparator() {
                return Comparator.comparingDouble((Map.Entry<UUID, PvpStatsSnapshot> e) -> e.getValue().getKd()).reversed();
            }
            @Override String line(PvpStatsSnapshot s) { return UiTheme.PRIMARY + kd(s) + " K/D"; }
        };

        final String label;
        final String description;
        Metric(String label, String description) { this.label = label; this.description = description; }
        abstract Comparator<Map.Entry<UUID, PvpStatsSnapshot>> comparator();
        abstract String line(PvpStatsSnapshot s);
    }
}
