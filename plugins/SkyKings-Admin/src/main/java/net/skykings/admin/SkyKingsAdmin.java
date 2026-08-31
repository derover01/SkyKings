package net.skykings.admin;

import net.skykings.admin.command.AnnouncementCommand;
import net.skykings.admin.command.ClearChatCommand;
import net.skykings.admin.command.RankAdminCommand;
import net.skykings.admin.command.RightsAdminCommand;
import net.skykings.admin.command.SystemCheckCommand;
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
        PluginCommand announcementCommand = getCommand("announcement");
        PluginCommand clearChatCommand = getCommand("clearchat");
        PluginCommand systemCheckCommand = getCommand("skcheck");
        if (rankCommand == null || rightsCommand == null || announcementCommand == null || clearChatCommand == null
                || systemCheckCommand == null) {
            getLogger().severe("Ein SkyKings-Admin-Command fehlt in plugin.yml - Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        RankAdminCommand rankExecutor = new RankAdminCommand(core.getRankService());
        rankCommand.setExecutor(rankExecutor);
        rankCommand.setTabCompleter(rankExecutor);

        RightsAdminCommand rightsExecutor = new RightsAdminCommand(core.getVoucherPermissionService());
        rightsCommand.setExecutor(rightsExecutor);
        rightsCommand.setTabCompleter(rightsExecutor);

        announcementCommand.setExecutor(new AnnouncementCommand());
        clearChatCommand.setExecutor(new ClearChatCommand());
        systemCheckCommand.setExecutor(new SystemCheckCommand());

        getLogger().info("SkyKings-Admin mit Rang-, Rechte-, Announcement-, Chat- und Diagnose-Tools aktiviert.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SkyKings-Admin deaktiviert.");
    }
}
