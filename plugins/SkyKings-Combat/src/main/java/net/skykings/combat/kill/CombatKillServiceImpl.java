package net.skykings.combat.kill;

import net.skykings.combat.antifarm.AntiFarmService;
import net.skykings.combat.event.SkyKingsPlayerKillEvent;
import net.skykings.combat.killstreak.KillstreakResult;
import net.skykings.combat.killstreak.KillstreakService;
import net.skykings.combat.loot.LootProtectionService;
import net.skykings.core.netherstar.NetherstarOverflowException;
import net.skykings.core.netherstar.NetherstarService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CombatKillServiceImpl implements CombatKillService {

    private final KillstreakService killstreakService;
    private final AntiFarmService antiFarmService;
    private final NetherstarService netherstarService;
    private final LootProtectionService lootProtectionService;
    private final Logger logger;

    public CombatKillServiceImpl(KillstreakService killstreakService, AntiFarmService antiFarmService,
                                  NetherstarService netherstarService, LootProtectionService lootProtectionService,
                                  Logger logger) {
        this.killstreakService = killstreakService;
        this.antiFarmService = antiFarmService;
        this.netherstarService = netherstarService;
        this.lootProtectionService = lootProtectionService;
        this.logger = logger;
    }

    @Override
    public void handleDeath(Player victim, Player killer) {
        UUID victimUuid = victim.getUniqueId();

        // Killstreak des Opfers wird bei JEDEM Tod zurueckgesetzt, unabhaengig von der Todesursache.
        killstreakService.reset(victimUuid);

        if (killer == null || killer.getUniqueId().equals(victimUuid)) {
            // Kein legitimer Player-Kill (Umwelt-Tod, Suizid, o.ae.) - keine PvP-Rewards, kein
            // erfundener Killer.
            return;
        }

        UUID killerUuid = killer.getUniqueId();
        KillstreakResult streakResult = killstreakService.recordKill(killerUuid);
        double antiFarmMultiplier = antiFarmService.registerKillAndGetMultiplier(killerUuid, victimUuid);
        long finalReward = Math.round(streakResult.getTotalReward() * antiFarmMultiplier);

        if (finalReward > 0) {
            try {
                netherstarService.deposit(killerUuid, finalReward, "COMBAT",
                        "PvP-Kill (Streak " + streakResult.getNewStreak() + ")");
            } catch (NetherstarOverflowException e) {
                logger.log(Level.WARNING, "Netherstern-Obergrenze erreicht bei PvP-Kill fuer " + killerUuid
                        + " - Reward wurde nicht vergeben.", e);
            }
        }

        lootProtectionService.protectDeathDrops(victim.getLocation(), killerUuid);

        SkyKingsPlayerKillEvent killEvent = new SkyKingsPlayerKillEvent(killerUuid, victimUuid,
                streakResult.getPerKillReward(), antiFarmMultiplier, streakResult.getMilestoneBonus(),
                finalReward, streakResult.getNewStreak());
        Bukkit.getPluginManager().callEvent(killEvent);
    }
}
