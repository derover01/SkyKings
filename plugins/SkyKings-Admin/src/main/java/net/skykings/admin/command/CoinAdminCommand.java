package net.skykings.admin.command;

import net.skykings.core.economy.EconomyOverflowException;
import net.skykings.core.economy.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Sichere Staff-Kommandos fuer manuelle Coin-Korrekturen. */
public final class CoinAdminCommand implements CommandExecutor, TabCompleter {
    public enum Mode { ADD, SET }

    private static final String PERMISSION = "skykings.admin.coins";
    private final EconomyService economy;
    private final Mode mode;

    public CoinAdminCommand(EconomyService economy, Mode mode) {
        this.economy = economy;
        this.mode = mode;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "Dafuer hast du keine Berechtigung.");
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Nutze /" + label + " <Spieler> <Anzahl>.");
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Die Anzahl muss eine ganze Zahl sein.");
            return true;
        }
        if ((mode == Mode.ADD && amount <= 0L) || (mode == Mode.SET && amount < 0L)) {
            sender.sendMessage(ChatColor.RED + (mode == Mode.ADD
                    ? "Die Anzahl muss groesser als 0 sein."
                    : "Der Kontostand darf nicht negativ sein."));
            return true;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.isOnline() && !target.hasPlayedBefore()) {
            sender.sendMessage(ChatColor.RED + "Dieser Spieler hat noch nie auf SkyKings gespielt.");
            return true;
        }

        String actor = sender instanceof Player ? sender.getName() : "CONSOLE";
        String reason = mode == Mode.ADD ? "Admin /addcoins" : "Admin /setcoins";
        try {
            if (mode == Mode.ADD) economy.deposit(target.getUniqueId(), amount, actor, reason);
            else economy.setBalance(target.getUniqueId(), amount, actor, reason);
        } catch (EconomyOverflowException ex) {
            sender.sendMessage(ChatColor.RED + "Die Coin-Aenderung wuerde den maximalen Kontostand ueberschreiten.");
            return true;
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ChatColor.RED + "Ungueltiger Coin-Wert.");
            return true;
        } catch (IllegalStateException ex) {
            sender.sendMessage(ChatColor.RED + "Das PlayerProfile konnte nicht sicher geladen werden.");
            return true;
        }

        String targetName = target.getName() == null ? args[0] : target.getName();
        if (mode == Mode.ADD) {
            sender.sendMessage(ChatColor.GREEN + targetName + " wurden " + format(amount) + " Coins hinzugefuegt.");
            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(ChatColor.GOLD + "+" + format(amount) + " Coins " + ChatColor.GRAY + "(Admin)");
            }
        } else {
            sender.sendMessage(ChatColor.GREEN + "Coins von " + targetName + " wurden auf " + format(amount) + " gesetzt.");
            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(ChatColor.GOLD + "Dein Coin-Kontostand wurde auf " + format(amount) + " gesetzt. " + ChatColor.GRAY + "(Admin)");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION) || args.length != 1) return Collections.emptyList();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) names.add(player.getName());
        }
        return names;
    }

    private static String format(long value) {
        return String.format("%,d", value).replace(',', '.');
    }
}
