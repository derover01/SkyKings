package net.skykings.core.shop.player;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Kompatibilitaets-Entry-Point fuer die bestehende Core-Registrierung.
 * Die eigentliche Runtime-Logik liegt im neuen 3x9 PlayerShopTradeController.
 */
public final class PlayerShopController implements Listener, CommandExecutor {
    private final PlayerShopTradeController delegate;

    public PlayerShopController(PlayerShopService service) {
        this.delegate = new PlayerShopTradeController(service);
        Bukkit.getPluginManager().registerEvents(delegate, JavaPlugin.getProvidingPlugin(PlayerShopController.class));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return delegate.onCommand(sender, command, label, args);
    }
}
