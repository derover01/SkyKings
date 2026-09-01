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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Gemeinsame Leaderboard-Oberflaeche fuer Kills, Beststreak, K/D und Kopfgelder. */
public final class TopCommand implements CommandExecutor, Listener {
    private final PvpStatsService statsService;

    public TopCommand(PvpStatsService statsService) { this.statsService = statsService; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar."); return true; }
        Player player = (Player) sender;
        boolean bountyAlias = label.equalsIgnoreCase("kopfgeld") || label.equalsIgnoreCase("bounty");
        if (bountyAlias && args.length == 2) {
            placeBounty(player, args[0], args[1]); return true;
        }
        if (bountyAlias && args.length > 0) {
            player.sendMessage(UiTheme.DANGER + "Verwendung: /kopfgeld <Spieler> <Preis>");
            player.sendMessage(UiTheme.MUTED + "Beispiel: /kopfgeld Roman 250k");
            return true;
        }
        if (bountyAlias) openBounties(player); else openRoot(player);
        return true;
    }

    private void placeBounty(Player issuer, String targetName, String rawAmount) {
        BountyService bounties = BountyService.active();
        if (bounties == null) { issuer.sendMessage(UiTheme.DANGER + "Kopfgeld-System ist nicht bereit."); return; }
        OfflinePlayer target = resolve(targetName);
        if (target == null) { issuer.sendMessage(UiTheme.DANGER + "Spieler nicht gefunden."); return; }
        long amount = parseAmount(rawAmount);
        if (amount < BountyService.minPlayerBounty() || amount > BountyService.maxSingleBounty()) {
            issuer.sendMessage(UiTheme.DANGER + "Kopfgeld muss zwischen " + UiFormat.coins(BountyService.minPlayerBounty())
                    + " und " + UiFormat.coins(BountyService.maxSingleBounty()) + " liegen.");
            return;
        }
        if (!bounties.place(issuer, target, amount)) {
            issuer.sendMessage(UiTheme.DANGER + "Kopfgeld konnte nicht gesetzt werden. Pruefe Guthaben und Ziel.");
            SoundFeedback.error(issuer); return;
        }
        SoundFeedback.success(issuer);
        openBounties(issuer);
    }

