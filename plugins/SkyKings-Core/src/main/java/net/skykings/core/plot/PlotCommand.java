package net.skykings.core.plot;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** PlotSquared-inspirierte /plot Bedienung auf dem eigenen SkyKings-Claim-System. */
public final class PlotCommand implements CommandExecutor, TabCompleter {
    private final PlotService plots;
    private final PlotMenu menu;

    public PlotCommand(PlotService plots) {
        this(plots, plots.getBorderService());
    }

    public PlotCommand(PlotService plots, PlotBorderService borders) {
        this.plots = plots;
        this.menu = new PlotMenu(plots, borders);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur ingame."); return true; }
        Player p = (Player) sender;
        if (args.length == 0 || "menu".equalsIgnoreCase(args[0])) { menu.open(p); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);

        if ("create".equals(sub) || "auto".equals(sub) || "claim".equals(sub) || "a".equals(sub) || "c".equals(sub)) {
            if (plots.hasPlot(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "Du besitzt bereits einen Plot. Fuer mehr Flaeche nutze /p merge."); return true; }
            if (!plots.create(p)) { p.sendMessage(ChatColor.RED + "Plot konnte nicht erstellt werden."); return true; }
            p.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "PLOT GECLAIMT! " + ChatColor.GRAY + "Die 65x65 Grasflaeche gehoert dir. Stone-Brick-Wege bleiben neutral.");
            return true;
        }
        if ("home".equals(sub) || "h".equals(sub)) { plots.teleportHome(p, p.getUniqueId()); return true; }
        if ("sethome".equals(sub) || "seth".equals(sub)) {
            boolean ok = plots.setHome(p.getUniqueId(), p.getLocation());
            p.sendMessage(ok ? ChatColor.GREEN + "Plot-Home gesetzt." : ChatColor.RED + "Du musst auf deiner Plotflaeche stehen.");
            p.playSound(p.getLocation(), ok ? Sound.ORB_PICKUP : Sound.VILLAGER_NO, 0.7F, ok ? 1.4F : 1F); return true;
        }
        if ("info".equals(sub) || "i".equals(sub)) { menu.open(p); return true; }
        if ("flags".equals(sub)) { menu.openFlags(p); return true; }
        if ("rand".equals(sub) || "border".equals(sub)) { menu.openBorders(p); return true; }

