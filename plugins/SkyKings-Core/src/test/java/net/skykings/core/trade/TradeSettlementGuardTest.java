package net.skykings.core.trade;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TradeSettlementGuardTest {

    @Test
    public void offerIncrementSaturatesAtBalanceInsteadOfWrappingNegative() {
        assertEquals(Long.MAX_VALUE,
                TradeSettlementGuard.adjustOffer(Long.MAX_VALUE - 5L, 1_000_000L, Long.MAX_VALUE));
    }

    @Test
    public void offerDecrementNeverDropsBelowZero() {
        assertEquals(0L, TradeSettlementGuard.adjustOffer(100L, -1_000_000L, Long.MAX_VALUE));
    }

    @Test
    public void settlementAllowsIncomingCoinsWhenOutgoingCreatesEnoughHeadroom() {
        assertTrue(TradeSettlementGuard.canSettle(
                Long.MAX_VALUE - 50L, 300L,
                1_000L, 200L));
    }

    @Test
    public void settlementRejectsRecipientOverflowBeforeAnyMutation() {
        assertFalse(TradeSettlementGuard.canSettle(
                Long.MAX_VALUE - 50L, 0L,
                1_000L, 100L));
    }

    @Test
    public void settlementRejectsMissingOutgoingBalance() {
        assertFalse(TradeSettlementGuard.canSettle(99L, 100L, 1_000L, 0L));
    }
}
