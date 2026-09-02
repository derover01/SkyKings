package net.skykings.core.shop.player;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Kompatibilitaets-Entry-Point fuer die bestehende Core-Registrierung.
 * Die eigentliche Runtime-Logik liegt im PlayerShopTradeController; der separate
 * Merchant-Safety-Listener schuetzt das echte 1.8-Villagerfenster vor Vanilla-Moves.
 */
public final class PlayerShopController implements Listener, CommandExecutor {
    private final PlayerShopTradeController delegate;

    public PlayerShopController(PlayerShopService service) {
        this.delegate = new PlayerShopTradeController(service);
        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(PlayerShopController.class);
        Bukkit.getPluginManager().registerEvents(delegate, plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerShopMerchantSafetyListener(), plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return delegate.onCommand(sender, command, label, args);
    }
}
