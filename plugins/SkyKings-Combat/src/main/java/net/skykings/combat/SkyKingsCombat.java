package net.skykings.combat;

import net.skykings.combat.antifarm.AntiFarmService;
import net.skykings.combat.antifarm.AntiFarmServiceImpl;
import net.skykings.combat.config.CombatConfig;
import net.skykings.combat.falldamage.FallDamageListener;
import net.skykings.combat.kill.CombatDeathListener;
import net.skykings.combat.kill.CombatKillService;
import net.skykings.combat.kill.CombatKillServiceImpl;
import net.skykings.combat.killstreak.KillstreakService;
import net.skykings.combat.killstreak.KillstreakServiceImpl;
import net.skykings.combat.loot.LootPickupListener;
import net.skykings.combat.loot.LootProtectionService;
import net.skykings.combat.loot.LootProtectionServiceImpl;
import net.skykings.combat.newbie.NewbieProtectionService;
import net.skykings.combat.newbie.NewbieProtectionServiceImpl;
import net.skykings.combat.pearl.EnderpearlCooldownListener;
import net.skykings.combat.pvp.PvpDamageListener;
import net.skykings.combat.starterkit.DeathStarterKit;
import net.skykings.combat.starterkit.DeathStarterKitService;
import net.skykings.combat.starterkit.DeathStarterKits;
import net.skykings.combat.starterkit.StarterKitRespawnListener;
import net.skykings.combat.tag.CombatFlyCommandListener;
import net.skykings.combat.tag.CombatTagService;
import net.skykings.combat.tag.CombatTagServiceImpl;
import net.skykings.combat.tag.LastAttackerService;
import net.skykings.combat.tag.LastAttackerServiceImpl;
import net.skykings.combat.util.MessageCooldownTracker;
import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.cooldown.CooldownService;
import net.skykings.core.netherstar.NetherstarService;
import net.skykings.core.profile.PlayerProfileService;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/** SkyKings-Combat: zentrale SkyPvP-Regeln und Combat-Schutzsysteme. */
public final class SkyKingsCombat extends JavaPlugin {

    @Override
    public void onEnable() {
        SkyKingsCoreAPI coreApi = resolveCoreApi();
        if (coreApi == null) {
            getLogger().severe("SkyKingsCoreAPI wurde nicht gefunden - SkyKings-Combat wird deaktiviert. "
                    + "Ist SkyKings-Core installiert und erfolgreich gestartet?");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        CombatConfig config = new CombatConfig(this);

        PlayerProfileService profileService = coreApi.getPlayerProfileService();
        NetherstarService netherstarService = coreApi.getNetherstarService();
        CooldownService cooldownService = coreApi.getCooldownService();

        CombatTagService combatTagService = new CombatTagServiceImpl(config.getCombatTagMillis());
        LastAttackerService lastAttackerService = new LastAttackerServiceImpl(config.getCombatTagMillis());
        NewbieProtectionService newbieProtectionService =
                new NewbieProtectionServiceImpl(profileService, config.getNewbieProtectionMillis());
        KillstreakService killstreakService =
                new KillstreakServiceImpl(config.getBaseNetherstarsPerKill(), config.getKillstreakTiers());
        AntiFarmService antiFarmService = new AntiFarmServiceImpl(config.getAntiFarmFullRewardMaxKills(),
                config.getAntiFarmHalfRewardMaxKills(), config.getAntiFarmHalfRewardMultiplier());
        LootProtectionService lootProtectionService =
                new LootProtectionServiceImpl(this, config.getLootProtectionMillis());
        CombatKillService combatKillService = new CombatKillServiceImpl(killstreakService, antiFarmService,
                netherstarService, lootProtectionService, getLogger());

        DeathStarterKit starterKit = DeathStarterKits.createDefault(config.getStarterKitGoldenApples());
        DeathStarterKitService starterKitService = new DeathStarterKitService(starterKit, config.isStarterKitEnabled());

        MessageCooldownTracker newbieFeedbackCooldown = new MessageCooldownTracker(config.getFeedbackMessageCooldownMillis());
        MessageCooldownTracker pearlFeedbackCooldown = new MessageCooldownTracker(config.getFeedbackMessageCooldownMillis());

        getServer().getPluginManager().registerEvents(new FallDamageListener(), this);
        getServer().getPluginManager().registerEvents(
                new PvpDamageListener(combatTagService, lastAttackerService, newbieProtectionService, newbieFeedbackCooldown),
                this);
        getServer().getPluginManager().registerEvents(new CombatFlyCommandListener(combatTagService), this);
        getServer().getPluginManager().registerEvents(
                new EnderpearlCooldownListener(cooldownService, config.getEnderpearlCooldownMillis(), pearlFeedbackCooldown),
                this);
        getServer().getPluginManager().registerEvents(
                new CombatDeathListener(combatKillService, combatTagService, lastAttackerService, getLogger()), this);
        getServer().getPluginManager().registerEvents(new StarterKitRespawnListener(starterKitService), this);
        getServer().getPluginManager().registerEvents(new LootPickupListener(lootProtectionService), this);

        getLogger().info("SkyKingsCoreAPI gefunden: true");
        getLogger().info("SkyKings-Combat (Phase 2 + Fly-Combat-Lock) aktiviert.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SkyKings-Combat deaktiviert.");
    }

    private SkyKingsCoreAPI resolveCoreApi() {
        try {
            RegisteredServiceProvider<SkyKingsCoreAPI> registration =
                    getServer().getServicesManager().getRegistration(SkyKingsCoreAPI.class);
            return registration != null ? registration.getProvider() : null;
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Konnte SkyKingsCoreAPI nicht aufloesen.", t);
            return null;
        }
    }
}
