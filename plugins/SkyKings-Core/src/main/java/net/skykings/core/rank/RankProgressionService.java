package net.skykings.core.rank;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.model.Rank;

import java.util.UUID;

/** Kauft ausschliesslich den jeweils naechsten Free-Rank mit Coins. */
public final class RankProgressionService {

    private final RankService rankService;
    private final EconomyService economyService;
    private final RankProgressionConfig config;

    public RankProgressionService(RankService rankService, EconomyService economyService,
                                  RankProgressionConfig config) {
        this.rankService = rankService;
        this.economyService = economyService;
        this.config = config;
    }

    public RankProgressionResult preview(UUID uuid) {
        Rank current = rankService.getRank(uuid);
        if (current.isPaid()) {
            return new RankProgressionResult(RankProgressionResult.Status.PAID_RANK, current, null, 0L);
        }
        if (current == Rank.DIAMOND) {
            return new RankProgressionResult(RankProgressionResult.Status.MAX_FREE_RANK, current, null, 0L);
        }
        Rank target = Rank.values()[current.ordinal() + 1];
        long cost = config.getCost(target);
        RankProgressionResult.Status status = economyService.has(uuid, cost)
                ? RankProgressionResult.Status.SUCCESS
                : RankProgressionResult.Status.INSUFFICIENT_COINS;
        return new RankProgressionResult(status, current, target, cost);
    }

    public RankProgressionResult purchaseNext(UUID uuid) {
        RankProgressionResult preview = preview(uuid);
        if (preview.getStatus() != RankProgressionResult.Status.SUCCESS) {
            return preview;
        }

        long cost = preview.getCost();
        if (!economyService.withdraw(uuid, cost, "RANKUP", "Free-Rank-Kauf zu " + preview.getTargetRank())) {
            return new RankProgressionResult(RankProgressionResult.Status.INSUFFICIENT_COINS,
                    preview.getCurrentRank(), preview.getTargetRank(), cost);
        }

        try {
            rankService.setRank(uuid, preview.getTargetRank(), "RANKUP");
            return preview;
        } catch (RuntimeException rankFailure) {
            try {
                economyService.deposit(uuid, cost, "RANKUP_REFUND", "Rueckerstattung nach fehlgeschlagenem Rankup");
            } catch (RuntimeException refundFailure) {
                rankFailure.addSuppressed(refundFailure);
            }
            throw rankFailure;
        }
    }
}
