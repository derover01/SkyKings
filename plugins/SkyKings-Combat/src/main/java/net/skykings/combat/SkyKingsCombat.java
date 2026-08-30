package net.skykings.combat;

import net.skykings.combat.antifarm.AntiFarmService;
import net.skykings.combat.antifarm.AntiFarmServiceImpl;
import net.skykings.combat.config.CombatConfig;
import net.skykings.combat.cosmetic.KillCosmeticService;
import net.skykings.combat.cosmetic.KillEffectCommand;
import net.skykings.combat.cosmetic.KillEffectGui;
import net.skykings.combat.falldamage.FallDamageListener;
import net.skykings.combat.kill.BountyService;
import net.skykings.combat.kill.CombatDeathListener;
import net.skykings.combat.kill.CombatKillService;
import net.skykings.combat.kill.CombatKillServiceImpl;
import net.skykings.combat.kill.KillMessageService;
import net.skykings.combat.kill.NetherstarRewardDelivery;
import net.skykings.combat.kill.PhysicalNetherstarRewardDelivery;
import net.skykings.combat.killstreak.KillstreakService;
import net.skykings.combat.killstreak.KillstreakServiceImpl;
import net.skykings.combat.loot.LootPickupListener;
import net.skykings.combat.loot.LootProtectionService;
import net.skykings.combat.loot.LootProtectionServiceImpl;
import net.skykings.combat.map.MapGameplayService;
import net.skykings.combat.newbie.NewbieProtectionService;
import net.skykings.combat.newbie.NewbieProtectionServiceImpl;
import net.skykings.combat.pearl.EnderpearlCooldownListener;
import net.skykings.combat.pvp.PvpDamageListener;
import net.skykings.combat.starterkit.DeathStarterKit;
import net.skykings.combat.starterkit.DeathStarterKitService;
import net.skykings.combat.starterkit.DeathStarterKits;
import net.skykings.combat.starterkit.StarterKitRespawnListener;
import net.skykings.combat.stats.PvpStatsService;
import net.skykings.combat.stats.StatsCommand;
import net.skykings.combat.stats.TopCommand;
import net.skykings.combat.tag.CombatFlyCommandListener;
import net.skykings.combat.tag.CombatTagService;
import net.skykings.combat.tag.CombatTagServiceImpl;
import net.skykings.combat.tag.LastAttackerService;
import net.skykings.combat.tag.LastAttackerServiceImpl;
import net.skykings.combat.util.MessageCooldownTracker;
import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.cooldown.CooldownService;
import net.skykings.core.profile.PlayerProfileService;
import net.skykings.core.pvp.PvpStatsProvider;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/** SkyKings-Combat: zentrale SkyPvP-Regeln und Combat-Schutzsysteme. */
public final class SkyKingsCombat extends JavaPlugin {

    private PvpStatsService pvpStatsService;
    private KillCosmeticService killCosmeticService;
    private MapGameplayService mapGameplayService;

