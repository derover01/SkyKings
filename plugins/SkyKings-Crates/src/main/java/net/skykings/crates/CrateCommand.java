package net.skykings.crates;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Admin-Ausgabecommand fuer echte stackbare Crate-Batches. */
public final class CrateCommand implements CommandExecutor, TabCompleter {

    private final CrateRegistry registry;
    private final CrateItemCodec codec;

    public CrateCommand(CrateRegistry registry, CrateItemCodec codec) {
        this.registry = registry;
        this.codec = codec;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("skykings.admin.crate")) {
            sender.sendMessage(ChatColor.RED + "Dafür hast du keine Rechte.");
            return true;
        }
        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /crate give <Spieler> <Typ> [Anzahl]");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Spieler ist nicht online.");
            return true;
        }
        CrateRegistry.CrateDefinition crate = registry.get(args[2]);
        if (crate == null) {
            sender.sendMessage(ChatColor.RED + "Unbekannte Crate. Typen: " + crateIds());
            return true;
        }
        int amount = 1;
        if (args.length >= 4) {
            try { amount = Integer.parseInt(args[3]); }
            catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Anzahl muss eine Zahl sein.");
                return true;
            }
        }
        if (amount < 1 || amount > 64) {
            sender.sendMessage(ChatColor.RED + "Anzahl muss zwischen 1 und 64 liegen.");
            return true;
        }

        ItemStack stack = codec.create(crate, amount);
        if (!target.getInventory().addItem(stack).isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Der Spieler braucht einen freien Inventarplatz.");
            return true;
        }
        sender.sendMessage(ChatColor.GREEN + target.getName() + " hat " + amount + "x "
                + ChatColor.translateAlternateColorCodes('&', crate.getDisplayName()) + ChatColor.GREEN + " erhalten.");
        return true;
    }

    private String crateIds() {
        List<String> ids = new ArrayList<String>();
        for (CrateRegistry.CrateDefinition crate : registry.getAll()) ids.add(crate.getId());
        return String.join(", ", ids);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return prefix(Collections.singletonList("give"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<String>();
            for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
            return prefix(names, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> ids = new ArrayList<String>();
            for (CrateRegistry.CrateDefinition crate : registry.getAll()) ids.add(crate.getId());
            return prefix(ids, args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> prefix(List<String> values, String raw) {
        String needle = raw.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<String>();
        for (String value : values) if (value.toLowerCase(Locale.ROOT).startsWith(needle)) result.add(value);
        return result;
    }
}
