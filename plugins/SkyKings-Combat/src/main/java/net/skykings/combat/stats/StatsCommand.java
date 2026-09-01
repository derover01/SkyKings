package net.skykings.combat.stats;

import net.skykings.combat.killstreak.KillstreakServiceImpl;
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
    public static final String RESET_PERMISSION = "skykings.perk.statsreset";

    private final PvpStatsService stats;

    public StatsCommand(PvpStatsService stats) { this.stats = stats; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar."); return true; }
        Player viewer = (Player) sender;

        if (label.equalsIgnoreCase("statsreset") || label.equalsIgnoreCase("resetstats")) {
            openReset(viewer);
            return true;
        }

        OfflinePlayer target = resolve(viewer, args);
        if (target == null) {
            viewer.sendMessage(UiTheme.DANGER + "Spieler nicht gefunden."); SoundFeedback.error(viewer); return true;
        }
        open(viewer, target); return true;
    }

    private void openReset(final Player player) {
        if (!player.hasPermission(RESET_PERMISSION)) {
            player.sendMessage(UiTheme.DANGER + "Du besitzt das Stats-Reset-Recht nicht.");
            player.sendMessage(UiTheme.MUTED + "Das Recht kann ueber einen Stats-Reset-Gutschein freigeschaltet werden.");
            SoundFeedback.error(player);
            return;
        }

        PvpStatsSnapshot current = stats.getStats(player.getUniqueId());
        GuiSession gui = GuiSession.create(player, UiTheme.title("Stats Reset"), 27);
        gui.setItem(4, UiItems.item(Material.BOOK, UiTheme.WARNING + "Combat-Stats zuruecksetzen",
                UiTheme.MUTED + "Kills " + UiTheme.TEXT + UiFormat.number(current.getKills()),
                UiTheme.MUTED + "Tode " + UiTheme.TEXT + UiFormat.number(current.getDeaths()),
                UiTheme.MUTED + "Beststreak " + UiTheme.TEXT + current.getBestStreak(), "",
                UiTheme.DANGER + "Diese Aktion kann nicht rueckgaengig gemacht werden."));
        gui.setItem(11, UiItems.item(Material.REDSTONE_BLOCK, UiTheme.DANGER + "ABBRECHEN",
                UiTheme.MUTED + "Deine Stats bleiben unveraendert.", "", UiItems.action("Klicken")), (p,e,s) -> {
            p.closeInventory();
            SoundFeedback.back(p);
        });
        gui.setItem(15, UiItems.item(Material.EMERALD_BLOCK, UiTheme.SUCCESS + "RESET BESTAETIGEN",
                UiTheme.MUTED + "Setzt Kills, Tode und Streaks auf 0.", "", UiItems.action("Klicken zum Bestaetigen")), (p,e,s) -> {
            if (!p.hasPermission(RESET_PERMISSION)) {
                p.closeInventory();
                p.sendMessage(UiTheme.DANGER + "Das Stats-Reset-Recht ist nicht mehr aktiv.");
                SoundFeedback.error(p);
                return;
            }
            stats.resetStats(p.getUniqueId());
            KillstreakServiceImpl.resetActive(p.getUniqueId());
            p.closeInventory();
            p.sendMessage(UiTheme.SUCCESS + "Deine PvP-Stats wurden vollstaendig zurueckgesetzt.");
            SoundFeedback.success(p);
        });
        GuiManager.active().open(gui);
        SoundFeedback.menuOpen(player);
    }

    private void open(Player viewer, OfflinePlayer target) {
        String name = target.getName() == null ? target.getUniqueId().toString().substring(0, 8) : target.getName();
        PvpStatsSnapshot value = stats.getStats(target.getUniqueId());
        boolean self = viewer.getUniqueId().equals(target.getUniqueId());

        GuiSession gui = GuiSession.create(viewer, UiTheme.title("Profile"), 54);
        gui.setItem(4, UiItems.head(name, UiTheme.TEXT + name,
                UiTheme.MUTED + "Combat Profile",
                UiTheme.TEXT + UiFormat.number(value.getKills()) + UiTheme.MUTED + " Kills  •  " + UiTheme.TEXT + kd(value) + UiTheme.MUTED + " K/D",
                UiTheme.MUTED + "Beststreak " + UiTheme.TEXT + value.getBestStreak()));

        gui.setItem(19, UiItems.item(Material.DIAMOND_SWORD, UiTheme.PRIMARY + "Combat",
                UiTheme.MUTED + "Kills " + UiTheme.TEXT + UiFormat.number(value.getKills()),
                UiTheme.MUTED + "Tode " + UiTheme.TEXT + UiFormat.number(value.getDeaths()),
                UiTheme.MUTED + "K/D " + UiTheme.TEXT + kd(value),
                UiTheme.MUTED + "Streak " + UiTheme.TEXT + value.getCurrentStreak(),
                UiTheme.MUTED + "Beststreak " + UiTheme.TEXT + value.getBestStreak()));

        gui.setItem(21, UiItems.item(Material.EXP_BOTTLE, UiTheme.PRIMARY + "Progression",
                UiTheme.MUTED + "Season-Level und Battle Pass.", "",
                self ? UiItems.action("Klicken zum Oeffnen") : UiTheme.DISABLED + "Nur im eigenen Profil"), (p,e,s) -> {
            if (self) Bukkit.dispatchCommand(p, "season");
        });

        gui.setItem(23, UiItems.item(Material.SKULL_ITEM, (short) 3, UiTheme.PRIMARY + "Collection",
                UiTheme.MUTED + "Besiegte Spieler als Head-Lookbook.", "",
                self ? UiItems.action("Klicken zum Oeffnen") : UiTheme.DISABLED + "Collection ist persoenlich"), (p,e,s) -> {
            if (self) Bukkit.dispatchCommand(p, "collection");
        });

        gui.setItem(25, UiItems.item(Material.BOOK, UiTheme.PRIMARY + "Achievements",
                UiTheme.MUTED + "PvP-, Map- und Progress-Erfolge.", "",
                self ? UiItems.action("Achievements oeffnen") : UiTheme.DISABLED + "Nur eigene Achievements"), (p,e,s) -> {
            if (self) Bukkit.dispatchCommand(p, "achievements");
        });

        gui.setItem(27, UiItems.item(Material.GOLD_INGOT, UiTheme.LEGENDARY + "Medaillen",
                UiTheme.MUTED + "Permanente Season-Auszeichnungen.", "",
                UiItems.action("Medaillen anzeigen")), (p,e,s) -> Bukkit.dispatchCommand(p, "medals " + name));

        gui.setItem(29, UiItems.item(Material.IRON_SWORD, UiTheme.PRIMARY + "Rivals",
                UiTheme.MUTED + "Revenge und persoenliche Rivalitaeten.",
                self ? UiTheme.MUTED + "Entsteht durch echte PvP-Kills." : UiTheme.MUTED + "Besiege Spieler fuer Rivalitaeten."));

        gui.setItem(31, UiItems.item(Material.NETHER_STAR, UiTheme.PRIMARY + "Events",
                UiTheme.MUTED + "KOTH, LMS, Most Wanted und Duels.", "",
                UiItems.action("Leaderboards oeffnen")), (p,e,s) -> Bukkit.dispatchCommand(p, "top"));

        gui.setItem(33, UiItems.item(Material.BOOK_AND_QUILL, UiTheme.PRIMARY + "History",
                UiTheme.MUTED + "Legacy Hall und Weapon History.", "",
                UiItems.action("Legacy Hall oeffnen")), (p,e,s) -> Bukkit.dispatchCommand(p, "legacyhall"));

        gui.setItem(UiTheme.NAV_BACK, UiItems.back(), (p,e,s) -> { SoundFeedback.back(p); Bukkit.dispatchCommand(p, "commands"); });
        gui.setItem(UiTheme.NAV_HOME, UiItems.item(Material.COMPASS, UiTheme.PRIMARY + "Profile",
                UiTheme.MUTED + "Combat  •  Progression  •  Collection",
                UiTheme.MUTED + "Achievements  •  Medaillen  •  History"));
        GuiManager.active().open(gui); SoundFeedback.menuOpen(viewer);
    }

    private OfflinePlayer resolve(Player viewer, String[] args) {
        if (args.length == 0) return viewer;
        if (args.length != 1) return null;
        Player online = Bukkit.getPlayerExact(args[0]); if (online != null) return online;
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) if (offline.getName() != null && offline.getName().equalsIgnoreCase(args[0])) return offline;
        return null;
    }

    private String kd(PvpStatsSnapshot value) { return String.format(Locale.GERMANY, "%.2f", value.getKd()); }
}
