package net.skykings.combat.kill;

import net.skykings.combat.antifarm.AntiFarmService;
import net.skykings.combat.event.SkyKingsPlayerKillEvent;
import net.skykings.combat.killstreak.KillstreakResult;
import net.skykings.combat.killstreak.KillstreakService;
import net.skykings.combat.loot.LootProtectionService;
import net.skykings.core.netherstar.NetherstarService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class CombatKillServiceImpl implements CombatKillService {

    private final KillstreakService killstreakService;
    private final AntiFarmService antiFarmService;
    @SuppressWarnings("unused")
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
        killstreakService.reset(victimUuid);

        if (killer == null || killer.getUniqueId().equals(victimUuid)) return;

        UUID killerUuid = killer.getUniqueId();
        KillstreakResult streakResult = killstreakService.recordKill(killerUuid);
        double antiFarmMultiplier = antiFarmService.registerKillAndGetMultiplier(killerUuid, victimUuid);
        long finalReward = Math.round(streakResult.getTotalReward() * antiFarmMultiplier);

        if (finalReward > 0L) {
            givePhysicalNetherstars(killer, finalReward);
            killer.sendMessage(ChatColor.DARK_AQUA + "+" + finalReward + " Netherstern"
                    + (finalReward == 1 ? "" : "e") + ChatColor.GRAY + " • Killstreak: "
                    + ChatColor.GOLD + streakResult.getNewStreak());
        }

        lootProtectionService.protectDeathDrops(victim.getLocation(), killerUuid);

        SkyKingsPlayerKillEvent killEvent = new SkyKingsPlayerKillEvent(killerUuid, victimUuid,
                streakResult.getPerKillReward(), antiFarmMultiplier, streakResult.getMilestoneBonus(),
                finalReward, streakResult.getNewStreak());
        Bukkit.getPluginManager().callEvent(killEvent);
    }

    private void givePhysicalNetherstars(Player killer, long amount) {
        long remaining = amount;
        while (remaining > 0L) {
            int stackAmount = (int) Math.min(64L, remaining);
            ItemStack stack = new ItemStack(Material.NETHER_STAR, stackAmount);
            Map<Integer, ItemStack> leftovers = killer.getInventory().addItem(stack);
            for (ItemStack leftover : leftovers.values()) {
                killer.getWorld().dropItemNaturally(killer.getLocation(), leftover);
            }
            remaining -= stackAmount;
        }
        killer.updateInventory();
    }
}
