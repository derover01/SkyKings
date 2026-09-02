package net.skykings.core;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.clan.ClanBaseService;
import net.skykings.core.clan.ClanCommand;
import net.skykings.core.clan.ClanService;
import net.skykings.core.command.BlocksCommand;
import net.skykings.core.command.BuildModeCommand;
import net.skykings.core.command.CommandsCommand;
import net.skykings.core.command.CommandsGui;
import net.skykings.core.command.EnderChestCommand;
import net.skykings.core.command.FlyCommand;
import net.skykings.core.command.GamemodeCommand;
import net.skykings.core.command.KitCommand;
import net.skykings.core.command.PortableInventoryCommand;
import net.skykings.core.command.RanksCommand;
import net.skykings.core.command.RankupCommand;
import net.skykings.core.command.RepairCommand;
import net.skykings.core.command.SellCommand;
import net.skykings.core.command.ShopCommand;
import net.skykings.core.command.SpeedCommand;
import net.skykings.core.command.StackCommand;
import net.skykings.core.command.TradeCommand;
import net.skykings.core.command.TrashCommand;
import net.skykings.core.command.WorthCommand;
import net.skykings.core.config.ConfigService;
import net.skykings.core.config.ConfigServiceImpl;
import net.skykings.core.config.StorageType;
import net.skykings.core.cooldown.CooldownService;
import net.skykings.core.cooldown.CooldownServiceImpl;
import net.skykings.core.display.OwnerAccessListener;
import net.skykings.core.display.PaidRankHologramListener;
import net.skykings.core.display.PlayerDisplayListener;
import net.skykings.core.display.PlayerDisplayService;
import net.skykings.core.display.PlayerJoinMessageListener;
import net.skykings.core.display.RankDisplayConfig;
import net.skykings.core.display.SkyKingsScoreboardService;
import net.skykings.core.economy.EconomyService;
import net.skykings.core.economy.EconomyServiceImpl;
import net.skykings.core.enderchest.EnderChestBlockListener;
import net.skykings.core.enderchest.EnderChestService;
import net.skykings.core.freesign.FreeSignListener;
import net.skykings.core.freesign.FreeSignStore;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.integration.EconomyBridge;
import net.skykings.core.integration.NoOpEconomyBridge;
import net.skykings.core.integration.NoOpPermissionBridge;
import net.skykings.core.integration.PermissionBridge;
import net.skykings.core.integration.luckperms.LuckPermsPermissionBridge;
import net.skykings.core.integration.vault.VaultEconomyBridge;
import net.skykings.core.island.IslandCommand;
import net.skykings.core.island.IslandProtectionListener;
import net.skykings.core.island.IslandService;
import net.skykings.core.kit.KitGrantService;
import net.skykings.core.kit.KitGrantServiceImpl;
import net.skykings.core.kit.KitGui;
import net.skykings.core.kit.KitRegistry;
import net.skykings.core.kit.KitRegistryImpl;
import net.skykings.core.kit.RankKitLoader;
import net.skykings.core.listener.InventoryDropSyncListener;
import net.skykings.core.listener.PlayerLifecycleListener;
import net.skykings.core.logging.AuditSink;
import net.skykings.core.logging.LoggingService;
import net.skykings.core.logging.LoggingServiceImpl;
import net.skykings.core.logging.PersistentAuditSink;
import net.skykings.core.logging.PluginLoggerAuditSink;
import net.skykings.core.netherstar.NetherstarService;
import net.skykings.core.netherstar.NetherstarServiceImpl;
import net.skykings.core.permission.VoucherPermissionService;
import net.skykings.core.perk.BuildBlockSafetyListener;
import net.skykings.core.perk.BuildBlockStore;
import net.skykings.core.perk.BuildBlockWorldListener;
import net.skykings.core.perk.BuildBlocksGui;
import net.skykings.core.plot.PlotCommand;
import net.skykings.core.plot.PlotProtectionListener;
import net.skykings.core.plot.PlotService;
import net.skykings.core.profile.PlayerProfileService;
import net.skykings.core.profile.PlayerProfileServiceImpl;
import net.skykings.core.protection.MapProtectionService;
import net.skykings.core.rank.RankProgressionConfig;
import net.skykings.core.rank.RankProgressionService;
import net.skykings.core.rank.RankService;
import net.skykings.core.rank.RankServiceImpl;
import net.skykings.core.rank.RanksGui;
import net.skykings.core.retention.DailyRewardCommand;
import net.skykings.core.retention.DailyRewardService;
import net.skykings.core.shop.PvpRestockShopGui;
import net.skykings.core.shop.ShopNpcService;
import net.skykings.core.shop.ShopPriceRegistry;
import net.skykings.core.shop.ShopTransactionService;
import net.skykings.core.shop.SystemShopGui;
import net.skykings.core.shop.player.IslandShopPlacementPolicy;
import net.skykings.core.shop.player.PlayerShopController;
import net.skykings.core.shop.player.PlayerShopService;
import net.skykings.core.shop.player.PlayerShopStore;
import net.skykings.core.spawner.MobStackService;
import net.skykings.core.spawner.SpawnerStackService;
import net.skykings.core.storage.DataStore;
import net.skykings.core.storage.DataStoreException;
import net.skykings.core.storage.sqlite.SQLiteDataStore;
import net.skykings.core.trade.TradeGuiService;
import net.skykings.core.trade.TradeService;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** SkyKings-Core: zentrale Player-, Economy-, Rank-, Claim-, Shop-, Clan- und Display-Services. */
public final class SkyKingsCore extends JavaPlugin implements SkyKingsCoreAPI {
    private DataStore dataStore;
    private ExecutorService dbExecutor;
    private ConfigService configService;
    private LoggingService loggingService;
    private PlayerProfileService playerProfileService;
    private RankService rankService;
    private RankProgressionService rankProgressionService;
    private EconomyService economyService;
    private NetherstarService netherstarService;
    private CooldownService cooldownService;
    private PermissionBridge permissionBridge;
    private EconomyBridge economyBridge;
    private KitRegistry kitRegistry;
    private KitGrantService kitGrantService;
    private GuiManager guiManager;
    private VoucherPermissionService voucherPermissionService;
    private FreeSignStore freeSignStore;
    private BuildBlockStore buildBlockStore;
    private EnderChestService enderChestService;
    private ShopTransactionService shopTransactionService;
    private IslandService islandService;
    private PlotService plotService;
    private ClanService clanService;
    private ClanBaseService clanBaseService;
    private DailyRewardService dailyRewardService;
    private PlayerShopStore playerShopStore;
    private SpawnerStackService spawnerStackService;
    private MobStackService mobStackService;

