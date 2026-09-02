package net.skykings.core.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** /trash oeffnet den Muell; /clearinv leert das komplette eigene Inventar. */
public final class TrashCommand implements CommandExecutor {

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur ingame verfuegbar.");
            return true;
        }

        final Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("clearinv")) {
            player.closeInventory();
            player.setItemOnCursor(null);
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.getInventory().setHeldItemSlot(Math.max(0, Math.min(8, player.getInventory().getHeldItemSlot())));
            player.updateInventory();

            // 1.8 kann nach einem Command noch einen alten Client-Slot zurueckschreiben.
            // Deshalb serverseitigen Clear einen Tick spaeter nochmals erzwingen und synchronisieren.
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("SkyKings-Core"), new Runnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    player.setItemOnCursor(null);
                    player.getInventory().clear();
                    player.getInventory().setArmorContents(new ItemStack[4]);
                    player.updateInventory();
                }
            });

            player.sendMessage(ChatColor.GREEN + "Dein Inventar wurde vollstaendig geleert.");
            return true;
        }

        Inventory trash = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "SkyKings | Muell");
        player.openInventory(trash);
        player.sendMessage(ChatColor.GRAY + "Alles, was du in dieses Inventar legst, wird beim Schliessen "
                + ChatColor.RED + "dauerhaft geloescht" + ChatColor.GRAY + ".");
        return true;
    }
}
