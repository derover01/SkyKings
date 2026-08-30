package net.skykings.combat.killstreak;

/** Ergebnis einer Killstreak-Aktualisierung nach einem Kill (siehe {@link KillstreakService}). */
public final class KillstreakResult {

    private final int newStreak;
    private final long perKillReward;
    private final long milestoneBonus;

    public KillstreakResult(int newStreak, long perKillReward, long milestoneBonus) {
        this.newStreak = newStreak;
        this.perKillReward = perKillReward;
        this.milestoneBonus = milestoneBonus;
    }

    public int getNewStreak() {
        return newStreak;
    }

    public long getPerKillReward() {
        return perKillReward;
    }

    public long getMilestoneBonus() {
        return milestoneBonus;
    }

    public long getTotalReward() {
        return perKillReward + milestoneBonus;
    }
}