    @Override
    public void onEnable() {
        this.configService = new ConfigServiceImpl(this);
        try {
            this.dataStore = createDataStore(); dataStore.initialize();
        } catch (DataStoreException e) {
            getLogger().log(Level.SEVERE, "Datenbank-Initialisierung fehlgeschlagen, SkyKings-Core wird deaktiviert.", e);
            getServer().getPluginManager().disablePlugin(this); return;
        }
        this.dbExecutor = Executors.newSingleThreadExecutor(runnable -> { Thread thread = new Thread(runnable, "SkyKings-Core-DB"); thread.setDaemon(true); return thread; });
        List<AuditSink> sinks = new ArrayList<AuditSink>();
        sinks.add(new PluginLoggerAuditSink(getLogger()));
        sinks.add(new PersistentAuditSink(dataStore, dbExecutor, getLogger()));
        this.loggingService = new LoggingServiceImpl(sinks, getLogger());
        this.playerProfileService = new PlayerProfileServiceImpl(dataStore, dbExecutor, loggingService, getLogger());
        this.permissionBridge = createPermissionBridge();
        this.rankService = new RankServiceImpl(playerProfileService, loggingService, permissionBridge);
        this.economyService = new EconomyServiceImpl(playerProfileService, loggingService);
        RankProgressionConfig rankProgressionConfig = new RankProgressionConfig(this);
        this.rankProgressionService = new RankProgressionService(rankService, economyService, rankProgressionConfig);
        this.netherstarService = new NetherstarServiceImpl(playerProfileService, loggingService);
        this.cooldownService = new CooldownServiceImpl(dataStore, dbExecutor, getLogger());
        this.kitRegistry = new KitRegistryImpl();
        new RankKitLoader(this, kitRegistry).loadAndRegister();
        this.kitGrantService = new KitGrantServiceImpl(kitRegistry, playerProfileService, cooldownService);
        this.guiManager = new GuiManager();
        this.voucherPermissionService = new VoucherPermissionService(this, permissionBridge, loggingService);
        this.freeSignStore = new FreeSignStore(this);
        this.buildBlockStore = new BuildBlockStore(this);
        this.enderChestService = new EnderChestService(this, rankService, economyService);
        this.shopTransactionService = new ShopTransactionService(economyService, loggingService);
        this.islandService = new IslandService(this);
        this.plotService = new PlotService(this);
        this.clanService = new ClanService(this);
        this.clanBaseService = new ClanBaseService(this, clanService);
        this.dailyRewardService = new DailyRewardService(this, economyService);
        this.playerShopStore = new PlayerShopStore(this);
        PlayerShopService playerShopService = new PlayerShopService(playerShopStore, economyService, loggingService);
        playerShopService.setPlacementPolicy(new IslandShopPlacementPolicy(islandService, plotService));
        PlayerShopController playerShopController = new PlayerShopController(playerShopService);
        this.spawnerStackService = new SpawnerStackService(this, islandService, plotService);
        this.mobStackService = new MobStackService(this, islandService, plotService);

        ShopPriceRegistry shopPrices = new ShopPriceRegistry(this);
        MapProtectionService mapProtectionService = new MapProtectionService();
        TradeService tradeService = new TradeService();
        TradeGuiService tradeGuiService = new TradeGuiService(this, tradeService, economyService, loggingService);
        RankDisplayConfig rankDisplayConfig = new RankDisplayConfig(this);
        PlayerDisplayService displayService = new PlayerDisplayService(playerProfileService, rankDisplayConfig, clanService);
        SkyKingsScoreboardService scoreboardService = new SkyKingsScoreboardService(playerProfileService, rankDisplayConfig);
        RanksGui ranksGui = new RanksGui(guiManager, rankService, rankProgressionService, rankProgressionConfig, economyService);
        KitGui kitGui = new KitGui(guiManager, kitGrantService, cooldownService);
        BuildBlocksGui buildBlocksGui = new BuildBlocksGui(guiManager);
        PaidRankHologramListener paidRankHolograms = new PaidRankHologramListener(this, rankService);
        CommandsGui commandsGui = new CommandsGui(guiManager);
        SystemShopGui systemShopGui = new SystemShopGui(guiManager, shopTransactionService);
        PvpRestockShopGui pvpRestockShopGui = new PvpRestockShopGui(guiManager, shopTransactionService);
        ShopNpcService shopNpcService = new ShopNpcService(this, systemShopGui, pvpRestockShopGui);
        this.economyBridge = createEconomyBridge();

        getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(playerProfileService, cooldownService, permissionBridge, getLogger()), this);
        getServer().getPluginManager().registerEvents(new InventoryDropSyncListener(this), this);
        getServer().getPluginManager().registerEvents(new OwnerAccessListener(rankDisplayConfig, permissionBridge), this);
        getServer().getPluginManager().registerEvents(new PlayerDisplayListener(displayService), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinMessageListener(rankDisplayConfig, rankService), this);
        getServer().getPluginManager().registerEvents(new FreeSignListener(freeSignStore, guiManager), this);
        getServer().getPluginManager().registerEvents(new BuildBlockSafetyListener(), this);
        getServer().getPluginManager().registerEvents(new BuildBlockWorldListener(buildBlockStore), this);
        getServer().getPluginManager().registerEvents(paidRankHolograms, this);
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(enderChestService, this);
        getServer().getPluginManager().registerEvents(new EnderChestBlockListener(enderChestService), this);
        getServer().getPluginManager().registerEvents(shopNpcService, this);
        getServer().getPluginManager().registerEvents(mapProtectionService, this);
        getServer().getPluginManager().registerEvents(tradeGuiService, this);
        getServer().getPluginManager().registerEvents(new IslandProtectionListener(islandService), this);
        getServer().getPluginManager().registerEvents(new PlotProtectionListener(plotService), this);
        getServer().getPluginManager().registerEvents(clanService, this);
        getServer().getPluginManager().registerEvents(clanBaseService, this);
        getServer().getPluginManager().registerEvents(playerShopController, this);
        getServer().getPluginManager().registerEvents(spawnerStackService, this);
        getServer().getPluginManager().registerEvents(mobStackService, this);

