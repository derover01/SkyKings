package net.skykings.combat.killstreak;

/**
 * Eine Killstreak-Stufe aus config.yml: ab {@code threshold} erreichten Kills in Folge gilt
 * {@code perKill} als Netherstern-Reward pro Kill; wird {@code threshold} exakt erreicht, kommt
 * einmalig {@code milestoneBonus} hinzu (siehe Auftrag Phase 2, Abschnitt 11).
 */
public final class KillstreakTier {

    private final int threshold;
    private final long perKill;
    private final long milestoneBonus;

    public KillstreakTier(int threshold, long perKill, long milestoneBonus) {
        if (threshold < 0) {
            throw new IllegalArgumentException("threshold darf nicht negativ sein: " + threshold);
        }
        if (perKill < 0 || milestoneBonus < 0) {
            throw new IllegalArgumentException("perKill/milestoneBonus duerfen nicht negativ sein");
        }
        this.threshold = threshold;
        this.perKill = perKill;
        this.milestoneBonus = milestoneBonus;
    }

    public int getThreshold() {
        return threshold;
    }

    public long getPerKill() {
        return perKill;
    }

    public long getMilestoneBonus() {
        return milestoneBonus;
    }
}
