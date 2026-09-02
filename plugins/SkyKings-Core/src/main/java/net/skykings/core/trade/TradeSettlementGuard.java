package net.skykings.core.trade;

/** Reine Grenzwertlogik fuer Coin-Angebote und die atomare Trade-Vorpruefung. */
final class TradeSettlementGuard {
    private TradeSettlementGuard() {}

    static long adjustOffer(long current, long delta, long balance) {
        long safeBalance = Math.max(0L, balance);
        long next;
        try {
            next = Math.addExact(Math.max(0L, current), delta);
        } catch (ArithmeticException ex) {
            next = delta >= 0L ? Long.MAX_VALUE : 0L;
        }
        if (next < 0L) next = 0L;
        return Math.min(next, safeBalance);
    }

    /**
     * Prueft den finalen Kontostand nach gegenseitiger Verrechnung, ohne selbst zu addieren.
     * Erst wird der eigene Einsatz abgezogen; nur der verbleibende Headroom darf als Eingang
     * der Gegenseite hinzukommen. So kann EconomyService.deposit() spaeter nicht ueberlaufen.
     */
    static boolean canSettle(long leftBalance, long leftOutgoing, long rightBalance, long rightOutgoing) {
        if (leftBalance < 0L || rightBalance < 0L || leftOutgoing < 0L || rightOutgoing < 0L) return false;
        if (leftBalance < leftOutgoing || rightBalance < rightOutgoing) return false;

        long leftAfterDebit = leftBalance - leftOutgoing;
        long rightAfterDebit = rightBalance - rightOutgoing;
        if (rightOutgoing > Long.MAX_VALUE - leftAfterDebit) return false;
        if (leftOutgoing > Long.MAX_VALUE - rightAfterDebit) return false;
        return true;
    }
}
