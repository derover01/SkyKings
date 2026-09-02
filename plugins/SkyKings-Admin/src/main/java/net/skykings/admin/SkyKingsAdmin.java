package net.skykings.admin;

import net.skykings.admin.casino.CasinoCommand;
import net.skykings.admin.casino.CasinoNpcService;
import net.skykings.admin.cleanup.GroundClearService;
import net.skykings.admin.command.AnnouncementCommand;
import net.skykings.admin.command.ClearChatCommand;
import net.skykings.admin.command.CoinAdminCommand;
import net.skykings.admin.command.DiscordTestCommand;
import net.skykings.admin.command.GroundClearCommand;
import net.skykings.admin.command.MapTeleportCommand;
import net.skykings.admin.command.RankAdminCommand;
import net.skykings.admin.command.RightsAdminCommand;
import net.skykings.admin.command.SystemCheckCommand;
import net.skykings.admin.command.WarpAdminCommand;
import net.skykings.admin.command.WarpCommand;
import net.skykings.admin.discord.DiscordBridge;
import net.skykings.admin.discord.DiscordEventRelay;
import net.skykings.admin.event.FridayEventService;
import net.skykings.admin.event.StandaloneRaffleCommand;
import net.skykings.admin.warp.WarpService;
import net.skykings.admin.warp.WarpTeleportService;
import net.skykings.combat.tag.CombatTagService;
import net.skykings.combat.tag.CombatTagServiceImpl;
import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.discord.DiscordNotifier;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/** SkyKings-Admin: Staff-, Warp-, Community-Event-, Casino- und Verwaltungsfunktionen. */
public class SkyKingsAdmin extends JavaPlugin {

    private DiscordBridge discordBridge;
    private GroundClearService groundClearService;
    private WarpTeleportService warpTeleportService;
    private FridayEventService fridayEventService;

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
        CombatTagService combatTags = CombatTagServiceImpl.liveInstance();
        if (combatTags == null) {
            getLogger().severe("CombatTagService nicht gefunden - sichere Warps koennen nicht gestartet werden.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.discordBridge = new DiscordBridge(this);
        getServer().getServicesManager().register(DiscordNotifier.class, discordBridge, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(new DiscordEventRelay(discordBridge), this);

        PluginCommand rankCommand = getCommand("rang");
        PluginCommand rightsCommand = getCommand("rechte");
        PluginCommand addCoinsCommand = getCommand("addcoins");
        PluginCommand setCoinsCommand = getCommand("setcoins");
        PluginCommand announcementCommand = getCommand("announcement");
        PluginCommand clearChatCommand = getCommand("clearchat");
        PluginCommand groundClearCommand = getCommand("clear");
        PluginCommand systemCheckCommand = getCommand("skcheck");
        PluginCommand discordTestCommand = getCommand("discordtest");
        PluginCommand warpCommand = getCommand("warp");
        PluginCommand setWarpCommand = getCommand("setwarp");
        PluginCommand delWarpCommand = getCommand("delwarp");
        PluginCommand mapTeleportCommand = getCommand("maptp");
        PluginCommand fridayCommand = getCommand("freitag");
        PluginCommand raffleCommand = getCommand("verlosen");
        PluginCommand casinoCommand = getCommand("casino");
        PluginCommand casinoNpcCommand = getCommand("casinonpc");
        if (rankCommand == null || rightsCommand == null || addCoinsCommand == null || setCoinsCommand == null
                || announcementCommand == null || clearChatCommand == null || groundClearCommand == null
                || systemCheckCommand == null || discordTestCommand == null || warpCommand == null
                || setWarpCommand == null || delWarpCommand == null || mapTeleportCommand == null
                || fridayCommand == null || raffleCommand == null || casinoCommand == null || casinoNpcCommand == null) {
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

        CoinAdminCommand addCoinsExecutor = new CoinAdminCommand(core.getEconomyService(), CoinAdminCommand.Mode.ADD);
        addCoinsCommand.setExecutor(addCoinsExecutor);
        addCoinsCommand.setTabCompleter(addCoinsExecutor);
        CoinAdminCommand setCoinsExecutor = new CoinAdminCommand(core.getEconomyService(), CoinAdminCommand.Mode.SET);
        setCoinsCommand.setExecutor(setCoinsExecutor);
        setCoinsCommand.setTabCompleter(setCoinsExecutor);

        announcementCommand.setExecutor(new AnnouncementCommand());
        clearChatCommand.setExecutor(new ClearChatCommand());

        this.groundClearService = new GroundClearService(this);
        groundClearCommand.setExecutor(new GroundClearCommand(groundClearService));
        groundClearService.startAutomaticCycle();

        WarpService warpService = new WarpService(this);
        this.warpTeleportService = new WarpTeleportService(this, warpService, combatTags);
        getServer().getPluginManager().registerEvents(warpTeleportService, this);
        WarpCommand warpExecutor = new WarpCommand(warpService, warpTeleportService);
        warpCommand.setExecutor(warpExecutor);
        warpCommand.setTabCompleter(warpExecutor);
        WarpAdminCommand warpAdminExecutor = new WarpAdminCommand(warpService);
        setWarpCommand.setExecutor(warpAdminExecutor);
        setWarpCommand.setTabCompleter(warpAdminExecutor);
        delWarpCommand.setExecutor(warpAdminExecutor);
        delWarpCommand.setTabCompleter(warpAdminExecutor);
        MapTeleportCommand mapExecutor = new MapTeleportCommand();
        mapTeleportCommand.setExecutor(mapExecutor);
        mapTeleportCommand.setTabCompleter(mapExecutor);

        this.fridayEventService = new FridayEventService(this, core, warpService);
        fridayCommand.setExecutor(fridayEventService);
        raffleCommand.setExecutor(new StandaloneRaffleCommand(this, fridayEventService));

        CasinoCommand casino = new CasinoCommand(core);
        casinoCommand.setExecutor(casino);
        CasinoNpcService casinoNpcs = new CasinoNpcService(this, casino);
        casinoNpcCommand.setExecutor(casinoNpcs);
        casinoNpcCommand.setTabCompleter(casinoNpcs);
        getServer().getPluginManager().registerEvents(casinoNpcs, this);

        systemCheckCommand.setExecutor(new SystemCheckCommand());
        discordTestCommand.setExecutor(new DiscordTestCommand(discordBridge));

        if (discordBridge.isConfigured("status")) {
            discordBridge.send("status", "🟢 SkyKings-Admin wurde gestartet.");
        }
        getLogger().info("SkyKings-Admin mit Coin-Verwaltung, Combat-Warps, Freitags-Community-Event, globalen Verlosungen, Void-Crown-Casino/NPC-Stationen, Map-, Rang-, Rechte-, Announcement-, Boden-Clear-, Diagnose- und Discord-Tools aktiviert.");
    }

    @Override
    public void onDisable() {
        if (fridayEventService != null) fridayEventService.shutdown();
        if (warpTeleportService != null) warpTeleportService.shutdown();
        if (groundClearService != null) groundClearService.stopAutomaticCycle();
        getServer().getServicesManager().unregisterAll(this);
        if (discordBridge != null) {
            if (discordBridge.isConfigured("status")) discordBridge.send("status", "🔴 SkyKings-Admin wird beendet.");
            discordBridge.shutdown();
        }
        getLogger().info("SkyKings-Admin deaktiviert.");
    }
}