        if ("merge".equals(sub) || "verbinden".equals(sub)) {
            if (!plots.hasPlot(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "Du besitzt keinen Plot."); return true; }
            if (args.length < 2) {
                p.sendMessage(ChatColor.AQUA + "Plot Merge " + ChatColor.GRAY + "- /p merge <nord|ost|sued|west>");
                p.sendMessage(ChatColor.DARK_GRAY + "Stell dich auf die Plotflaeche, von der aus du erweitern willst.");
                return true;
            }
            PlotService.MergeDirection direction = PlotService.MergeDirection.parse(args[1]);
            PlotService.MergeResult result = plots.merge(p.getUniqueId(), p.getLocation(), direction);
            if (result == PlotService.MergeResult.SUCCESS) {
                p.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "PLOTS VERBUNDEN! " + ChatColor.GRAY + "Die Stone-Brick-Strasse dazwischen wurde zur Baufläche.");
                p.playSound(p.getLocation(), Sound.ANVIL_USE, 0.65F, 1.35F);
            } else if (result == PlotService.MergeResult.CLAIMED_BY_OTHER) {
                p.sendMessage(ChatColor.RED + "Die angrenzende Plotflaeche ist bereits vergeben.");
            } else if (result == PlotService.MergeResult.ALREADY_MERGED) {
                p.sendMessage(ChatColor.YELLOW + "Diese Plotflaeche ist bereits mit deinem Plot verbunden.");
            } else if (result == PlotService.MergeResult.NOT_ON_OWN_PLOT) {
                p.sendMessage(ChatColor.RED + "Stell dich auf eine deiner Grasflaechen und versuche es erneut.");
            } else if (result == PlotService.MergeResult.WORLD_EDGE) {
                p.sendMessage(ChatColor.RED + "In diese Richtung kann nicht erweitert werden.");
            } else if (result == PlotService.MergeResult.INVALID_DIRECTION) {
                p.sendMessage(ChatColor.RED + "Richtung: nord, ost, sued oder west.");
            } else {
                p.sendMessage(ChatColor.RED + "Plot konnte nicht verbunden werden.");
            }
            return true;
        }

        if ("add".equals(sub) || "trust".equals(sub) || "remove".equals(sub) || "untrust".equals(sub)
                || "deny".equals(sub) || "undeny".equals(sub)) {
            if (!plots.hasPlot(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "Du besitzt keinen Plot."); return true; }
            if (args.length < 2) {
                p.sendMessage(ChatColor.AQUA + "Plot Verwaltung");
                p.sendMessage(ChatColor.GRAY + "/p " + sub + " <Spieler>");
                return true;
            }
            UUID target = resolveTarget(args[1]);
            if (p.getUniqueId().equals(target)) { p.sendMessage(ChatColor.RED + "Du kannst dich nicht selbst verwalten."); return true; }
            boolean changed;
            String action;
            if ("add".equals(sub)) { changed = plots.add(p.getUniqueId(), target); action = "Added"; }
            else if ("trust".equals(sub)) { changed = plots.trust(p.getUniqueId(), target); action = "Trusted"; }
            else if ("deny".equals(sub)) { changed = plots.deny(p.getUniqueId(), target); action = "Denied"; }
            else if ("undeny".equals(sub)) { changed = plots.undeny(p.getUniqueId(), target); action = "Deny entfernt"; }
            else { changed = plots.remove(p.getUniqueId(), target); action = "Baurechte entfernt"; }
            p.sendMessage(changed
                    ? ChatColor.GREEN + args[1] + ChatColor.GRAY + " - " + ChatColor.WHITE + action
                    : ChatColor.YELLOW + "Keine Aenderung fuer " + args[1] + ".");
            p.playSound(p.getLocation(), changed ? Sound.CLICK : Sound.VILLAGER_NO, 0.6F, changed ? 1.3F : 1.0F);
            return true;
        }

        if ("flag".equals(sub)) {
            if (!plots.hasPlot(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "Du besitzt keinen Plot."); return true; }
            if (args.length < 3) {
                p.sendMessage(ChatColor.AQUA + "Plot Flags");
                p.sendMessage(ChatColor.GRAY + "/p flag <pvp|explosions|fire|mob-spawn> <an|aus>");
                return true;
            }
            boolean value = "on".equalsIgnoreCase(args[2]) || "true".equalsIgnoreCase(args[2]) || "an".equalsIgnoreCase(args[2]);
            if (!value && !("off".equalsIgnoreCase(args[2]) || "false".equalsIgnoreCase(args[2]) || "aus".equalsIgnoreCase(args[2]))) {
                p.sendMessage(ChatColor.RED + "Nutze an/aus bzw. on/off."); return true;
            }
            if (!plots.setFlag(p.getUniqueId(), args[1], value)) {
                p.sendMessage(ChatColor.RED + "Unbekannte Flag. Verfuegbar: pvp, explosions, fire, mob-spawn"); return true;
            }
            p.sendMessage(ChatColor.GREEN + "Plot-Flag " + args[1] + ": " + (value ? "AN" : "AUS"));
            p.playSound(p.getLocation(), Sound.CLICK, 0.6F, value ? 1.4F : 0.9F);
            return true;
        }

        if (("visit".equals(sub) || "v".equals(sub)) && args.length >= 2) {
            UUID owner = resolveTarget(args[1]);
            if (!plots.hasPlot(owner)) { p.sendMessage(ChatColor.RED + "Dieser Spieler besitzt keinen Plot."); return true; }
            plots.teleportHome(p, owner); return true;
        }
        usage(p); return true;
    }

    @SuppressWarnings("deprecation")
    private UUID resolveTarget(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.getUniqueId();
    }

    private void usage(Player p) {
        p.sendMessage(ChatColor.AQUA + "Plot Verwaltung");
        p.sendMessage(ChatColor.GREEN + "/p auto" + ChatColor.GRAY + " - freien Plot claimen");
        p.sendMessage(ChatColor.GREEN + "/p h" + ChatColor.GRAY + " - Plot-Home");
        p.sendMessage(ChatColor.GREEN + "/p merge <Richtung>" + ChatColor.GRAY + " - angrenzenden Plot verbinden");
        p.sendMessage(ChatColor.GREEN + "/p add <Spieler>" + ChatColor.GRAY + " - baut wenn Owner online ist");
        p.sendMessage(ChatColor.GREEN + "/p trust <Spieler>" + ChatColor.GRAY + " - dauerhaft Baurechte");
        p.sendMessage(ChatColor.GREEN + "/p remove <Spieler>" + ChatColor.GRAY + " - Add/Trust entfernen");
        p.sendMessage(ChatColor.GREEN + "/p deny <Spieler>" + ChatColor.GRAY + " - Plot-Zutritt sperren");
        p.sendMessage(ChatColor.GREEN + "/p undeny <Spieler>" + ChatColor.GRAY + " - Sperre entfernen");
        p.sendMessage(ChatColor.GREEN + "/p flags" + ChatColor.GRAY + " - Flag-Menue");
        p.sendMessage(ChatColor.GREEN + "/p rand" + ChatColor.GRAY + " - Plot-Rand kaufen/waehlen");
        p.sendMessage(ChatColor.GREEN + "/p visit <Spieler>" + ChatColor.GRAY + " - Plot besuchen");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("menu", "auto", "home", "sethome", "info", "merge", "add", "trust", "remove", "deny", "undeny", "flags", "flag", "rand", "visit"), args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("flag".equals(sub)) return filter(Arrays.asList("pvp", "explosions", "fire", "mob-spawn"), args[1]);
            if ("merge".equals(sub) || "verbinden".equals(sub)) return filter(Arrays.asList("nord", "ost", "sued", "west"), args[1]);
            if (sender instanceof Player) {
                Player player = (Player) sender;
                PlotService.PlotData data = plots.get(player.getUniqueId());
                if (("remove".equals(sub) || "untrust".equals(sub)) && data != null) {
                    List<String> values = new ArrayList<String>();
                    addNames(values, data.getMembers()); addNames(values, data.getTrusted());
                    return filter(values, args[1]);
                }
                if ("undeny".equals(sub) && data != null) {
                    List<String> values = new ArrayList<String>(); addNames(values, data.getDenied()); return filter(values, args[1]);
                }
            }
            if ("add".equals(sub) || "trust".equals(sub) || "deny".equals(sub) || "visit".equals(sub) || "v".equals(sub)) {
                List<String> names = new ArrayList<String>();
                for (Player online : Bukkit.getOnlinePlayers()) if (!(sender instanceof Player) || !online.getUniqueId().equals(((Player) sender).getUniqueId())) names.add(online.getName());
                return filter(names, args[1]);
            }
        }
        if (args.length == 3 && "flag".equalsIgnoreCase(args[0])) return filter(Arrays.asList("an", "aus"), args[2]);
        return Collections.emptyList();
    }

    private void addNames(List<String> out, Set<UUID> values) {
        for (UUID uuid : values) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            if (offline.getName() != null) out.add(offline.getName());
        }
    }

    private List<String> filter(List<String> values, String raw) {
        String prefix = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<String>();
        for (String value : values) if (value != null && value.toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(value);
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }
}
