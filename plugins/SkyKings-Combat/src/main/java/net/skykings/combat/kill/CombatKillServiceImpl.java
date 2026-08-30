package net.skykings.combat.kill;

import net.skykings.combat.antifarm.AntiFarmService;
import net.skykings.combat.event.SkyKingsPlayerKillEvent;
import net.skykings.combat.killstreak.KillstreakResult;
import net.skykings.combat.killstreak.KillstreakService;
import net.skykings.combat.loot.LootProtectionService;
import net.skykings.combat.stats.PvpStatsTracker;
import net.skykings.core.pvp.PvpStatsSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Logger;

public final class CombatKillServiceImpl implements CombatKillService {

    private final KillstreakService killstreakService;
    private final AntiFarmService antiFarmService;
    private final NetherstarRewardDelivery rewardDelivery;
    private final LootProtectionService lootProtectionService;
    private final PvpStatsTracker statsService;
    private final BountyService bountyService;
    private final Logger logger;

    public CombatKillServiceImpl(KillstreakService killstreakService, AntiFarmService antiFarmService,
                                  NetherstarRewardDelivery rewardDelivery, LootProtectionService lootProtectionService,
                                  PvpStatsTracker statsService, Logger logger) {
        this(killstreakService, antiFarmService, rewardDelivery, lootProtectionService, statsService, null, logger);
    }

    public CombatKillServiceImpl(KillstreakService killstreakService, AntiFarmService antiFarmService,
                                  NetherstarRewardDelivery rewardDelivery, LootProtectionService lootProtectionService,
                                  PvpStatsTracker statsService, BountyService bountyService, Logger logger) {
        this.killstreakService = killstreakService;
        this.antiFarmService = antiFarmService;
        this.rewardDelivery = rewardDelivery;
        this.lootProtectionService = lootProtectionService;
        this.statsService = statsService;
        this.bountyService = bountyService;
        this.logger = logger;
    }

    @Override
    public void handleDeath(Player victim, Player killer) {
        UUID victimUuid = victim.getUniqueId();
        PvpStatsSnapshot victimStatsBeforeDeath = statsService.getStats(victimUuid);
        int victimStreak = Math.max(killstreakService.getStreak(victimUuid), victimStatsBeforeDeath.getCurrentStreak());

        killstreakService.reset(victimUuid);
        statsService.recordDeath(victimUuid);

        if (killer == null || killer.getUniqueId().equals(victimUuid)) return;

        UUID killerUuid = killer.getUniqueId();
        if (killstreakService.getStreak(killerUuid) == 0) {
            int persistedStreak = statsService.getStats(killerUuid).getCurrentStreak();
            if (persistedStreak > 0) killstreakService.restore(killerUuid, persistedStreak);
        }

        KillstreakResult streakResult = killstreakService.recordKill(killerUuid);
        statsService.recordKill(killerUuid, streakResult.getNewStreak());

        double antiFarmMultiplier = antiFarmService.registerKillAndGetMultiplier(killerUuid, victimUuid);
        long finalReward = Math.round(streakResult.getTotalReward() * antiFarmMultiplier);

        if (finalReward > 0L) {
            rewardDelivery.give(killer, finalReward);
            killer.sendMessage(ChatColor.DARK_AQUA + "+" + finalReward + " Netherstern"
                    + (finalReward == 1 ? "" : "e") + ChatColor.GRAY + " • Killstreak: "
                    + ChatColor.GOLD + streakResult.getNewStreak());
        }

        if (bountyService != null) {
            bountyService.awardStreakShutdown(killer, victim, victimStreak, antiFarmMultiplier);
            bountyService.announceStreak(killer, streakResult.getNewStreak());
        }

        lootProtectionService.protectDeathDrops(victim.getLocation(), killerUuid);

        SkyKingsPlayerKillEvent killEvent = new SkyKingsPlayerKillEvent(killerUuid, victimUuid,
                streakResult.getPerKillReward(), antiFarmMultiplier, streakResult.getMilestoneBonus(),
                finalReward, streakResult.getNewStreak());
        Bukkit.getPluginManager().callEvent(killEvent);
    }
}
