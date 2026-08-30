package net.skykings.core;

import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.command.KitCommand;
import net.skykings.core.config.ConfigService;
import net.skykings.core.config.ConfigServiceImpl;
import net.skykings.core.config.StorageType;
import net.skykings.core.cooldown.CooldownService;
import net.skykings.core.cooldown.CooldownServiceImpl;
import net.skykings.core.economy.EconomyService;
import net.skykings.core.economy.EconomyServiceImpl;
import net.skykings.core.gui.GuiManager;
import net.skykings.core.integration.EconomyBridge;
import net.skykings.core.integration.NoOpEconomyBridge;
import net.skykings.core.integration.NoOpPermissionBridge;
import net.skykings.core.integration.PermissionBridge;
import net.skykings.core.integration.luckperms.LuckPermsPermissionBridge;
import net.skykings.core.integration.vault.VaultEconomyBridge;
import net.skykings.core.kit.KitGrantService;
import net.skykings.core.kit.KitGrantServiceImpl;
import net.skykings.core.kit.KitRegistry;
import net.skykings.core.kit.KitRegistryImpl;
import net.skykings.core.kit.RankKitLoader;
import net.skykings.core.listener.PlayerLifecycleListener;
import net.skykings.core.logging.AuditSink;
import net.skykings.core.logging.LoggingService;
import net.skykings.core.logging.LoggingServiceImpl;
import net.skykings.core.logging.PersistentAuditSink;
import net.skykings.core.logging.PluginLoggerAuditSink;
import net.skykings.core.netherstar.NetherstarService;
import net.skykings.core.netherstar.NetherstarServiceImpl;
import net.skykings.core.profile.PlayerProfileService;
import net.skykings.core.profile.PlayerProfileServiceImpl;
import net.skykings.core.rank.RankService;
import net.skykings.core.rank.RankServiceImpl;
import net.skykings.core.storage.DataStore;
import net.skykings.core.storage.DataStoreException;
import net.skykings.core.storage.sqlite.SQLiteDataStore;
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

/** SkyKings-Core: zentrale Player-, Economy-, Rank-, Cooldown- und Kit-Services. */
public final class SkyKingsCore extends JavaPlugin implements SkyKingsCoreAPI {

    private DataStore dataStore;
    private ExecutorService dbExecutor;
    private ConfigService configService;
    private LoggingService loggingService;
    private PlayerProfileService playerProfileService;
    private RankService rankService;
    private EconomyService economyService;
    private NetherstarService netherstarService;
    private CooldownService cooldownService;
    private PermissionBridge permissionBridge;
    private EconomyBridge economyBridge;
    private KitRegistry kitRegistry;
    private KitGrantService kitGrantService;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        this.configService = new ConfigServiceImpl(this);

        try {
            this.dataStore = createDataStore();
            dataStore.initialize();
        } catch (DataStoreException e) {
            getLogger().log(Level.SEVERE, "Datenbank-Initialisierung fehlgeschlagen, SkyKings-Core wird deaktiviert.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.dbExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "SkyKings-Core-DB");
            thread.setDaemon(true);
            return thread;
        });

        List<AuditSink> sinks = new ArrayList<>();
        sinks.add(new PluginLoggerAuditSink(getLogger()));
        sinks.add(new PersistentAuditSink(dataStore, dbExecutor, getLogger()));
        this.loggingService = new LoggingServiceImpl(sinks, getLogger());

        this.playerProfileService = new PlayerProfileServiceImpl(dataStore, dbExecutor, loggingService, getLogger());
        this.permissionBridge = createPermissionBridge();
        this.rankService = new RankServiceImpl(playerProfileService, loggingService, permissionBridge);
        this.economyService = new EconomyServiceImpl(playerProfileService, loggingService);
        this.netherstarService = new NetherstarServiceImpl(playerProfileService, loggingService);
        this.cooldownService = new CooldownServiceImpl(dataStore, dbExecutor, getLogger());
        this.kitRegistry = new KitRegistryImpl();
        new RankKitLoader(this, kitRegistry).loadAndRegister();
        this.kitGrantService = new KitGrantServiceImpl(kitRegistry, playerProfileService, cooldownService);
        this.guiManager = new GuiManager();

