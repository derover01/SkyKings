package net.skykings.core.shop.rent;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.shop.player.PlayerShopService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Installiert Market Rentals im naechsten Tick, nachdem SkyKingsCoreAPI registriert wurde.
 * So bleibt der grosse Core-Bootstrap klein und die vorhandene Island/Plot-Policy wird nur erweitert.
 */
public final class ShopRentBootstrap {
    private ShopRentBootstrap() {}

    public static void installLater(final PlayerShopService playerShops) {
        if (playerShops == null) return;
        try {
            final Plugin raw = Bukkit.getPluginManager().getPlugin("SkyKings-Core");
            if (!(raw instanceof JavaPlugin) || !raw.isEnabled()) return;
            final JavaPlugin plugin = (JavaPlugin) raw;
            Bukkit.getScheduler().runTask(plugin, () -> install(plugin, playerShops));
        } catch (Throwable ignored) {
            // Unit-Tests ohne laufenden Bukkit-Server duerfen den Controller weiterhin konstruieren.
        }
    }

    private static void install(JavaPlugin plugin, PlayerShopService playerShops) {
        SkyKingsCoreAPI api = Bukkit.getServicesManager().load(SkyKingsCoreAPI.class);
        if (api == null) {
            plugin.getLogger().severe("ShopRent konnte nicht starten: SkyKingsCoreAPI fehlt.");
            return;
        }
        PluginCommand command = plugin.getCommand("shoprent");
        if (command == null) {
            plugin.getLogger().severe("/shoprent fehlt in plugin.yml.");
            return;
        }

        ShopRentService rentals = new ShopRentService(plugin, api.getEconomyService(), api.getGuiManager());
        playerShops.setPlacementPolicy(new RentalAwareShopPlacementPolicy(playerShops.getPlacementPolicy(), rentals));
        Bukkit.getPluginManager().registerEvents(rentals, plugin);
        command.setExecutor(rentals);
        command.setTabCompleter(rentals);
        plugin.getLogger().info("Market Rentals aktiviert: /shoprent");
    }
}
