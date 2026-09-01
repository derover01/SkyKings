package net.skykings.combat;

import net.skykings.combat.antifarm.AntiFarmService;
import net.skykings.combat.antifarm.AntiFarmServiceImpl;
import net.skykings.combat.collection.CollectionCommand;
import net.skykings.combat.collection.HeadCollectionService;
import net.skykings.combat.collection.KillContextService;
import net.skykings.combat.collection.RevengeService;
import net.skykings.combat.community.GiveawayCommand;
import net.skykings.combat.community.PeaceCommand;
import net.skykings.combat.community.PeaceService;
import net.skykings.combat.config.CombatConfig;
import net.skykings.combat.cosmetic.KillCosmeticService;
import net.skykings.combat.cosmetic.KillEffectCommand;
import net.skykings.combat.cosmetic.KillEffectGui;
import net.skykings.combat.event.DuelCommand;
import net.skykings.combat.event.DuelService;
import net.skykings.combat.event.EventArenaCommand;
import net.skykings.combat.event.EventArenaService;
import net.skykings.combat.event.EventParticipationService;
import net.skykings.combat.event.LmsService;
import net.skykings.combat.event.TargetEventCommand;
import net.skykings.combat.event.TargetEventService;
import net.skykings.combat.event.TournamentCommand;
import net.skykings.combat.event.TournamentService;
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
import net.skykings.combat.map.IslandGameplayService;
import net.skykings.combat.map.MapDisplayCommand;
import net.skykings.combat.map.MapDisplayService;
import net.skykings.combat.map.MapGameplayService;
import net.skykings.combat.map.MapLandmarkCommand;
import net.skykings.combat.map.MapLandmarkService;
import net.skykings.combat.map.MapSetupCommand;
import net.skykings.combat.map.TrashBinService;
import net.skykings.combat.map.builder.SkyMapCommand;
import net.skykings.combat.map.route.MapRouteCommand;
import net.skykings.combat.map.route.MapRouteService;
import net.skykings.combat.map.secret.SecretCommand;
import net.skykings.combat.map.secret.SecretDiscoveryService;
import net.skykings.combat.map.secret.SecretLootRoomCommand;
import net.skykings.combat.map.secret.SecretLootRoomService;
import net.skykings.combat.map.zone.EndZoneCommand;
import net.skykings.combat.map.zone.EndZoneService;
import net.skykings.combat.map.zone.HotZoneCommand;
import net.skykings.combat.map.zone.HotZoneRewardListener;
import net.skykings.combat.map.zone.HotZoneService;
import net.skykings.combat.map.zone.KingAltarCommand;
import net.skykings.combat.map.zone.KingAltarService;
import net.skykings.combat.map.zone.MapMasteryCommand;
import net.skykings.combat.map.zone.MapMasteryService;
import net.skykings.combat.map.zone.PvpRegionCommand;
import net.skykings.combat.map.zone.PvpRegionService;
import net.skykings.combat.newbie.NewbieProtectionService;
import net.skykings.combat.newbie.NewbieProtectionServiceImpl;
import net.skykings.combat.pearl.EnderpearlCooldownListener;
import net.skykings.combat.pvp.PvpDamageListener;
import net.skykings.combat.retention.AchievementsCommand;
import net.skykings.combat.retention.BattlePassCommand;
import net.skykings.combat.retention.BattlePassService;
import net.skykings.combat.retention.LegacyHallCommand;
import net.skykings.combat.retention.LegacyHallService;
import net.skykings.combat.retention.QuestCommand;
import net.skykings.combat.retention.QuestService;
import net.skykings.combat.retention.SeasonCommand;
import net.skykings.combat.retention.SeasonMedalCommand;
import net.skykings.combat.retention.SeasonMedalService;
import net.skykings.combat.retention.SeasonProgressService;
import net.skykings.combat.spawn.SpawnService;
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
import net.skykings.combat.weapon.StatTrackCommand;
import net.skykings.combat.weapon.StatTrackItemService;
import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.cooldown.CooldownService;
import net.skykings.core.profile.PlayerProfileService;
import net.skykings.core.pvp.PvpStatsProvider;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/** SkyKings-Combat: PvP, Map Gameplay, Retention, Collection und Community-Systeme. */
public final class SkyKingsCombat extends JavaPlugin {

