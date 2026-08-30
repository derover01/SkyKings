package net.skykings.core.pvp;

/** Immutable PvP stats snapshot exposed by the Combat module. */
public final class PvpStatsSnapshot {
    private final long kills;
    private final long deaths;
    private final int currentStreak;
    private final int bestStreak;

    public PvpStatsSnapshot(long kills, long deaths, int currentStreak, int bestStreak) {
        this.kills = Math.max(0L, kills);
        this.deaths = Math.max(0L, deaths);
        this.currentStreak = Math.max(0, currentStreak);
        this.bestStreak = Math.max(0, bestStreak);
    }

    public long getKills() { return kills; }
    public long getDeaths() { return deaths; }
    public int getCurrentStreak() { return currentStreak; }
    public int getBestStreak() { return bestStreak; }

    public double getKd() {
        return deaths <= 0L ? (double) kills : (double) kills / (double) deaths;
    }
}
