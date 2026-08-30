package net.skykings.combat.kill;

import net.skykings.core.economy.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/** Automatische Kopfgelder für hohe Killstreaks. */
public final class BountyService {

    private final EconomyService economyService;
    private final NetherstarRewardDelivery rewardDelivery;

    public BountyService(EconomyService economyService, NetherstarRewardDelivery rewardDelivery) {
        this.economyService = economyService;
        this.rewardDelivery = rewardDelivery;
    }

    public void announceStreak(Player player, int streak) {
        Bounty bounty = bountyFor(streak);
        if (!isMilestone(streak) || bounty == null) return;
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "⚔ KILLSTREAK ⚔");
        Bukkit.broadcastMessage(ChatColor.YELLOW + player.getName() + ChatColor.GRAY + " ist jetzt auf einer "
                + ChatColor.RED + streak + "er Killstreak" + ChatColor.GRAY + "!");
        Bukkit.broadcastMessage(ChatColor.GRAY + "Kopfgeld: " + ChatColor.AQUA + bounty.netherstars + " Nethersterne"
                + ChatColor.DARK_GRAY + " + " + ChatColor.GOLD + bounty.coins + " Coins");
        Bukkit.broadcastMessage("");
    }

    public void awardStreakShutdown(Player killer, Player victim, int victimStreak, double antiFarmMultiplier) {
        Bounty base = bountyFor(victimStreak);
        if (base == null || antiFarmMultiplier <= 0.0D) return;
        long stars = Math.round(base.netherstars * antiFarmMultiplier);
        long coins = Math.round(base.coins * antiFarmMultiplier);
        if (stars > 0L) rewardDelivery.give(killer, stars);
        if (coins > 0L) economyService.deposit(killer.getUniqueId(), coins, "BOUNTY",
                "Streak-Shutdown gegen " + victim.getName() + " (" + victimStreak + ")");

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.DARK_RED.toString() + ChatColor.BOLD + "☠ STREAK BEENDET ☠");
        Bukkit.broadcastMessage(ChatColor.GOLD + killer.getName() + ChatColor.GRAY + " hat die "
                + ChatColor.RED + victimStreak + "er Streak" + ChatColor.GRAY + " von "
                + ChatColor.YELLOW + victim.getName() + ChatColor.GRAY + " beendet!");
        Bukkit.broadcastMessage(ChatColor.GRAY + "Belohnung: " + ChatColor.AQUA + stars + " Nethersterne"
                + ChatColor.DARK_GRAY + " + " + ChatColor.GOLD + coins + " Coins");
        Bukkit.broadcastMessage("");
    }

    private boolean isMilestone(int streak) {
        return streak == 5 || streak == 10 || streak == 20 || streak == 30 || streak == 50 || streak == 100;
    }

    private Bounty bountyFor(int streak) {
        if (streak >= 100) return new Bounty(250L, 5000000L);
        if (streak >= 50) return new Bounty(100L, 1000000L);
        if (streak >= 30) return new Bounty(50L, 500000L);
        if (streak >= 20) return new Bounty(25L, 250000L);
        if (streak >= 10) return new Bounty(10L, 75000L);
        if (streak >= 5) return new Bounty(3L, 25000L);
        return null;
    }

    private static final class Bounty {
        private final long netherstars;
        private final long coins;
        private Bounty(long netherstars, long coins) {
            this.netherstars = netherstars;
            this.coins = coins;
        }
    }
}