        this.economyBridge = createEconomyBridge();

        getServer().getPluginManager().registerEvents(
                new PlayerLifecycleListener(playerProfileService, cooldownService, permissionBridge, getLogger()), this);
        getServer().getPluginManager().registerEvents(guiManager, this);

        PluginCommand kitCommand = getCommand("kit");
        if (kitCommand == null) {
            getLogger().severe("/kit fehlt in plugin.yml - SkyKings-Core wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        KitCommand kitExecutor = new KitCommand(kitGrantService, cooldownService);
        kitCommand.setExecutor(kitExecutor);
        kitCommand.setTabCompleter(kitExecutor);

        getServer().getServicesManager().register(SkyKingsCoreAPI.class, this, this, ServicePriority.Normal);

        logIntegrationStatus();
        getLogger().info("SkyKings-Core (Phase 3 Rank-Kits) aktiviert. Storage: " + configService.getStorageType());
    }

    @Override
    public void onDisable() {
        if (playerProfileService != null) {
            playerProfileService.saveAll();
        }
        if (dbExecutor != null) {
            dbExecutor.shutdown();
            try {
                if (!dbExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    getLogger().warning("SkyKings-Core-DB-Executor wurde nach 5 Sekunden nicht sauber beendet.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (dataStore != null) {
            dataStore.close();
        }
        getServer().getServicesManager().unregisterAll(this);
        getLogger().info("SkyKings-Core deaktiviert.");
    }

    private DataStore createDataStore() {
        StorageType type = configService.getStorageType();
        if (type == StorageType.SQLITE) {
            File file = new File(getDataFolder(), configService.getSqliteFileName());
            return new SQLiteDataStore(file, getLogger());
        }
        throw new DataStoreException("MySQL/MariaDB-Storage ist architektonisch vorbereitet (siehe DataStore-"
                + "Interface), aber noch nicht implementiert. Bitte storage.type: SQLITE verwenden.");
    }

    private PermissionBridge createPermissionBridge() {
        if (getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            return new NoOpPermissionBridge();
        }
        try {
            return LuckPermsPermissionBridge.createIfAvailable(getLogger());
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "LuckPerms erkannt, aber die Bridge konnte nicht initialisiert werden - "
                    + "SkyKings-Core laeuft ohne Rang-Synchronisation weiter.", t);
            return new NoOpPermissionBridge();
        }
    }

    private EconomyBridge createEconomyBridge() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return new NoOpEconomyBridge();
        }
        try {
            return VaultEconomyBridge.createAndRegister(this, economyService, getLogger());
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Vault/VaultUnlocked erkannt, aber die Economy-Bridge konnte nicht "
                    + "registriert werden - SkyKings-Coins sind ueber Vault nicht nutzbar.", t);
            return new NoOpEconomyBridge();
        }
    }

    private void logIntegrationStatus() {
        boolean luckPerms = getServer().getPluginManager().getPlugin("LuckPerms") != null;
        boolean vault = getServer().getPluginManager().getPlugin("Vault") != null;
        getLogger().info("LuckPerms verfuegbar: " + luckPerms);
        getLogger().info("PermissionBridge aktiv: " + permissionBridge.isAvailable());
        getLogger().info("Vault/VaultUnlocked verfuegbar: " + vault);
        getLogger().info("SkyKings Economy Provider registriert: " + economyBridge.isRegistered());
    }

    @Override
    public PlayerProfileService getPlayerProfileService() {
        return playerProfileService;
    }

    @Override
    public RankService getRankService() {
        return rankService;
    }

    @Override
    public EconomyService getEconomyService() {
        return economyService;
    }

    @Override
    public NetherstarService getNetherstarService() {
        return netherstarService;
    }

    @Override
    public CooldownService getCooldownService() {
        return cooldownService;
    }

    @Override
    public LoggingService getLoggingService() {
        return loggingService;
    }

    @Override
    public KitRegistry getKitRegistry() {
        return kitRegistry;
    }

    @Override
    public KitGrantService getKitGrantService() {
        return kitGrantService;
    }

    @Override
    public GuiManager getGuiManager() {
        return guiManager;
    }
}
