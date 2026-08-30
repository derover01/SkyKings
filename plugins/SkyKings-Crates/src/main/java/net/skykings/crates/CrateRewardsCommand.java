package net.skykings.crates;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /craterewards - Paid-Rank-Crate-Rewards. */
public final class CrateRewardsCommand implements CommandExecutor {
    private final CrateRewardsGui gui;

    public CrateRewardsCommand(CrateRewardsGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler verfuegbar.");
            return true;
        }
        if (args.length != 0) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /craterewards");
            return true;
        }
        gui.open((Player) sender);
        return true;
    }
}
