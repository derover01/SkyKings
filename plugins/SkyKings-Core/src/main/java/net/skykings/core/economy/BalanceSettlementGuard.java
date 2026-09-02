package net.skykings.core.economy;

/** Reine Grenzwertpruefung fuer nichtnegative long-Transaktionen. */
public final class BalanceSettlementGuard {
    private BalanceSettlementGuard() {}

    /** Prueft eine Addition ohne Zustand zu mutieren oder auf saturierende Werte auszuweichen. */
    public static boolean canAdd(long base, long addition) {
        return base >= 0L && addition >= 0L && addition <= Long.MAX_VALUE - base;
    }

    /**
     * Prueft, ob aus einem Kontostand zuerst debit abgezogen und danach credit addiert werden kann,
     * ohne negativen Zwischenstand oder long-Overflow. Mutiert selbst keinerlei Zustand.
     */
    public static boolean canSettle(long balance, long debit, long credit) {
        if (balance < 0L || debit < 0L || credit < 0L) return false;
        if (balance < debit) return false;
        return canAdd(balance - debit, credit);
    }

    /** Liefert den finalen Kontostand; nur nach erfolgreichem canSettle-Aufruf verwenden. */
    public static long settledBalance(long balance, long debit, long credit) {
        if (!canSettle(balance, debit, credit)) {
            throw new IllegalArgumentException("Settlement ist nicht darstellbar");
        }
        return balance - debit + credit;
    }
}