    private OfflinePlayer resolve(String name) {
        Player online = Bukkit.getPlayerExact(name); if (online != null) return online;
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline.getName() != null && offline.getName().equalsIgnoreCase(name)) return offline;
        }
        return null;
    }

    private long parseAmount(String raw) {
        if (raw == null) return -1L;
        String value = raw.toLowerCase(Locale.ROOT).replace(".", "").replace("_", "").trim();
        long multiplier = 1L;
        if (value.endsWith("k")) { multiplier = 1_000L; value = value.substring(0, value.length() - 1); }
        else if (value.endsWith("m")) { multiplier = 1_000_000L; value = value.substring(0, value.length() - 1); }
        try { return Math.multiplyExact(Long.parseLong(value), multiplier); }
        catch (Exception ex) { return -1L; }
    }

    private void openRoot(Player player) {
        GuiSession gui = GuiSession.create(player, UiTheme.title("Leaderboards"), 27);
        gui.setItem(10, UiItems.item(Material.DIAMOND_SWORD, UiTheme.PRIMARY + "Kills", UiTheme.MUTED + "Die meisten legitimen PvP-Kills.", "", UiItems.action("Oeffnen")), (p,e,s) -> openLeaderboard(p, Metric.KILLS));
        gui.setItem(12, UiItems.item(Material.BLAZE_POWDER, UiTheme.PRIMARY + "Beststreak", UiTheme.MUTED + "Die hoechsten Killstreaks.", "", UiItems.action("Oeffnen")), (p,e,s) -> openLeaderboard(p, Metric.STREAK));
        gui.setItem(14, UiItems.item(Material.NETHER_STAR, UiTheme.PRIMARY + "K/D", UiTheme.MUTED + "Die staerksten K/D-Werte.", "", UiItems.action("Oeffnen")), (p,e,s) -> openLeaderboard(p, Metric.KD));
        gui.setItem(16, UiItems.item(Material.SKULL_ITEM, (short) 3, UiTheme.WARNING + "Kopfgelder",
                UiTheme.MUTED + "Streak- und Spieler-Kopfgelder.", "", UiItems.action("Bounty Board")), (p,e,s) -> openBounties(p));
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> Bukkit.dispatchCommand(p, "profile"));
        guiManagerOpen(gui, player);
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
            gui.setItem(i, playerHead(entry.getKey(), entry.getValue(), i + 1, metric), (p,e,s) -> Bukkit.dispatchCommand(p, "profile " + selected));
        }
        if (entries.isEmpty()) gui.setItem(22, UiItems.empty("Keine Daten", "Noch keine PvP-Stats vorhanden."));
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> openRoot(p));
        guiManagerOpen(gui, player);
    }

    private void openBounties(Player player) {
        BountyService bounties = BountyService.active();
        GuiSession gui = GuiSession.create(player, UiTheme.title("Kopfgelder"), 54);
        Set<UUID> ids = new LinkedHashSet<UUID>();
        if (bounties != null) ids.addAll(bounties.getPlayerBounties().keySet());
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (BountyService.getCoinBounty(statsService.getStats(online.getUniqueId()).getCurrentStreak()) > 0L) ids.add(online.getUniqueId());
        }
        List<BountyTarget> targets = new ArrayList<BountyTarget>();
        for (UUID uuid : ids) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            PvpStatsSnapshot stats = statsService.getStats(uuid);
            long placed = bounties == null ? 0L : bounties.getPlayerBounty(uuid);
            long streakCoins = BountyService.getCoinBounty(stats.getCurrentStreak());
            long stars = BountyService.getStarBounty(stats.getCurrentStreak());
            targets.add(new BountyTarget(offline, placed, streakCoins, stars, stats.getCurrentStreak()));
        }
        targets.sort((a,b) -> Long.compare(b.totalCoins(), a.totalCoins()));

        int slot = 0;
        for (BountyTarget target : targets) {
            if (slot >= 45) break;
            String name = target.player.getName() == null ? target.player.getUniqueId().toString().substring(0,8) : target.player.getName();
            final String selected = name;
            gui.setItem(slot++, UiItems.head(name, UiTheme.WARNING + name,
                    target.player.isOnline() ? UiTheme.SUCCESS + "ONLINE - jagdbar" : UiTheme.MUTED + "Offline",
                    target.placed > 0L ? UiTheme.MUTED + "Spieler-Kopfgeld: " + UiTheme.LEGENDARY + UiFormat.coins(target.placed) : UiTheme.DISABLED + "Kein Spieler-Kopfgeld",
                    target.streakCoins > 0L ? UiTheme.MUTED + "Streak-Bounty: " + UiTheme.TEXT + UiFormat.coins(target.streakCoins) : UiTheme.DISABLED + "Keine Streak-Bounty",
                    target.stars > 0L ? UiTheme.MUTED + "+ " + UiTheme.TEXT + target.stars + " SkyKings Sterne" : "",
                    "", UiItems.action("Profile oeffnen")), (p,e,s) -> Bukkit.dispatchCommand(p, "profile " + selected));
        }
        if (targets.isEmpty()) gui.setItem(22, UiItems.empty("Keine Kopfgelder", "Setze eins mit /kopfgeld <Spieler> <Preis>."));
        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> openRoot(p));
        gui.setItem(UiTheme.NAV_HOME, UiItems.item(Material.GOLD_INGOT, UiTheme.WARNING + "Bounty Board",
                UiTheme.MUTED + "/kopfgeld <Spieler> <Preis>", UiTheme.MUTED + "Minimum: " + UiFormat.coins(BountyService.minPlayerBounty())));
        guiManagerOpen(gui, player);
    }

    private void guiManagerOpen(GuiSession gui, Player player) { GuiManager.active().open(gui); SoundFeedback.menuOpen(player); }

    private org.bukkit.inventory.ItemStack playerHead(UUID uuid, PvpStatsSnapshot value, int position, Metric metric) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline.getName() == null ? uuid.toString().substring(0, 8) : offline.getName();
        String rankColor = position == 1 ? UiTheme.LEGENDARY.toString() : position == 2 ? UiTheme.PRIMARY.toString() : position == 3 ? UiTheme.WARNING.toString() : UiTheme.TEXT.toString();
        return UiItems.head(name, rankColor + "#" + position + " " + UiTheme.TEXT + name,
                metric.line(value), UiTheme.MUTED + "Kills " + UiTheme.TEXT + UiFormat.number(value.getKills()),
                UiTheme.MUTED + "K/D " + UiTheme.TEXT + kd(value), UiTheme.MUTED + "Beststreak " + UiTheme.TEXT + value.getBestStreak(), "", UiItems.action("Profile oeffnen"));
    }

    private static String kd(PvpStatsSnapshot value) { return String.format(Locale.GERMANY, "%.2f", value.getKd()); }

    private static final class BountyTarget {
        final OfflinePlayer player; final long placed; final long streakCoins; final long stars; final int streak;
        BountyTarget(OfflinePlayer player, long placed, long streakCoins, long stars, int streak) { this.player=player; this.placed=placed; this.streakCoins=streakCoins; this.stars=stars; this.streak=streak; }
        long totalCoins() { return placed + streakCoins; }
    }

    private enum Metric {
        KILLS("Kills") { @Override Comparator<Map.Entry<UUID,PvpStatsSnapshot>> comparator(){ return Comparator.comparingLong((Map.Entry<UUID,PvpStatsSnapshot> e)->e.getValue().getKills()).reversed(); } @Override String line(PvpStatsSnapshot s){ return UiTheme.PRIMARY + UiFormat.number(s.getKills()) + " Kills"; } },
        STREAK("Beststreak") { @Override Comparator<Map.Entry<UUID,PvpStatsSnapshot>> comparator(){ return Comparator.comparingInt((Map.Entry<UUID,PvpStatsSnapshot> e)->e.getValue().getBestStreak()).reversed(); } @Override String line(PvpStatsSnapshot s){ return UiTheme.PRIMARY.toString()+s.getBestStreak()+" Beststreak"; } },
        KD("K/D") { @Override Comparator<Map.Entry<UUID,PvpStatsSnapshot>> comparator(){ return Comparator.comparingDouble((Map.Entry<UUID,PvpStatsSnapshot> e)->e.getValue().getKd()).reversed(); } @Override String line(PvpStatsSnapshot s){ return UiTheme.PRIMARY+kd(s)+" K/D"; } };
        final String label; Metric(String label){this.label=label;} abstract Comparator<Map.Entry<UUID,PvpStatsSnapshot>> comparator(); abstract String line(PvpStatsSnapshot s);
    }
}
