package net.skykings.core.rank;

import net.skykings.core.economy.EconomyService;
import net.skykings.core.model.Rank;
import net.skykings.core.transaction.GameplaySettlementJournal;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/** Kauft ausschliesslich den jeweils naechsten Free-Rank mit Coins. */
public final class RankProgressionService {

    private final RankService rankService;
    private final EconomyService economyService;
    private final RankProgressionConfig config;
    private final GameplaySettlementJournal settlementJournal;

    public RankProgressionService(RankService rankService, EconomyService economyService,
                                  RankProgressionConfig config) {
        this(rankService, economyService, config, resolveJournal());
    }

    RankProgressionService(RankService rankService, EconomyService economyService,
                           RankProgressionConfig config, GameplaySettlementJournal settlementJournal) {
        this.rankService = rankService;
        this.economyService = economyService;
        this.config = config;
        this.settlementJournal = settlementJournal;
    }

    private static GameplaySettlementJournal resolveJournal() {
        try {
            GameplaySettlementJournal existing = GameplaySettlementJournal.active();
            if (existing != null) return existing;
            JavaPlugin plugin = JavaPlugin.getProvidingPlugin(RankProgressionService.class);
            return plugin == null ? null : new GameplaySettlementJournal(plugin);
        } catch (Throwable ignored) {
            return null;
        }
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

    public synchronized RankProgressionResult purchaseNext(UUID uuid) {
        if (settlementJournal != null && settlementJournal.hasPendingFor(uuid)) {
            throw new IllegalStateException("Vorherige Gameplay-Transaktion erfordert Staff-Review");
        }

        RankProgressionResult preview = preview(uuid);
        if (preview.getStatus() != RankProgressionResult.Status.SUCCESS) {
            return preview;
        }

        long cost = preview.getCost();
        UUID transaction = settlementJournal == null ? null : settlementJournal.begin(
                uuid, "RANK_PURCHASE", preview.getTargetRank().name(),
                "from=" + preview.getCurrentRank() + ", cost=" + cost);
        if (settlementJournal != null && transaction == null) {
            throw new IllegalStateException("Rank-Kauf konnte nicht sicher vorbereitet werden");
        }

        if (!economyService.withdraw(uuid, cost, "RANKUP", "Free-Rank-Kauf zu " + preview.getTargetRank())) {
            completeJournal(transaction, "WITHDRAW_REJECTED_BEFORE_MUTATION");
            return new RankProgressionResult(RankProgressionResult.Status.INSUFFICIENT_COINS,
                    preview.getCurrentRank(), preview.getTargetRank(), cost);
        }

        try {
            rankService.setRank(uuid, preview.getTargetRank(), "RANKUP");
        } catch (RuntimeException rankFailure) {
            try {
                economyService.deposit(uuid, cost, "RANKUP_REFUND", "Rueckerstattung nach fehlgeschlagenem Rankup");
                if (settlementJournal != null && !economyService.persistNow(uuid)) {
                    settlementJournal.noteFailure(transaction, "RANK_FAILURE_REFUND_DURABLE_COMMIT_FAILED");
                    throw rankFailure;
                }
                completeJournal(transaction, "RANK_FAILURE_REFUND_JOURNAL_CLOSE_FAILED");
            } catch (RuntimeException refundFailure) {
                rankFailure.addSuppressed(refundFailure);
                if (settlementJournal != null) settlementJournal.noteFailure(transaction, "RANK_FAILURE_REFUND_MUTATION_FAILED");
            }
            throw rankFailure;
        }

        if (settlementJournal != null) {
            if (!economyService.persistNow(uuid)) {
                settlementJournal.noteFailure(transaction, "FINAL_PROFILE_DURABLE_COMMIT_FAILED");
                throw new IllegalStateException("Rank-Kauf hat einen unklaren Persistenzzustand erreicht");
            }
            if (!settlementJournal.complete(transaction)) {
                settlementJournal.noteFailure(transaction, "PROFILE_COMMITTED_BUT_JOURNAL_CLOSE_FAILED");
                throw new IllegalStateException("Rank-Kauf ist gespeichert, Journal-Abschluss erfordert Review");
            }
        }
        return preview;
    }

    private void completeJournal(UUID transaction, String fallbackReason) {
        if (settlementJournal == null || transaction == null) return;
        if (!settlementJournal.complete(transaction)) settlementJournal.noteFailure(transaction, fallbackReason);
    }
}