        getServer().getScheduler().runTaskTimer(this, () -> getServer().getOnlinePlayers().forEach(player -> {
            displayService.refreshTab(player); paidRankHolograms.refresh(player); scoreboardService.refresh(player);
        }), 40L, 40L);

        if (!registerCommand("commands", new CommandsCommand(commandsGui))) return;
        PluginCommand kitCommand = requireCommand("kit"); if (kitCommand == null) return;
        KitCommand kitExecutor = new KitCommand(kitGrantService, kitGui); kitCommand.setExecutor(kitExecutor); kitCommand.setTabCompleter(kitExecutor);
        PluginCommand rankupCommand = requireCommand("rankup"); if (rankupCommand == null) return; rankupCommand.setExecutor(new RankupCommand(rankProgressionService));
        PluginCommand ranksCommand = requireCommand("raenge"); if (ranksCommand == null) return; ranksCommand.setExecutor(new RanksCommand(ranksGui));
        if (!registerCommand("fly", new FlyCommand(rankService))) return;
        if (!registerCommand("speed", new SpeedCommand())) return;
        if (!registerCommand("stack", new StackCommand(rankService))) return;
        if (!registerCommand("bloecke", new BlocksCommand(rankService, buildBlocksGui))) return;
        if (!registerCommand("repair", new RepairCommand(rankService))) return;
        if (!registerCommand("enderchest", new EnderChestCommand(enderChestService))) return;
        if (!registerCommand("anvil", new PortableInventoryCommand(PortableInventoryCommand.Type.ANVIL, "skykings.perk.anvil"))) return;
        if (!registerCommand("workbench", new PortableInventoryCommand(PortableInventoryCommand.Type.WORKBENCH, "skykings.perk.workbench"))) return;
        if (!registerCommand("enchantmenttable", new PortableInventoryCommand(PortableInventoryCommand.Type.ENCHANTING, "skykings.perk.enchantmenttable"))) return;
        if (!registerCommand("buildmode", new BuildModeCommand(mapProtectionService))) return;
        if (!registerCommand("trade", new TradeCommand(tradeService, tradeGuiService))) return;
        if (!registerCommand("shop", new ShopCommand(systemShopGui))) return;
        if (!registerCommand("worth", new WorthCommand(shopPrices))) return;
        if (!registerCommand("sell", new SellCommand(shopPrices, economyService))) return;
        PluginCommand shopNpcCommand = requireCommand("shopnpc"); if (shopNpcCommand == null) return;
        shopNpcCommand.setExecutor(shopNpcService); shopNpcCommand.setTabCompleter(shopNpcService);
        PluginCommand islandCommand = requireCommand("island"); if (islandCommand == null) return;
        IslandCommand islandExecutor = new IslandCommand(islandService); islandCommand.setExecutor(islandExecutor); islandCommand.setTabCompleter(islandExecutor);
        if (!registerCommand("plot", new PlotCommand(plotService))) return;
        if (!registerCommand("clan", new ClanCommand(clanService, clanBaseService))) return;
        if (!registerCommand("playershop", playerShopController)) return;
        if (!registerCommand("spawnerstack", spawnerStackService)) return;
        if (!registerCommand("dailyrewards", new DailyRewardCommand(dailyRewardService))) return;
        if (!registerCommand("gm", new GamemodeCommand())) return;
        TrashCommand trashCommand = new TrashCommand();
        if (!registerCommand("trash", trashCommand)) return;
        if (!registerCommand("clearinv", trashCommand)) return;

