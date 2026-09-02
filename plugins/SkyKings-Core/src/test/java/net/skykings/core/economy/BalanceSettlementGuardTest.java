package net.skykings.core.economy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BalanceSettlementGuardTest {

    @Test
    public void normalDebitAndCreditIsAllowed() {
        assertTrue(BalanceSettlementGuard.canSettle(1_000L, 100L, 500L));
        assertEquals(1_400L, BalanceSettlementGuard.settledBalance(1_000L, 100L, 500L));
    }

    @Test
    public void debitCanCreateHeadroomForLargeCredit() {
        assertTrue(BalanceSettlementGuard.canSettle(Long.MAX_VALUE - 10L, 100L, 100L));
        assertEquals(Long.MAX_VALUE - 10L,
                BalanceSettlementGuard.settledBalance(Long.MAX_VALUE - 10L, 100L, 100L));
    }

    @Test
    public void recipientOverflowIsRejectedBeforeMutation() {
        assertFalse(BalanceSettlementGuard.canSettle(Long.MAX_VALUE - 10L, 0L, 11L));
        assertFalse(BalanceSettlementGuard.canSettle(Long.MAX_VALUE - 10L, 5L, 16L));
    }

    @Test
    public void insufficientBalanceAndNegativeValuesAreRejected() {
        assertFalse(BalanceSettlementGuard.canSettle(99L, 100L, 0L));
        assertFalse(BalanceSettlementGuard.canSettle(-1L, 0L, 0L));
        assertFalse(BalanceSettlementGuard.canSettle(100L, -1L, 0L));
        assertFalse(BalanceSettlementGuard.canSettle(100L, 0L, -1L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void settledBalanceRefusesInvalidSettlement() {
        BalanceSettlementGuard.settledBalance(Long.MAX_VALUE, 0L, 1L);
    }
}
