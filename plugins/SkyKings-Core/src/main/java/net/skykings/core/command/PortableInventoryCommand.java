package net.skykings.core.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

/** Portable Anvil-/Workbench-/Enchanting-GUIs fuer berechtigte Spieler. */
public final class PortableInventoryCommand implements CommandExecutor {

    public enum Type { ANVIL, WORKBENCH, ENCHANTING }

    private final Type type;
    private final String permission;

    public PortableInventoryCommand(Type type, String permission) {
        this.type = type;
        this.permission = permission;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler verfuegbar.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(permission)) {
            player.sendMessage(ChatColor.RED + "Dafuer hast du keine Berechtigung.");
            return true;
        }

        switch (type) {
            case ANVIL:
                player.openInventory(Bukkit.createInventory(player, InventoryType.ANVIL, ChatColor.DARK_GRAY + "Amboss"));
                break;
            case WORKBENCH:
                player.openInventory(Bukkit.createInventory(player, InventoryType.WORKBENCH, ChatColor.DARK_GRAY + "Werkbank"));
                break;
            case ENCHANTING:
                player.openInventory(Bukkit.createInventory(player, InventoryType.ENCHANTING, ChatColor.DARK_GRAY + "Verzauberung"));
                break;
            default:
                return false;
        }
        return true;
    }
}
