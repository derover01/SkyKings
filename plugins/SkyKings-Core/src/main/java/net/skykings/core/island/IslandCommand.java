package net.skykings.core.island;

import net.skykings.core.gui.GuiManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** /is oeffnet das Hauptmenue; Unterbefehle bleiben fuer schnelle Bedienung verfuegbar. */
public final class IslandCommand implements CommandExecutor, TabCompleter {
    private final IslandService islands;
    private final IslandMenu menu;

    public IslandCommand(IslandService islands) {
        this(islands, new IslandMenu(GuiManager.active(), islands));
        Bukkit.getPluginManager().registerEvents(new IslandWelcomeSignListener(islands),
                JavaPlugin.getProvidingPlugin(IslandCommand.class));
    }

    public IslandCommand(IslandService islands, IslandMenu menu) { this.islands = islands; this.menu = menu; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar."); return true; }
        Player player = (Player) sender;
        if (args.length == 0 || "menu".equalsIgnoreCase(args[0])) { menu.open(player); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);

        if ("create".equals(sub) || "erstellen".equals(sub)) {
            if (islands.hasIsland(player.getUniqueId())) { player.sendMessage(ChatColor.RED + "Du besitzt bereits eine Insel."); return true; }
            if (!islands.create(player)) { player.sendMessage(ChatColor.RED + "Deine Insel konnte nicht erstellt werden."); return true; }
            player.sendMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "SKYKINGS ISLANDS " + ChatColor.GREEN + "Deine Insel wurde erstellt!");
            player.sendMessage(ChatColor.GRAY + "Baum, Void und Startertruhe sind im klassischen SkyBlock-Stil aufgebaut.");
            return true;
        }
        if ("delete".equals(sub) || "remove".equals(sub) || "loeschen".equals(sub) || "löschen".equals(sub)) {
            if (!islands.hasIsland(player.getUniqueId())) { player.sendMessage(ChatColor.RED + "Du besitzt keine Insel."); return true; }
            menu.openDeleteConfirm(player);
            return true;
        }
        if ("home".equals(sub)) { islands.teleportHome(player, player.getUniqueId()); return true; }
        if ("top".equals(sub) || "top10".equals(sub)) { menu.openTop(player); return true; }
        if ("level".equals(sub)) {
            IslandService.IslandData data = islands.get(player.getUniqueId());
            if (data == null) { player.sendMessage(ChatColor.RED + "Du besitzt keine Insel."); return true; }
            player.sendMessage(ChatColor.AQUA + "Island-Level: " + ChatColor.WHITE + data.getLevel()
                    + ChatColor.DARK_GRAY + " (" + data.getLevelPoints() + " Punkte)");
            return true;
        }
        if ("sethome".equals(sub)) {
            boolean ok = islands.setHome(player.getUniqueId(), player.getLocation());
            player.sendMessage(ok ? ChatColor.GREEN + "Island-Home gesetzt." : ChatColor.RED + "Du musst auf deiner Insel stehen.");
            player.playSound(player.getLocation(), ok ? Sound.ORB_PICKUP : Sound.VILLAGER_NO, 0.7F, ok ? 1.4F : 1.0F);
            return true;
        }
        if ("info".equals(sub)) { menu.open(player); return true; }
        if (("trust".equals(sub) || "untrust".equals(sub)) && args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { player.sendMessage(ChatColor.RED + "Spieler muss online sein."); return true; }
            if (!islands.hasIsland(player.getUniqueId())) { player.sendMessage(ChatColor.RED + "Du besitzt keine Insel."); return true; }
            boolean adding = "trust".equals(sub);
            boolean changed = adding ? islands.trust(player.getUniqueId(), target.getUniqueId()) : islands.untrust(player.getUniqueId(), target.getUniqueId());
            if (changed) {
                player.sendMessage(ChatColor.GREEN + target.getName() + (adding ? " darf jetzt auf deiner Insel bauen." : " wurde entfernt."));
                player.playSound(player.getLocation(), adding ? Sound.ORB_PICKUP : Sound.CLICK, 0.7F, adding ? 1.4F : 0.8F);
            } else player.sendMessage(ChatColor.YELLOW + "Keine Aenderung.");
            return true;
        }
        if ("visit".equals(sub) && args.length >= 2) {
            Player online = Bukkit.getPlayer(args[1]);
            UUID owner;
            if (online != null) owner = online.getUniqueId();
            else { @SuppressWarnings("deprecation") OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]); owner = offline.getUniqueId(); }
            islands.visit(player, owner); return true;
        }
        usage(player); return true;
    }

    private void usage(Player player) {
        player.sendMessage(ChatColor.AQUA + "/is" + ChatColor.GRAY + " - Island-Menue");
        player.sendMessage(ChatColor.AQUA + "/is home" + ChatColor.GRAY + " - eigenes Home");
        player.sendMessage(ChatColor.AQUA + "/is top" + ChatColor.GRAY + " - Island Top 10");
        player.sendMessage(ChatColor.AQUA + "/is level" + ChatColor.GRAY + " - dein Island-Level");
        player.sendMessage(ChatColor.AQUA + "/is trust <Spieler>" + ChatColor.GRAY + " - Baurechte geben");
        player.sendMessage(ChatColor.AQUA + "/is visit <Spieler>" + ChatColor.GRAY + " - [Welcome]-Insel besuchen");
        player.sendMessage(ChatColor.RED + "/is delete" + ChatColor.GRAY + " - eigene Insel unwiderruflich loeschen");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(Arrays.asList("menu", "create", "home", "sethome", "info", "level", "top", "trust", "untrust", "visit", "delete"), args[0]);
        if (args.length == 2 && ("trust".equalsIgnoreCase(args[0]) || "untrust".equalsIgnoreCase(args[0]) || "visit".equalsIgnoreCase(args[0]))) {
            List<String> names = new ArrayList<String>(); for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName()); return filter(names, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String raw) {
        String prefix = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<String>(); for (String value : values) if (value.toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(value); return out;
    }
}
