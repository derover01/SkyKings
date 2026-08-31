package net.skykings.combat.kill;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.core.ui.UiFormat;
import net.skykings.core.ui.UiTheme;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Automatische Kopfgelder fuer hohe Killstreaks mit zentralem SkyKings-Feedback. */
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
        Bukkit.broadcastMessage(UiTheme.WARNING + "Bounty active");
        Bukkit.broadcastMessage(UiTheme.TEXT + player.getName() + UiTheme.MUTED + " • Streak " + UiTheme.TEXT + streak);
        Bukkit.broadcastMessage(UiTheme.MUTED + "Reward " + UiTheme.TEXT + UiFormat.number(bounty.netherstars)
                + " SkyKings Sterne  •  " + UiTheme.TEXT + UiFormat.coins(bounty.coins));
        for (Player online : Bukkit.getOnlinePlayers()) SoundFeedback.warning(online);
    }

    public void awardStreakShutdown(Player killer, Player victim, int victimStreak, double antiFarmMultiplier) {
        Bounty base = bountyFor(victimStreak);
        if (base == null || antiFarmMultiplier <= 0.0D) return;
        long stars = Math.round(base.netherstars * antiFarmMultiplier);
        long coins = Math.round(base.coins * antiFarmMultiplier);
        if (stars > 0L) rewardDelivery.give(killer, stars);
        if (coins > 0L) economyService.deposit(killer.getUniqueId(), coins, "BOUNTY",
                "Streak-Shutdown gegen " + victim.getName() + " (" + victimStreak + ")");

        Bukkit.broadcastMessage(UiTheme.PRIMARY + "Bounty claimed");
        Bukkit.broadcastMessage(UiTheme.TEXT + killer.getName() + UiTheme.MUTED + " besiegt "
                + UiTheme.TEXT + victim.getName() + UiTheme.MUTED + " bei Streak " + UiTheme.TEXT + victimStreak);
        Bukkit.broadcastMessage(UiTheme.MUTED + "Reward " + UiTheme.TEXT + UiFormat.number(stars)
                + " SkyKings Sterne  •  " + UiTheme.TEXT + UiFormat.coins(coins));
        SoundFeedback.reward(killer);
    }

    public long getCoinBounty(int streak) {
        Bounty bounty = bountyFor(streak);
        return bounty == null ? 0L : bounty.coins;
    }

    public long getStarBounty(int streak) {
        Bounty bounty = bountyFor(streak);
        return bounty == null ? 0L : bounty.netherstars;
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
