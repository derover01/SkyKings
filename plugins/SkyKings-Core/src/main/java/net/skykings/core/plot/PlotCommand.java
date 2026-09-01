package net.skykings.core.plot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

/** PlotSquared-inspirierte /plot Bedienung auf dem eigenen SkyKings-Claim-System. */
public final class PlotCommand implements CommandExecutor {
    private final PlotService plots;
    private final PlotMenu menu;

    public PlotCommand(PlotService plots) { this.plots = plots; this.menu = new PlotMenu(plots); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        Player p = (Player) sender;
        if (args.length == 0 || "menu".equalsIgnoreCase(args[0])) { menu.open(p); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);

        if ("create".equals(sub) || "auto".equals(sub) || "claim".equals(sub) || "a".equals(sub) || "c".equals(sub)) {
            if (plots.hasPlot(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "Du besitzt bereits einen Plot."); return true; }
            if (!plots.create(p)) { p.sendMessage(ChatColor.RED + "Plot konnte nicht erstellt werden."); return true; }
            p.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "PLOT GECLAIMT! " + ChatColor.GRAY + "65x65 Baugrundstueck mit Strassenanschluss.");
            return true;
        }
        if ("home".equals(sub) || "h".equals(sub)) { plots.teleportHome(p, p.getUniqueId()); return true; }
        if ("sethome".equals(sub) || "seth".equals(sub)) {
            boolean ok = plots.setHome(p.getUniqueId(), p.getLocation());
            p.sendMessage(ok ? ChatColor.GREEN + "Plot-Home gesetzt." : ChatColor.RED + "Du musst auf deinem Plot stehen.");
            p.playSound(p.getLocation(), ok ? Sound.ORB_PICKUP : Sound.VILLAGER_NO, 0.7F, ok ? 1.4F : 1F); return true;
        }
        if ("info".equals(sub) || "i".equals(sub)) { menu.open(p); return true; }

        if (("add".equals(sub) || "trust".equals(sub) || "remove".equals(sub) || "untrust".equals(sub)
                || "deny".equals(sub) || "undeny".equals(sub)) && args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { p.sendMessage(ChatColor.RED + "Spieler muss online sein."); return true; }
            boolean changed;
            if ("add".equals(sub)) changed = plots.add(p.getUniqueId(), target.getUniqueId());
            else if ("trust".equals(sub)) changed = plots.trust(p.getUniqueId(), target.getUniqueId());
            else if ("deny".equals(sub)) changed = plots.deny(p.getUniqueId(), target.getUniqueId());
            else if ("undeny".equals(sub)) changed = plots.undeny(p.getUniqueId(), target.getUniqueId());
            else changed = plots.remove(p.getUniqueId(), target.getUniqueId());
            p.sendMessage(changed ? ChatColor.GREEN + "Plot-Mitgliedschaft aktualisiert: " + target.getName() : ChatColor.YELLOW + "Keine Aenderung.");
            return true;
        }

        if ("flag".equals(sub) && args.length >= 3) {
            boolean value = "on".equalsIgnoreCase(args[2]) || "true".equalsIgnoreCase(args[2]) || "an".equalsIgnoreCase(args[2]);
            if (!value && !("off".equalsIgnoreCase(args[2]) || "false".equalsIgnoreCase(args[2]) || "aus".equalsIgnoreCase(args[2]))) {
                p.sendMessage(ChatColor.RED + "Nutze an/aus bzw. on/off."); return true;
            }
            if (!plots.setFlag(p.getUniqueId(), args[1], value)) {
                p.sendMessage(ChatColor.RED + "Unbekannte Flag. Verfuegbar: pvp, explosions, mob-spawn"); return true;
            }
            p.sendMessage(ChatColor.GREEN + "Plot-Flag " + args[1] + ": " + (value ? "AN" : "AUS")); return true;
        }

        if (("visit".equals(sub) || "v".equals(sub)) && args.length >= 2) {
            Player online = Bukkit.getPlayer(args[1]); UUID owner;
            if (online != null) owner = online.getUniqueId();
            else { @SuppressWarnings("deprecation") OfflinePlayer off = Bukkit.getOfflinePlayer(args[1]); owner = off.getUniqueId(); }
            if (!plots.hasPlot(owner)) { p.sendMessage(ChatColor.RED + "Dieser Spieler besitzt keinen Plot."); return true; }
            plots.teleportHome(p, owner); return true;
        }
        usage(p); return true;
    }

    private void usage(Player p) {
        p.sendMessage(ChatColor.GREEN + "/p auto" + ChatColor.GRAY + " - freien Plot claimen");
        p.sendMessage(ChatColor.GREEN + "/p h" + ChatColor.GRAY + " - Plot-Home");
        p.sendMessage(ChatColor.GREEN + "/p add <Spieler>" + ChatColor.GRAY + " - baut wenn Owner online ist");
        p.sendMessage(ChatColor.GREEN + "/p trust <Spieler>" + ChatColor.GRAY + " - dauerhaft Baurechte");
        p.sendMessage(ChatColor.GREEN + "/p remove <Spieler>" + ChatColor.GRAY + " - Mitglied entfernen");
        p.sendMessage(ChatColor.GREEN + "/p deny <Spieler>" + ChatColor.GRAY + " - Plot-Zutritt sperren");
        p.sendMessage(ChatColor.GREEN + "/p flag <pvp|explosions|mob-spawn> <an|aus>");
        p.sendMessage(ChatColor.GREEN + "/p visit <Spieler>" + ChatColor.GRAY + " - Plot besuchen");
    }
}
