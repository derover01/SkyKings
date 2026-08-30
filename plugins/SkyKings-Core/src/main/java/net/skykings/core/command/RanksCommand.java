package net.skykings.core.command;

import net.skykings.core.rank.RanksGui;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Oeffnet die zentrale Rang-Uebersicht. */
public final class RanksCommand implements CommandExecutor {

    private final RanksGui ranksGui;

    public RanksCommand(RanksGui ranksGui) {
        this.ranksGui = ranksGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Dieser Befehl kann nur ingame verwendet werden.");
            return true;
        }
        ranksGui.open((Player) sender);
        return true;
    }
}
