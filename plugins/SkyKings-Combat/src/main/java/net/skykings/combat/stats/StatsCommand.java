package net.skykings.combat.stats;

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

/** /stats, /profile und /profil teilen sich dieselbe moderne Spielerprofil-Oberflaeche. */
public final class StatsCommand implements CommandExecutor {
    private final PvpStatsTracker stats;

    public StatsCommand(PvpStatsTracker stats) {
        this.stats = stats;
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

    private void open(Player viewer, OfflinePlayer target) {
        String name = target.getName() == null ? target.getUniqueId().toString().substring(0, 8) : target.getName();
        PvpStatsSnapshot value = stats.getStats(target.getUniqueId());
        boolean self = viewer.getUniqueId().equals(target.getUniqueId());

        GuiSession gui = GuiSession.create(viewer, UiTheme.title("Profile"), 54);
        gui.setItem(4, UiItems.head(name,
                UiTheme.TEXT + name,
                UiTheme.MUTED + "Combat Profile",
                UiTheme.TEXT + UiFormat.number(value.getKills()) + UiTheme.MUTED + " Kills  •  "
                        + UiTheme.TEXT + kd(value) + UiTheme.MUTED + " K/D",
                UiTheme.MUTED + "Beststreak " + UiTheme.TEXT + value.getBestStreak()));

        gui.setItem(19, UiItems.item(Material.DIAMOND_SWORD,
                UiTheme.PRIMARY + "Combat",
                UiTheme.MUTED + "Kills " + UiTheme.TEXT + UiFormat.number(value.getKills()),
                UiTheme.MUTED + "Tode " + UiTheme.TEXT + UiFormat.number(value.getDeaths()),
                UiTheme.MUTED + "K/D " + UiTheme.TEXT + kd(value),
                UiTheme.MUTED + "Streak " + UiTheme.TEXT + value.getCurrentStreak(),
                UiTheme.MUTED + "Beststreak " + UiTheme.TEXT + value.getBestStreak()));

        gui.setItem(21, UiItems.item(Material.EXP_BOTTLE,
                UiTheme.PRIMARY + "Progression",
                UiTheme.MUTED + "Season-Level und Battle Pass.",
                "",
                self ? UiItems.action("Klicken zum Oeffnen") : UiTheme.DISABLED + "Nur im eigenen Profile"), (p,e,s) -> {
            if (self) Bukkit.dispatchCommand(p, "season");
        });

        gui.setItem(23, UiItems.item(Material.SKULL_ITEM, (short) 3,
                UiTheme.PRIMARY + "Collection",
                UiTheme.MUTED + "Welche Spieler hast du bereits besiegt?",
                UiTheme.MUTED + "Kopf-Lookbook und Kill-Historie.",
                "",
                self ? UiItems.action("Klicken zum Oeffnen") : UiTheme.DISABLED + "Collection ist persoenlich"), (p,e,s) -> {
            if (self) Bukkit.dispatchCommand(p, "collection");
        });

        gui.setItem(25, UiItems.item(Material.GOLD_INGOT,
                UiTheme.PRIMARY + "Achievements",
                UiTheme.MUTED + "Erfolge und permanente Medaillen.",
                "",
                UiItems.action("Medaillen anzeigen")), (p,e,s) -> Bukkit.dispatchCommand(p, "medals " + name));

        gui.setItem(29, UiItems.item(Material.IRON_SWORD,
                UiTheme.PRIMARY + "Rivals",
                UiTheme.MUTED + "Revenge und persoenliche Rivalitaeten.",
                self ? UiTheme.MUTED + "Revenge-Status entsteht durch echte PvP-Kills."
                        : UiTheme.MUTED + "Besiege Spieler, um Rivalitaeten aufzubauen."));

        gui.setItem(31, UiItems.item(Material.NETHER_STAR,
                UiTheme.PRIMARY + "Events",
                UiTheme.MUTED + "KOTH, LMS, Most Wanted und Duels.",
                "",
                UiItems.action("Leaderboards oeffnen")), (p,e,s) -> Bukkit.dispatchCommand(p, "top"));

        gui.setItem(33, UiItems.item(Material.BOOK_AND_QUILL,
                UiTheme.PRIMARY + "History",
                UiTheme.MUTED + "Legacy Hall und Weapon History.",
                UiTheme.MUTED + "Fortschritt behaelt langfristigen Sammlerwert.",
                "",
                UiItems.action("Legacy Hall oeffnen")), (p,e,s) -> Bukkit.dispatchCommand(p, "legacyhall"));

        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> {
            SoundFeedback.back(p);
            Bukkit.dispatchCommand(p, "commands");
        });
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

    private String kd(PvpStatsSnapshot value) {
        return String.format(Locale.GERMANY, "%.2f", value.getKd());
    }
}
