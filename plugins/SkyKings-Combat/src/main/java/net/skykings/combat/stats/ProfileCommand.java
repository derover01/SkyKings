package net.skykings.combat.stats;

import net.skykings.combat.collection.HeadCollectionService;
import net.skykings.combat.collection.RevengeService;
import net.skykings.combat.map.zone.MapMasteryService;
import net.skykings.combat.retention.SeasonMedalService;
import net.skykings.combat.retention.SeasonProgressService;
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

import java.util.Locale;
import java.util.UUID;

/**
 * Zentrale Game-Profile-Oberflaeche. /stats wird bewusst auf denselben Screen geroutet,
 * damit Combat, Progression, Collection und History wie ein Produkt wirken.
 */
public final class ProfileCommand implements CommandExecutor {
    private final PvpStatsService stats;
    private final SeasonProgressService season;
    private final HeadCollectionService collection;
    private final SeasonMedalService medals;
    private final MapMasteryService mastery;
    private final RevengeService revenge;

    public ProfileCommand(PvpStatsService stats, SeasonProgressService season,
                          HeadCollectionService collection, SeasonMedalService medals,
                          MapMasteryService mastery, RevengeService revenge) {
        this.stats = stats;
        this.season = season;
        this.collection = collection;
        this.medals = medals;
        this.mastery = mastery;
        this.revenge = revenge;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player viewer = (Player) sender;
        OfflinePlayer target = resolve(viewer, args);
        if (target == null) {
            viewer.sendMessage(UiTheme.DANGER + "Spieler nicht gefunden.");
            SoundFeedback.error(viewer);
            return true;
        }
        open(viewer, target);
        return true;
    }

