package net.skykings.core.island;

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
import java.util.UUID;

/** /is create|home|sethome|info|trust|untrust|visit */
public final class IslandCommand implements CommandExecutor, TabCompleter {
    private final IslandService islands;

    public IslandCommand(IslandService islands) { this.islands = islands; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            if (islands.hasIsland(player.getUniqueId())) islands.teleportHome(player, player.getUniqueId());
            else usage(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("create".equals(sub) || "erstellen".equals(sub)) {
            if (islands.hasIsland(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Du besitzt bereits eine Insel.");
                return true;
            }
            if (!islands.create(player)) {
                player.sendMessage(ChatColor.RED + "Deine Insel konnte nicht erstellt werden.");
                return true;
            }
            player.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "INSEL ERSTELLT!" + ChatColor.GRAY + " Nutze /is fuer dein Home.");
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.7F, 1.3F);
            return true;
        }
        if ("home".equals(sub)) {
            islands.teleportHome(player, player.getUniqueId());
            return true;
        }
        if ("sethome".equals(sub)) {
            if (islands.setHome(player.getUniqueId(), player.getLocation())) {
                player.sendMessage(ChatColor.GREEN + "Island-Home gesetzt.");
            } else player.sendMessage(ChatColor.RED + "Du musst dich auf deiner eigenen Insel befinden.");
            return true;
        }
        if ("info".equals(sub)) {
            IslandService.IslandData data = islands.get(player.getUniqueId());
            if (data == null) {
                player.sendMessage(ChatColor.RED + "Du besitzt noch keine Insel. /is create");
                return true;
            }
            player.sendMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "DEINE INSEL");
            player.sendMessage(ChatColor.GRAY + "Region: " + ChatColor.WHITE + (IslandService.RADIUS * 2 + 1) + "x" + (IslandService.RADIUS * 2 + 1));
            player.sendMessage(ChatColor.GRAY + "Trusted: " + ChatColor.WHITE + data.getTrusted().size());
            player.sendMessage(ChatColor.GRAY + "Center: " + ChatColor.WHITE + data.centerX + ", " + data.centerZ);
            return true;
        }
        if (("trust".equals(sub) || "untrust".equals(sub)) && args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Spieler muss online sein.");
                return true;
            }
            if (!islands.hasIsland(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Du besitzt keine Insel.");
                return true;
            }
            boolean changed = "trust".equals(sub)
                    ? islands.trust(player.getUniqueId(), target.getUniqueId())
                    : islands.untrust(player.getUniqueId(), target.getUniqueId());
            player.sendMessage(changed ? ChatColor.GREEN + target.getName() + ("trust".equals(sub) ? " wurde vertraut." : " wurde entfernt.")
                    : ChatColor.YELLOW + "Keine Aenderung.");
            return true;
        }
        if ("visit".equals(sub) && args.length >= 2) {
            Player online = Bukkit.getPlayer(args[1]);
            UUID owner;
            if (online != null) owner = online.getUniqueId();
            else {
                @SuppressWarnings("deprecation") OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
                owner = offline.getUniqueId();
            }
            if (!islands.hasIsland(owner)) {
                player.sendMessage(ChatColor.RED + "Dieser Spieler besitzt keine Insel.");
                return true;
            }
            islands.teleportHome(player, owner);
            return true;
        }
        usage(player);
        return true;
    }

    private void usage(Player player) {
        player.sendMessage(ChatColor.GOLD + "SkyKings Islands");
        player.sendMessage(ChatColor.YELLOW + "/is create" + ChatColor.GRAY + " - Insel erstellen");
        player.sendMessage(ChatColor.YELLOW + "/is home" + ChatColor.GRAY + " - eigenes Home");
        player.sendMessage(ChatColor.YELLOW + "/is sethome");
        player.sendMessage(ChatColor.YELLOW + "/is trust <Spieler>");
        player.sendMessage(ChatColor.YELLOW + "/is untrust <Spieler>");
        player.sendMessage(ChatColor.YELLOW + "/is visit <Spieler>");
        player.sendMessage(ChatColor.YELLOW + "/is info");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(Arrays.asList("create", "home", "sethome", "info", "trust", "untrust", "visit"), args[0]);
        if (args.length == 2 && ("trust".equalsIgnoreCase(args[0]) || "untrust".equalsIgnoreCase(args[0]) || "visit".equalsIgnoreCase(args[0]))) {
            List<String> names = new ArrayList<String>();
            for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
            return filter(names, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String raw) {
        String prefix = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<String>();
        for (String value : values) if (value.toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(value);
        return out;
    }
}
