package net.skykings.combat.retention;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /battlepass sowie Admin-Premiumtoggle. */
public final class BattlePassCommand implements CommandExecutor {
    private final BattlePassService battlePass;

    public BattlePassCommand(BattlePassService battlePass) { this.battlePass = battlePass; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 3 && "premium".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("skykings.admin.battlepass")) {
                sender.sendMessage(ChatColor.RED + "Keine Berechtigung.");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { sender.sendMessage(ChatColor.RED + "Spieler muss online sein."); return true; }
            boolean enabled = "on".equalsIgnoreCase(args[2]) || "true".equalsIgnoreCase(args[2]) || "1".equals(args[2]);
            battlePass.setPremium(target.getUniqueId(), enabled);
            sender.sendMessage(ChatColor.GREEN + "Battle Pass Premium fuer " + target.getName() + ": " + enabled);
            target.sendMessage(enabled ? ChatColor.GOLD + "Premium Battle Pass wurde aktiviert." : ChatColor.YELLOW + "Premium Battle Pass wurde deaktiviert.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Nutze /battlepass premium <Spieler> <on|off>.");
            return true;
        }
        battlePass.open((Player) sender);
        return true;
    }
}
