package net.skykings.combat.retention;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /battlepass Hub + /premiumpass give|remove fuer Staff. */
public final class BattlePassCommand implements CommandExecutor {
    private final BattlePassService battlePass;

    public BattlePassCommand(BattlePassService battlePass) { this.battlePass = battlePass; }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("premiumpass".equalsIgnoreCase(label)) {
            if (!sender.hasPermission("skykings.admin.battlepass")) {
                sender.sendMessage(ChatColor.RED + "Keine Berechtigung.");
                return true;
            }
            if (args.length != 2 || !("give".equalsIgnoreCase(args[0]) || "remove".equalsIgnoreCase(args[0]))) {
                sender.sendMessage(ChatColor.YELLOW + "/premiumpass give <Spieler>");
                sender.sendMessage(ChatColor.YELLOW + "/premiumpass remove <Spieler>");
                return true;
            }

            Player online = Bukkit.getPlayerExact(args[1]);
            OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayer(args[1]);
            if (target == null || (online == null && !target.hasPlayedBefore())) {
                sender.sendMessage(ChatColor.RED + "Spieler nicht gefunden: " + args[1]);
                return true;
            }

            boolean enabled = "give".equalsIgnoreCase(args[0]);
            String name = target.getName() == null ? args[1] : target.getName();
            if (!battlePass.setPremium(target.getUniqueId(), enabled)) {
                sender.sendMessage(ChatColor.RED + "Premium Pass fuer " + name + " konnte nicht sicher gespeichert werden.");
                if (online != null) online.sendMessage(ChatColor.RED + "Premium-Pass-Aenderung konnte nicht gespeichert werden.");
                return true;
            }

            sender.sendMessage((enabled ? ChatColor.GREEN : ChatColor.YELLOW)
                    + "Premium Pass fuer " + ChatColor.WHITE + name
                    + (enabled ? ChatColor.GREEN + " vergeben." : ChatColor.YELLOW + " entfernt."));
            if (online != null) {
                online.sendMessage(enabled
                        ? ChatColor.GOLD.toString() + ChatColor.BOLD + "PREMIUM PASS FREIGESCHALTET"
                        : ChatColor.YELLOW.toString() + ChatColor.BOLD + "PREMIUM PASS ENTFERNT");
            }
            return true;
        }

        // Alter Admin-Flow bleibt kompatibel, falls alte Staff-Makros existieren.
        if (args.length >= 3 && "premium".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("skykings.admin.battlepass")) {
                sender.sendMessage(ChatColor.RED + "Keine Berechtigung.");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { sender.sendMessage(ChatColor.RED + "Spieler muss online sein."); return true; }
            boolean enabled = "on".equalsIgnoreCase(args[2]) || "true".equalsIgnoreCase(args[2]) || "1".equals(args[2]);
            if (!battlePass.setPremium(target.getUniqueId(), enabled)) {
                sender.sendMessage(ChatColor.RED + "Premium-Status konnte nicht sicher gespeichert werden.");
                return true;
            }
            sender.sendMessage(ChatColor.GREEN + "Battle Pass Premium fuer " + target.getName() + ": " + enabled);
            target.sendMessage(enabled ? ChatColor.GOLD + "Premium Battle Pass wurde aktiviert." : ChatColor.YELLOW + "Premium Battle Pass wurde deaktiviert.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Nutze /premiumpass give|remove <Spieler>.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length >= 1 && ("rewards".equalsIgnoreCase(args[0]) || "belohnungen".equalsIgnoreCase(args[0]))) {
            int page = 0;
            if (args.length >= 2) {
                try { page = Math.max(0, Integer.parseInt(args[1]) - 1); }
                catch (NumberFormatException ignored) { }
            }
            battlePass.openRewards(player, page);
            return true;
        }
        if (args.length >= 1 && ("quests".equalsIgnoreCase(args[0]) || "aufgaben".equalsIgnoreCase(args[0]))) {
            Bukkit.dispatchCommand(player, "quests");
            return true;
        }
        battlePass.open(player);
        return true;
    }
}
