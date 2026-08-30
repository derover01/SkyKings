package net.skykings.admin;

import net.skykings.admin.command.RankAdminCommand;
import net.skykings.admin.command.RightsAdminCommand;
import net.skykings.core.api.SkyKingsCoreAPI;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/** SkyKings-Admin: Staff- und Verwaltungsfunktionen. */
public class SkyKingsAdmin extends JavaPlugin {

    @Override
    public void onEnable() {
        RegisteredServiceProvider<SkyKingsCoreAPI> registration =
                getServer().getServicesManager().getRegistration(SkyKingsCoreAPI.class);
        if (registration == null) {
            getLogger().severe("SkyKingsCoreAPI nicht gefunden - SkyKings-Admin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        SkyKingsCoreAPI core = registration.getProvider();

        PluginCommand rankCommand = getCommand("rang");
        PluginCommand rightsCommand = getCommand("rechte");
        if (rankCommand == null || rightsCommand == null) {
            getLogger().severe("/rang oder /rechte fehlt in plugin.yml - SkyKings-Admin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        RankAdminCommand rankExecutor = new RankAdminCommand(core.getRankService());
        rankCommand.setExecutor(rankExecutor);
        rankCommand.setTabCompleter(rankExecutor);

        RightsAdminCommand rightsExecutor = new RightsAdminCommand(core.getVoucherPermissionService());
        rightsCommand.setExecutor(rightsExecutor);
        rightsCommand.setTabCompleter(rightsExecutor);

        getLogger().info("SkyKings-Admin mit /rang und /rechte aktiviert.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SkyKings-Admin deaktiviert.");
    }
}
