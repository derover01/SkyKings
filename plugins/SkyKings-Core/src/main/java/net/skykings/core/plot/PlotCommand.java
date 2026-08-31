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

    public PlotCommand(PlotService plots) {
        this.plots = plots;
        this.menu = new PlotMenu(plots);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        Player p = (Player) sender;
        if (args.length == 0 || "menu".equalsIgnoreCase(args[0])) { menu.open(p); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);

        if ("create".equals(sub) || "auto".equals(sub) || "claim".equals(sub)) {
            if (plots.hasPlot(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "Du besitzt bereits einen Plot."); return true; }
            if (!plots.create(p)) { p.sendMessage(ChatColor.RED + "Plot konnte nicht erstellt werden."); return true; }
            p.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "PLOT GECLAIMT! " + ChatColor.GRAY + "Willkommen auf deinem SkyKings Plot.");
            p.playSound(p.getLocation(), Sound.LEVEL_UP, 0.8F, 1.3F); return true;
        }
        if ("home".equals(sub) || "h".equals(sub)) { plots.teleportHome(p, p.getUniqueId()); return true; }
        if ("sethome".equals(sub) || "seth".equals(sub)) {
            boolean ok = plots.setHome(p.getUniqueId(), p.getLocation());
            p.sendMessage(ok ? ChatColor.GREEN + "Plot-Home gesetzt." : ChatColor.RED + "Du musst auf deinem Plot stehen.");
            p.playSound(p.getLocation(), ok ? Sound.ORB_PICKUP : Sound.VILLAGER_NO, 0.7F, ok ? 1.4F : 1F); return true;
        }
        if ("info".equals(sub) || "i".equals(sub)) { menu.open(p); return true; }
        if (("trust".equals(sub) || "add".equals(sub) || "untrust".equals(sub) || "remove".equals(sub)) && args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { p.sendMessage(ChatColor.RED + "Spieler muss online sein."); return true; }
            boolean adding = "trust".equals(sub) || "add".equals(sub);
            boolean changed = adding ? plots.trust(p.getUniqueId(), target.getUniqueId()) : plots.untrust(p.getUniqueId(), target.getUniqueId());
            if (changed) {
                p.sendMessage(ChatColor.GREEN + target.getName() + (adding ? " darf jetzt auf deinem Plot bauen." : " wurde von deinem Plot entfernt."));
                p.playSound(p.getLocation(), adding ? Sound.ORB_PICKUP : Sound.CLICK, 0.7F, adding ? 1.4F : 0.8F);
            } else p.sendMessage(ChatColor.YELLOW + "Keine Aenderung.");
            return true;
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
        p.sendMessage(ChatColor.DARK_GRAY + "---------------- " + ChatColor.GREEN + ChatColor.BOLD + "SKYKINGS PLOTS" + ChatColor.DARK_GRAY + " ----------------");
        p.sendMessage(ChatColor.GREEN + "/plot" + ChatColor.GRAY + " - Plot-Menue");
        p.sendMessage(ChatColor.GREEN + "/plot auto" + ChatColor.GRAY + " - freien Plot claimen");
        p.sendMessage(ChatColor.GREEN + "/plot h" + ChatColor.GRAY + " - Plot-Home");
        p.sendMessage(ChatColor.GREEN + "/plot sethome" + ChatColor.GRAY + " - Home setzen");
        p.sendMessage(ChatColor.GREEN + "/plot trust <Spieler>" + ChatColor.GRAY + " - Baurechte geben");
        p.sendMessage(ChatColor.GREEN + "/plot visit <Spieler>" + ChatColor.GRAY + " - Plot besuchen");
    }
}