    private PvpStatsService pvpStatsService;
    private KillCosmeticService killCosmeticService;
    private MapGameplayService mapGameplayService;
    private SpawnService spawnService;
    private KingAltarService kingAltarService;
    private HotZoneService hotZoneService;
    private MapMasteryService mapMasteryService;
    private EndZoneService endZoneService;
    private SecretDiscoveryService secretDiscoveryService;
    private SecretLootRoomService secretLootRoomService;
    private MapRouteService mapRouteService;
    private MapLandmarkService mapLandmarkService;
    private IslandGameplayService islandGameplayService;
    private TrashBinService trashBinService;
    private MapDisplayService mapDisplayService;
    private PvpRegionService pvpRegionService;
    private TargetEventService targetEventService;
    private SeasonProgressService seasonProgressService;
    private LegacyHallService legacyHallService;
    private SeasonMedalService seasonMedalService;
    private HeadCollectionService headCollectionService;
    private PeaceService peaceService;
    private BattlePassService battlePassService;
    private QuestService questService;
    private EventArenaService eventArenaService;
    private DuelService duelService;
    private LmsService lmsService;
    private TournamentService tournamentService;

    @Override
    public void onEnable() {
        SkyKingsCoreAPI coreApi = resolveCoreApi();
        if (coreApi == null) {
            getLogger().severe("SkyKingsCoreAPI wurde nicht gefunden - SkyKings-Combat wird deaktiviert.");
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
        KillContextService killContextService = new KillContextService();
        RevengeService revengeService = new RevengeService(coreApi.getEconomyService());
        StatTrackItemService statTrackItemService = new StatTrackItemService();

        this.mapGameplayService = new MapGameplayService(this);
        this.spawnService = new SpawnService(this, combatTagService);
        this.mapMasteryService = new MapMasteryService(this);
        this.kingAltarService = new KingAltarService(this, coreApi.getEconomyService(), mapMasteryService);
        this.hotZoneService = new HotZoneService(this);
        this.endZoneService = new EndZoneService(this, coreApi.getEconomyService(), mapMasteryService);
        this.secretDiscoveryService = new SecretDiscoveryService(this, coreApi.getEconomyService(), mapMasteryService);
        this.secretLootRoomService = new SecretLootRoomService(this);
        this.mapRouteService = new MapRouteService(this, coreApi.getEconomyService());
        this.mapLandmarkService = new MapLandmarkService(this);
        this.islandGameplayService = new IslandGameplayService(this, mapLandmarkService, coreApi.getEconomyService());
        this.trashBinService = new TrashBinService(this);
        this.pvpRegionService = new PvpRegionService(this);
        this.pvpStatsService = new PvpStatsService(this);
        this.headCollectionService = new HeadCollectionService(this, killContextService, pvpStatsService);
        this.mapDisplayService = new MapDisplayService(this, pvpStatsService, kingAltarService, hotZoneService);
        this.seasonProgressService = new SeasonProgressService(this);
        this.legacyHallService = new LegacyHallService(this);
        this.seasonMedalService = new SeasonMedalService(this, seasonProgressService, pvpStatsService, legacyHallService);
        this.peaceService = new PeaceService(this);
        this.battlePassService = new BattlePassService(this, seasonProgressService, coreApi.getEconomyService());
        this.questService = new QuestService(this, coreApi.getEconomyService());
        this.eventArenaService = new EventArenaService(this);
        this.duelService = new DuelService(this, eventArenaService, combatTagService, coreApi.getEconomyService());
        this.lmsService = new LmsService(this, eventArenaService, coreApi.getEconomyService());
        this.tournamentService = new TournamentService(this, eventArenaService, coreApi.getEconomyService());
        this.targetEventService = new TargetEventService(this, pvpRegionService, coreApi.getEconomyService(), EventParticipationService.global());
        getServer().getServicesManager().register(PvpStatsProvider.class, pvpStatsService, this, ServicePriority.Normal);

        CombatKillService combatKillService = new CombatKillServiceImpl(killstreakService, antiFarmService,
                rewardDelivery, lootProtectionService, pvpStatsService, bountyService, getLogger());

        DeathStarterKit starterKit = DeathStarterKits.createDefault(config.getStarterKitGoldenApples());
        DeathStarterKitService starterKitService = new DeathStarterKitService(starterKit, config.isStarterKitEnabled());
        MessageCooldownTracker newbieFeedbackCooldown = new MessageCooldownTracker(config.getFeedbackMessageCooldownMillis());
        MessageCooldownTracker pearlFeedbackCooldown = new MessageCooldownTracker(config.getFeedbackMessageCooldownMillis());

        getServer().getPluginManager().registerEvents(new FallDamageListener(), this);
        getServer().getPluginManager().registerEvents(new PvpDamageListener(combatTagService, lastAttackerService,
                newbieProtectionService, newbieFeedbackCooldown), this);
        getServer().getPluginManager().registerEvents(new CombatFlyCommandListener(combatTagService), this);
        getServer().getPluginManager().registerEvents(new EnderpearlCooldownListener(cooldownService,
                config.getEnderpearlCooldownMillis(), pearlFeedbackCooldown), this);
        getServer().getPluginManager().registerEvents(killContextService, this);
        getServer().getPluginManager().registerEvents(new CombatDeathListener(combatKillService, combatTagService,
                lastAttackerService, killMessageService, killCosmeticService, EventParticipationService.global(), getLogger()), this);
        getServer().getPluginManager().registerEvents(headCollectionService, this);
        getServer().getPluginManager().registerEvents(revengeService, this);
        getServer().getPluginManager().registerEvents(statTrackItemService, this);
        getServer().getPluginManager().registerEvents(new HotZoneRewardListener(hotZoneService,
                coreApi.getEconomyService(), mapMasteryService), this);
        getServer().getPluginManager().registerEvents(endZoneService, this);
        getServer().getPluginManager().registerEvents(secretDiscoveryService, this);
        getServer().getPluginManager().registerEvents(secretLootRoomService, this);
        getServer().getPluginManager().registerEvents(mapRouteService, this);
        getServer().getPluginManager().registerEvents(mapLandmarkService, this);
        getServer().getPluginManager().registerEvents(trashBinService, this);
        getServer().getPluginManager().registerEvents(seasonProgressService, this);
        getServer().getPluginManager().registerEvents(seasonMedalService, this);
        getServer().getPluginManager().registerEvents(peaceService, this);
        getServer().getPluginManager().registerEvents(battlePassService, this);
        getServer().getPluginManager().registerEvents(questService, this);
        getServer().getPluginManager().registerEvents(duelService, this);
        getServer().getPluginManager().registerEvents(lmsService, this);
        getServer().getPluginManager().registerEvents(tournamentService, this);
        getServer().getPluginManager().registerEvents(targetEventService, this);
        getServer().getPluginManager().registerEvents(new StarterKitRespawnListener(starterKitService), this);
        getServer().getPluginManager().registerEvents(new LootPickupListener(lootProtectionService), this);
        getServer().getPluginManager().registerEvents(mapGameplayService, this);
        getServer().getPluginManager().registerEvents(spawnService, this);
        getServer().getPluginManager().registerEvents(hotZoneService, this);

        PluginCommand statsCommand = getCommand("stats");
        PluginCommand topCommand = getCommand("top");
        PluginCommand killEffectCommand = getCommand("killeffect");
        PluginCommand collectionCommand = getCommand("collection");
        PluginCommand statTrackCommand = getCommand("stattrack");
        PluginCommand mapMasteryCommand = getCommand("mapmastery");
        PluginCommand achievementsCommand = getCommand("achievements");
        PluginCommand seasonCommand = getCommand("season");
        PluginCommand pvpLevelCommand = getCommand("pvplevel");
        PluginCommand medalsCommand = getCommand("medals");
        PluginCommand seasonAdminCommand = getCommand("seasonadmin");
        PluginCommand legacyHallCommand = getCommand("legacyhall");
        PluginCommand battlePassCommand = getCommand("battlepass");
        PluginCommand questsCommand = getCommand("quests");
        PluginCommand peaceCommand = getCommand("peace");
        PluginCommand duelCommand = getCommand("duel");
        PluginCommand lmsCommand = getCommand("lms");
        PluginCommand tournamentCommand = getCommand("tournament");
        PluginCommand targetEventCommand = getCommand("targetevent");
        PluginCommand giveawayCommand = getCommand("verlosung");
        PluginCommand eventArenaCommand = getCommand("eventarena");
        PluginCommand mapSetupCommand = getCommand("mapsetup");
        PluginCommand mapLootCommand = getCommand("maploot");
        PluginCommand supplyDropCommand = getCommand("supplydrop");
        PluginCommand kingAltarCommand = getCommand("kingaltar");
        PluginCommand hotZoneCommand = getCommand("hotzone");
        PluginCommand endZoneCommand = getCommand("endzone");
        PluginCommand pvpRegionCommand = getCommand("pvpregion");
        PluginCommand secretCommand = getCommand("secret");
        PluginCommand secretRoomCommand = getCommand("secretroom");
        PluginCommand routeCommand = getCommand("route");
        PluginCommand landmarkCommand = getCommand("landmark");
        PluginCommand trashBinCommand = getCommand("trashbin");
        PluginCommand mapDisplayCommand = getCommand("mapdisplay");
        PluginCommand skyMapCommand = getCommand("skymap");
        PluginCommand spawnCommand = getCommand("spawn");
        PluginCommand setSpawnCommand = getCommand("setspawn");

        if (statsCommand == null || topCommand == null || killEffectCommand == null || collectionCommand == null
                || statTrackCommand == null || mapMasteryCommand == null || achievementsCommand == null
                || seasonCommand == null || pvpLevelCommand == null || medalsCommand == null || seasonAdminCommand == null
                || legacyHallCommand == null || battlePassCommand == null || questsCommand == null || peaceCommand == null
                || duelCommand == null || lmsCommand == null || tournamentCommand == null || targetEventCommand == null
                || giveawayCommand == null || eventArenaCommand == null || mapSetupCommand == null || mapLootCommand == null
                || supplyDropCommand == null || kingAltarCommand == null || hotZoneCommand == null || endZoneCommand == null
                || pvpRegionCommand == null || secretCommand == null || secretRoomCommand == null || routeCommand == null
                || landmarkCommand == null || trashBinCommand == null || mapDisplayCommand == null || skyMapCommand == null
                || spawnCommand == null || setSpawnCommand == null) {
            getLogger().severe("Ein SkyKings-Combat-Command fehlt in plugin.yml - Modul wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        statsCommand.setExecutor(new StatsCommand(pvpStatsService));
        TopCommand topExecutor = new TopCommand(pvpStatsService);
        topCommand.setExecutor(topExecutor);
        getServer().getPluginManager().registerEvents(topExecutor, this);
        killEffectCommand.setExecutor(new KillEffectCommand(new KillEffectGui(coreApi.getGuiManager(), killCosmeticService)));
        collectionCommand.setExecutor(new CollectionCommand(headCollectionService));
        statTrackCommand.setExecutor(new StatTrackCommand(statTrackItemService));
        mapMasteryCommand.setExecutor(new MapMasteryCommand(mapMasteryService));
        achievementsCommand.setExecutor(new AchievementsCommand(pvpStatsService, mapMasteryService));
        SeasonCommand seasonExecutor = new SeasonCommand(seasonProgressService);
        seasonCommand.setExecutor(seasonExecutor);
        pvpLevelCommand.setExecutor(seasonExecutor);
        SeasonMedalCommand medalExecutor = new SeasonMedalCommand(seasonMedalService, seasonProgressService);
        medalsCommand.setExecutor(medalExecutor);
        seasonAdminCommand.setExecutor(medalExecutor);
        legacyHallCommand.setExecutor(new LegacyHallCommand(legacyHallService));
        battlePassCommand.setExecutor(new BattlePassCommand(battlePassService));
        questsCommand.setExecutor(new QuestCommand(questService));
        peaceCommand.setExecutor(new PeaceCommand(peaceService));
        duelCommand.setExecutor(new DuelCommand(duelService));
        lmsCommand.setExecutor(lmsService);
        tournamentCommand.setExecutor(new TournamentCommand(tournamentService));
        targetEventCommand.setExecutor(new TargetEventCommand(targetEventService));
        giveawayCommand.setExecutor(new GiveawayCommand(this, coreApi.getEconomyService()));
        eventArenaCommand.setExecutor(new EventArenaCommand(eventArenaService));
        mapSetupCommand.setExecutor(new MapSetupCommand(kingAltarService, hotZoneService, endZoneService,
                secretDiscoveryService, mapLandmarkService, mapRouteService, trashBinService, mapDisplayService));
        mapLootCommand.setExecutor(mapGameplayService);
        supplyDropCommand.setExecutor(mapGameplayService);
        kingAltarCommand.setExecutor(new KingAltarCommand(kingAltarService));
        hotZoneCommand.setExecutor(new HotZoneCommand(hotZoneService));
        endZoneCommand.setExecutor(new EndZoneCommand(endZoneService));
        pvpRegionCommand.setExecutor(new PvpRegionCommand(pvpRegionService));
        secretCommand.setExecutor(new SecretCommand(secretDiscoveryService));
        secretRoomCommand.setExecutor(new SecretLootRoomCommand(secretLootRoomService));
        routeCommand.setExecutor(new MapRouteCommand(mapRouteService));
        landmarkCommand.setExecutor(new MapLandmarkCommand(mapLandmarkService));
        trashBinCommand.setExecutor(trashBinService);
        mapDisplayCommand.setExecutor(new MapDisplayCommand(mapDisplayService));
        skyMapCommand.setExecutor(new SkyMapCommand(this, spawnService));
        spawnCommand.setExecutor(spawnService);
        setSpawnCommand.setExecutor(spawnService);

        getLogger().info("SkyKingsCoreAPI gefunden: true");
        getLogger().info("SkyKings-Combat: Combat + Collection + Map + Retention + Events aktiviert.");
    }

    @Override
    public void onDisable() {
        if (targetEventService != null) targetEventService.stop(false);
        if (tournamentService != null) tournamentService.shutdown();
        if (lmsService != null) lmsService.shutdown();
        if (duelService != null) duelService.shutdown();
        if (eventArenaService != null) eventArenaService.save();
        if (seasonMedalService != null) seasonMedalService.save();
        if (legacyHallService != null) legacyHallService.save();
        if (headCollectionService != null) headCollectionService.save();
        if (pvpRegionService != null) pvpRegionService.save();
        if (secretLootRoomService != null) secretLootRoomService.save();
        if (questService != null) questService.save();
        if (battlePassService != null) battlePassService.save();
        if (peaceService != null) peaceService.save();
        if (seasonProgressService != null) seasonProgressService.save();
        if (mapDisplayService != null) mapDisplayService.shutdown();
        if (trashBinService != null) trashBinService.save();
        if (islandGameplayService != null) islandGameplayService.shutdown();
        if (mapLandmarkService != null) mapLandmarkService.save();
        if (mapRouteService != null) mapRouteService.save();
        if (secretDiscoveryService != null) secretDiscoveryService.save();
        if (endZoneService != null) endZoneService.save();
        if (mapMasteryService != null) mapMasteryService.save();
        if (kingAltarService != null) kingAltarService.save();
        if (hotZoneService != null) hotZoneService.save();
        if (spawnService != null) spawnService.shutdown();
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
            getLogger().log(Level.SEVERE, "Konnte SkyKingsCoreAPI nicht aufloesen.", t);
            return null;
        }
    }
}
