package net.skykings.admin.command;

import net.skykings.core.model.Rank;
import net.skykings.core.rank.RankService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** /rang <Spieler> <Rang> setzt den internen SkyKings-Gameplay-Rang. */
public final class RankAdminCommand implements CommandExecutor, TabCompleter {

    public static final String ADMIN_PERMISSION = "skykings.admin.rang";
    private final RankService rankService;

    public RankAdminCommand(RankService rankService) { this.rankService = rankService; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "Dafür hast du keine Berechtigung.");
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /rang <Spieler> <Rang>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Der Spieler muss aktuell online sein.");
            return true;
        }
        Rank rank;
        try { rank = Rank.valueOf(args[1].toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + "Unbekannter Rang. Gültig: Spieler, Iron, Gold, Epic, Diamond, Knight, Phoenix, Eternal, Exile, Endling, King.");
            return true;
        }
        try { rankService.setRank(target.getUniqueId(), rank, sender.getName()); }
        catch (RuntimeException ex) {
            sender.sendMessage(ChatColor.RED + "Der Rang konnte nicht gesetzt werden.");
            return true;
        }
        sender.sendMessage(ChatColor.GREEN + target.getName() + " hat jetzt den Rang " + ChatColor.GOLD + pretty(rank) + ChatColor.GREEN + ".");
        target.sendMessage(ChatColor.GOLD + "Dein Rang wurde auf " + ChatColor.YELLOW + pretty(rank) + ChatColor.GOLD + " gesetzt.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) return Collections.emptyList();
        if (args.length == 1) {
            List<String> names = new ArrayList<String>();
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (Player player : Bukkit.getOnlinePlayers()) if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) names.add(player.getName());
            return names;
        }
        if (args.length == 2) {
            List<String> ranks = new ArrayList<String>();
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (Rank rank : Rank.values()) {
                String name = pretty(rank);
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) ranks.add(name);
            }
            return ranks;
        }
        return Collections.emptyList();
    }

    private String pretty(Rank rank) {
        String raw = rank.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
