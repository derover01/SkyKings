package net.skykings.core.command;

import net.skykings.core.shop.SystemShopGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /shop öffnet den zentralen SkyKings-Systemshop. */
public final class ShopCommand implements CommandExecutor {

    private final SystemShopGui shopGui;

    public ShopCommand(SystemShopGui shopGui) {
        this.shopGui = shopGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur für Spieler verfügbar.");
            return true;
        }
        shopGui.open((Player) sender);
        return true;
    }
}
