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

/** /plot create|home|sethome|trust|untrust|visit|info */
public final class PlotCommand implements CommandExecutor {
    private final PlotService plots;
    public PlotCommand(PlotService plots) { this.plots = plots; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        Player p = (Player) sender;
        if (args.length == 0) { if (plots.hasPlot(p.getUniqueId())) plots.teleportHome(p, p.getUniqueId()); else usage(p); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("create".equals(sub)) {
            if (plots.hasPlot(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "Du besitzt bereits einen Plot."); return true; }
            if (!plots.create(p)) { p.sendMessage(ChatColor.RED + "Plot konnte nicht erstellt werden."); return true; }
            p.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "PLOT ERSTELLT"); p.playSound(p.getLocation(), Sound.LEVEL_UP, 0.6F, 1.3F); return true;
        }
        if ("home".equals(sub)) { plots.teleportHome(p, p.getUniqueId()); return true; }
        if ("sethome".equals(sub)) { p.sendMessage(plots.setHome(p.getUniqueId(), p.getLocation()) ? ChatColor.GREEN + "Plot-Home gesetzt." : ChatColor.RED + "Du musst auf deinem Plot stehen."); return true; }
        if ("info".equals(sub)) {
            PlotService.PlotData d = plots.get(p.getUniqueId()); if (d == null) { p.sendMessage(ChatColor.RED + "Kein Plot. /plot create"); return true; }
            p.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "DEIN PLOT");
            p.sendMessage(ChatColor.GRAY + "Region: " + ChatColor.WHITE + (PlotService.RADIUS * 2 + 1) + "x" + (PlotService.RADIUS * 2 + 1));
            p.sendMessage(ChatColor.GRAY + "Trusted: " + ChatColor.WHITE + d.getTrusted().size()); return true;
        }
        if (("trust".equals(sub) || "untrust".equals(sub)) && args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]); if (target == null) { p.sendMessage(ChatColor.RED + "Spieler muss online sein."); return true; }
            boolean changed = "trust".equals(sub) ? plots.trust(p.getUniqueId(), target.getUniqueId()) : plots.untrust(p.getUniqueId(), target.getUniqueId());
            p.sendMessage(changed ? ChatColor.GREEN + "Plot-Rechte aktualisiert." : ChatColor.YELLOW + "Keine Aenderung."); return true;
        }
        if ("visit".equals(sub) && args.length >= 2) {
            Player online = Bukkit.getPlayer(args[1]); UUID owner;
            if (online != null) owner = online.getUniqueId(); else { @SuppressWarnings("deprecation") OfflinePlayer off = Bukkit.getOfflinePlayer(args[1]); owner = off.getUniqueId(); }
            if (!plots.hasPlot(owner)) { p.sendMessage(ChatColor.RED + "Dieser Spieler besitzt keinen Plot."); return true; }
            plots.teleportHome(p, owner); return true;
        }
        usage(p); return true;
    }

    private void usage(Player p) {
        p.sendMessage(ChatColor.GOLD + "SkyKings Plots");
        p.sendMessage(ChatColor.YELLOW + "/plot create | home | sethome | info");
        p.sendMessage(ChatColor.YELLOW + "/plot trust <Spieler> | untrust <Spieler>");
        p.sendMessage(ChatColor.YELLOW + "/plot visit <Spieler>");
    }
}