        getServer().getServicesManager().register(SkyKingsCoreAPI.class, this, this, ServicePriority.Normal);
        logIntegrationStatus();
        getLogger().info("SkyKings-Core (Claims + Plots + Clans/ClanBase + PlayerShops + Spawner/Mob-Stacking + Retention) aktiviert. Storage: " + configService.getStorageType());
    }

    private boolean registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = requireCommand(name); if (command == null) return false; command.setExecutor(executor); return true;
    }
    private PluginCommand requireCommand(String name) {
        PluginCommand command = getCommand(name); if (command != null) return command;
        getLogger().severe("/" + name + " fehlt in plugin.yml - SkyKings-Core wird deaktiviert.");
        getServer().getPluginManager().disablePlugin(this); return null;
    }

    @Override
    public void onDisable() {
        if (spawnerStackService != null) spawnerStackService.save();
        if (dailyRewardService != null) dailyRewardService.save();
        if (clanBaseService != null) clanBaseService.save();
        if (clanService != null) clanService.save();
        if (playerShopStore != null) playerShopStore.save();
        if (plotService != null) plotService.save();
        if (islandService != null) islandService.save();
        if (enderChestService != null) enderChestService.shutdown();
        if (buildBlockStore != null) buildBlockStore.shutdown();
        if (freeSignStore != null) freeSignStore.shutdown();
        if (playerProfileService != null) playerProfileService.saveAll();
        if (dbExecutor != null) {
            dbExecutor.shutdown();
            try { if (!dbExecutor.awaitTermination(5, TimeUnit.SECONDS)) getLogger().warning("SkyKings-Core-DB-Executor wurde nach 5 Sekunden nicht sauber beendet."); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        if (dataStore != null) dataStore.close();
        getServer().getServicesManager().unregisterAll(this);
        getLogger().info("SkyKings-Core deaktiviert.");
    }

    private DataStore createDataStore() {
        StorageType type = configService.getStorageType();
        if (type == StorageType.SQLITE) return new SQLiteDataStore(new File(getDataFolder(), configService.getSqliteFileName()), getLogger());
        throw new DataStoreException("MySQL/MariaDB-Storage ist architektonisch vorbereitet, aber noch nicht implementiert. Bitte storage.type: SQLITE verwenden.");
    }
    private PermissionBridge createPermissionBridge() {
        if (getServer().getPluginManager().getPlugin("LuckPerms") == null) return new NoOpPermissionBridge();
        try { return LuckPermsPermissionBridge.createIfAvailable(getLogger()); }
        catch (Throwable t) { getLogger().log(Level.WARNING, "LuckPerms erkannt, aber die Bridge konnte nicht initialisiert werden.", t); return new NoOpPermissionBridge(); }
    }
    private EconomyBridge createEconomyBridge() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return new NoOpEconomyBridge();
        try { return VaultEconomyBridge.createAndRegister(this, economyService, getLogger()); }
        catch (Throwable t) { getLogger().log(Level.WARNING, "Vault/VaultUnlocked erkannt, aber die Economy-Bridge konnte nicht registriert werden.", t); return new NoOpEconomyBridge(); }
    }
    private void logIntegrationStatus() {
        getLogger().info("LuckPerms verfügbar: " + (getServer().getPluginManager().getPlugin("LuckPerms") != null));
        getLogger().info("PermissionBridge aktiv: " + permissionBridge.isAvailable());
        getLogger().info("Vault/VaultUnlocked verfügbar: " + (getServer().getPluginManager().getPlugin("Vault") != null));
        getLogger().info("SkyKings Economy Provider registriert: " + economyBridge.isRegistered());
    }

    @Override public PlayerProfileService getPlayerProfileService() { return playerProfileService; }
    @Override public RankService getRankService() { return rankService; }
    @Override public EconomyService getEconomyService() { return economyService; }
    @Override public NetherstarService getNetherstarService() { return netherstarService; }
    @Override public CooldownService getCooldownService() { return cooldownService; }
    @Override public LoggingService getLoggingService() { return loggingService; }
    @Override public KitRegistry getKitRegistry() { return kitRegistry; }
    @Override public KitGrantService getKitGrantService() { return kitGrantService; }
    @Override public GuiManager getGuiManager() { return guiManager; }
    @Override public VoucherPermissionService getVoucherPermissionService() { return voucherPermissionService; }
    @Override public ShopTransactionService getShopTransactionService() { return shopTransactionService; }
    @Override public ClanService getClanService() { return clanService; }
}