    @Override
    public void onEnable() {
        SkyKingsCoreAPI coreApi = resolveCoreApi();
        if (coreApi == null) {
            getLogger().severe("SkyKingsCoreAPI wurde nicht gefunden - SkyKings-Combat wird deaktiviert. Ist SkyKings-Core installiert und erfolgreich gestartet?");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        CombatConfig config = new CombatConfig(this);
        PlayerProfileService profileService = coreApi.getPlayerProfileService();
        CooldownService cooldownService = coreApi.getCooldownService();

        CombatTagService combatTagService = new CombatTagServiceImpl(config.getCombatTagMillis());
        LastAttackerService lastAttackerService = new LastAttackerServiceImpl(config.getCombatTagMillis());
        NewbieProtectionService newbieProtectionService = new NewbieProtectionServiceImpl(profileService, config.getNewbieProtectionMillis());
        KillstreakService killstreakService = new KillstreakServiceImpl(config.getBaseNetherstarsPerKill(), config.getKillstreakTiers());
        AntiFarmService antiFarmService = new AntiFarmServiceImpl(config.getAntiFarmFullRewardMaxKills(),
                config.getAntiFarmHalfRewardMaxKills(), config.getAntiFarmHalfRewardMultiplier());
        LootProtectionService lootProtectionService = new LootProtectionServiceImpl(this, config.getLootProtectionMillis());
        NetherstarRewardDelivery rewardDelivery = new PhysicalNetherstarRewardDelivery();
        BountyService bountyService = new BountyService(coreApi.getEconomyService(), rewardDelivery);
        this.killCosmeticService = new KillCosmeticService(this);
        KillMessageService killMessageService = new KillMessageService();
        this.mapGameplayService = new MapGameplayService(this);

        this.pvpStatsService = new PvpStatsService(this);
        getServer().getServicesManager().register(PvpStatsProvider.class, pvpStatsService, this, ServicePriority.Normal);

        CombatKillService combatKillService = new CombatKillServiceImpl(killstreakService, antiFarmService,
                rewardDelivery, lootProtectionService, pvpStatsService, bountyService, getLogger());

        DeathStarterKit starterKit = DeathStarterKits.createDefault(config.getStarterKitGoldenApples());
        DeathStarterKitService starterKitService = new DeathStarterKitService(starterKit, config.isStarterKitEnabled());

        MessageCooldownTracker newbieFeedbackCooldown = new MessageCooldownTracker(config.getFeedbackMessageCooldownMillis());
        MessageCooldownTracker pearlFeedbackCooldown = new MessageCooldownTracker(config.getFeedbackMessageCooldownMillis());

        getServer().getPluginManager().registerEvents(new FallDamageListener(), this);
        getServer().getPluginManager().registerEvents(
                new PvpDamageListener(combatTagService, lastAttackerService, newbieProtectionService, newbieFeedbackCooldown), this);
        getServer().getPluginManager().registerEvents(new CombatFlyCommandListener(combatTagService), this);
        getServer().getPluginManager().registerEvents(
                new EnderpearlCooldownListener(cooldownService, config.getEnderpearlCooldownMillis(), pearlFeedbackCooldown), this);
        getServer().getPluginManager().registerEvents(
                new CombatDeathListener(combatKillService, combatTagService, lastAttackerService,
                        killMessageService, killCosmeticService, getLogger()), this);
        getServer().getPluginManager().registerEvents(new StarterKitRespawnListener(starterKitService), this);
        getServer().getPluginManager().registerEvents(new LootPickupListener(lootProtectionService), this);
        getServer().getPluginManager().registerEvents(mapGameplayService, this);

        PluginCommand statsCommand = getCommand("stats");
        PluginCommand topCommand = getCommand("top");
        PluginCommand killEffectCommand = getCommand("killeffect");
        PluginCommand mapLootCommand = getCommand("maploot");
        PluginCommand supplyDropCommand = getCommand("supplydrop");
        if (statsCommand == null || topCommand == null || killEffectCommand == null || mapLootCommand == null || supplyDropCommand == null) {
            getLogger().severe("Ein SkyKings-Combat-Command fehlt in plugin.yml - Modul wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        statsCommand.setExecutor(new StatsCommand(pvpStatsService));
        TopCommand topExecutor = new TopCommand(pvpStatsService);
        topCommand.setExecutor(topExecutor);
        getServer().getPluginManager().registerEvents(topExecutor, this);
        killEffectCommand.setExecutor(new KillEffectCommand(new KillEffectGui(coreApi.getGuiManager(), killCosmeticService)));
        mapLootCommand.setExecutor(mapGameplayService);
        supplyDropCommand.setExecutor(mapGameplayService);

        getLogger().info("SkyKingsCoreAPI gefunden: true");
        getLogger().info("SkyKings-Combat (PvP + Bounty + Kill-Effects + Map-Loot + Supply-Drops) aktiviert.");
    }

    @Override
    public void onDisable() {
        if (mapGameplayService != null) mapGameplayService.shutdown();
        if (killCosmeticService != null) killCosmeticService.shutdown();
        if (pvpStatsService != null) pvpStatsService.shutdown();
        getServer().getServicesManager().unregisterAll(this);
        getLogger().info("SkyKings-Combat deaktiviert.");
    }

    private SkyKingsCoreAPI resolveCoreApi() {
        try {
            RegisteredServiceProvider<SkyKingsCoreAPI> registration = getServer().getServicesManager().getRegistration(SkyKingsCoreAPI.class);
            return registration != null ? registration.getProvider() : null;
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Konnte SkyKingsCoreAPI nicht auflösen.", t);
            return null;
        }
    }
}
