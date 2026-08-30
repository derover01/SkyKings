package net.skykings.core.rank;

import net.skykings.core.model.Rank;

/** Ergebnis eines /rankup-Versuchs. */
public final class RankProgressionResult {

    public enum Status {
        SUCCESS,
        MAX_FREE_RANK,
        PAID_RANK,
        INSUFFICIENT_COINS
    }

    private final Status status;
    private final Rank currentRank;
    private final Rank targetRank;
    private final long cost;

    RankProgressionResult(Status status, Rank currentRank, Rank targetRank, long cost) {
        this.status = status;
        this.currentRank = currentRank;
        this.targetRank = targetRank;
        this.cost = cost;
    }

    public Status getStatus() { return status; }
    public Rank getCurrentRank() { return currentRank; }
    public Rank getTargetRank() { return targetRank; }
    public long getCost() { return cost; }
}
