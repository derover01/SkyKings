package net.skykings.core.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/** /commands - zentrale SkyKings-Befehlsübersicht. */
public final class CommandsCommand implements CommandExecutor {
    private final CommandsGui gui;

    public CommandsCommand(CommandsGui gui) {
        this.gui = gui;
        Plugin plugin = Bukkit.getPluginManager().getPlugin("SkyKings-Core");
        if (plugin instanceof JavaPlugin) {
            PluginCommand trash = ((JavaPlugin) plugin).getCommand("trash");
            if (trash != null) trash.setExecutor(new TrashCommand());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur für Spieler verfügbar.");
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