    public void open(Player viewer, OfflinePlayer target) {
        UUID targetId = target.getUniqueId();
        String name = target.getName() == null ? targetId.toString().substring(0, 8) : target.getName();
        PvpStatsSnapshot combat = stats.getStats(targetId);
        int level = season.getLevel(targetId);
        int xp = season.getXp(targetId);
        int nextXp = level >= 100 ? xp : season.xpForLevel(level + 1);
        int currentLevelBase = season.xpForLevel(level);
        long progressCurrent = Math.max(0, xp - currentLevelBase);
        long progressMax = level >= 100 ? 1L : Math.max(1, nextXp - currentLevelBase);

        GuiSession gui = GuiSession.create(viewer, UiTheme.title("Profile"), 54);
        gui.setItem(4, UiItems.head(name,
                UiTheme.TEXT + name,
                UiTheme.MUTED + "Combat Profile",
                UiTheme.TEXT + UiFormat.number(combat.getKills()) + UiTheme.MUTED + " Kills  •  "
                        + UiTheme.TEXT + kd(combat) + UiTheme.MUTED + " K/D",
                UiTheme.MUTED + "Level " + UiTheme.TEXT + level));

        gui.setItem(19, UiItems.item(Material.DIAMOND_SWORD,
                UiTheme.PRIMARY + "Combat",
                UiTheme.MUTED + "Kills " + UiTheme.TEXT + UiFormat.number(combat.getKills()),
                UiTheme.MUTED + "Tode " + UiTheme.TEXT + UiFormat.number(combat.getDeaths()),
                UiTheme.MUTED + "K/D " + UiTheme.TEXT + kd(combat),
                UiTheme.MUTED + "Beststreak " + UiTheme.TEXT + combat.getBestStreak()));

        gui.setItem(21, UiItems.item(Material.EXP_BOTTLE,
                UiTheme.PRIMARY + "Progression",
                UiTheme.MUTED + "Season " + UiTheme.TEXT + season.getSeason(),
                UiTheme.MUTED + "Level " + UiTheme.TEXT + level,
                UiFormat.progress(progressCurrent, progressMax, 10),
                UiTheme.MUTED + "Season XP " + UiTheme.TEXT + UiFormat.number(xp)), (p,e,s) -> {
            if (targetId.equals(p.getUniqueId())) Bukkit.dispatchCommand(p, "season");
        });

        boolean self = viewer.getUniqueId().equals(targetId);
        boolean collected = self ? false : collection.hasCollected(viewer.getUniqueId(), targetId);
        gui.setItem(23, UiItems.item(Material.SKULL_ITEM, (short) (collected ? 3 : 0),
                UiTheme.PRIMARY + "Collection",
                self ? UiTheme.MUTED + "Deine Head Collection"
                        : (collected ? UiTheme.SUCCESS + "COLLECTED" : UiTheme.DISABLED + "LOCKED"),
                self ? UiTheme.MUTED + "Freigeschaltet " + UiTheme.TEXT + collection.collectedCount(viewer.getUniqueId())
                        : UiTheme.MUTED + "Besiege " + UiTheme.TEXT + name + UiTheme.MUTED + " fuer den Kopf.",
                "",
                self ? UiItems.action("Klicken zum Oeffnen") : UiTheme.MUTED + "Collection ist persoenlich"), (p,e,s) -> {
            if (self) Bukkit.dispatchCommand(p, "collection");
        });

        int medalCount = medals.getMedals(targetId).size();
        gui.setItem(25, UiItems.item(Material.GOLD_INGOT,
                UiTheme.PRIMARY + "Achievements",
                UiTheme.MUTED + "Permanente Medaillen " + UiTheme.TEXT + medalCount,
                UiTheme.MUTED + "Map Mastery " + UiTheme.TEXT + mastery.getTitle(targetId),
                "",
                UiItems.action("Medaillen anzeigen")), (p,e,s) -> Bukkit.dispatchCommand(p, "medals " + name));

        UUID revengeTarget = revenge.getRevengeTarget(viewer.getUniqueId());
        String rival = revengeTarget == null ? "Kein Revenge Target" : display(revengeTarget);
        gui.setItem(29, UiItems.item(Material.IRON_SWORD,
                UiTheme.PRIMARY + "Rivals",
                revengeTarget != null && revengeTarget.equals(targetId) ? UiTheme.WARNING + "REVENGE TARGET"
                        : UiTheme.MUTED + rival,
                UiTheme.MUTED + "Dein letzter Killer kann mit Revenge besiegt werden."));

        gui.setItem(31, UiItems.item(Material.NETHER_STAR,
                UiTheme.PRIMARY + "Events",
                UiTheme.MUTED + "KOTH, LMS, Most Wanted und Duels",
                UiTheme.MUTED + "Mastery " + UiTheme.TEXT + mastery.getTitle(targetId),
                "",
                UiItems.action("Leaderboards oeffnen")), (p,e,s) -> Bukkit.dispatchCommand(p, "top"));

        gui.setItem(33, UiItems.item(Material.BOOK_AND_QUILL,
                UiTheme.PRIMARY + "History",
                UiTheme.MUTED + "Season Legacy und Weapon History",
                UiTheme.MUTED + "Permanente Historie statt wertloser Resets.",
                "",
                UiItems.action("Legacy Hall oeffnen")), (p,e,s) -> Bukkit.dispatchCommand(p, "legacyhall"));

        gui.setItem(UiTheme.NAV_HOME, UiItems.item(Material.COMPASS,
                UiTheme.PRIMARY + "Profile",
                UiTheme.MUTED + "Overview  •  Combat  •  Progression",
                UiTheme.MUTED + "Collection  •  Rivals  •  Events  •  History"));

        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(viewer);
    }

    private OfflinePlayer resolve(Player viewer, String[] args) {
        if (args.length == 0) return viewer;
        if (args.length != 1) return null;
        Player online = Bukkit.getPlayerExact(args[0]);
        if (online != null) return online;
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline.getName() != null && offline.getName().equalsIgnoreCase(args[0])) return offline;
        }
        return null;
    }

    private String display(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() == null ? uuid.toString().substring(0, 8) : player.getName();
    }

    private String kd(PvpStatsSnapshot value) {
        return String.format(Locale.GERMANY, "%.2f", value.getKd());
    }
}
