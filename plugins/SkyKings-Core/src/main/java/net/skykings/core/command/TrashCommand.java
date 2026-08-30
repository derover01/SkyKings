package net.skykings.core.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/** /trash bzw. /müll öffnet ein temporäres Inventar. Alles darin wird beim Schließen verworfen. */
public final class TrashCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfügbar.");
            return true;
        }

        Player player = (Player) sender;
        Inventory trash = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "SkyKings | Müll");
        player.openInventory(trash);
        player.sendMessage(ChatColor.GRAY + "Alles, was du in dieses Inventar legst, wird beim Schließen "
                + ChatColor.RED + "dauerhaft gelöscht" + ChatColor.GRAY + ".");
        return true;
    }
}
