package net.skykings.core.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /commands - zentrale SkyKings-Befehlsuebersicht. */
public final class CommandsCommand implements CommandExecutor {
    private final CommandsGui gui;

    public CommandsCommand(CommandsGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler verfuegbar.");
            return true;
        }
        if (args.length != 0) {
            sender.sendMessage(ChatColor.RED + "Verwendung: /commands");
            return true;
        }
        gui.open((Player) sender);
        return true;
    }
}
