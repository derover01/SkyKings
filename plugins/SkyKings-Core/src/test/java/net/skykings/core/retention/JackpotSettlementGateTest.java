package net.skykings.core.retention;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JackpotSettlementGateTest {

    @Test
    public void pendingSettlementBlocksNewRoundActivity() {
        assertTrue(JackpotSettlementGate.blocks("PENDING", ""));
        assertTrue(JackpotSettlementGate.blocks(" pending ", null));
    }

    @Test
    public void manualReviewBlocksNewRoundActivity() {
        assertTrue(JackpotSettlementGate.blocks("", "REVIEW_REQUIRED"));
        assertTrue(JackpotSettlementGate.blocks(null, " review_required "));
    }

    @Test
    public void clearStateAllowsRoundActivity() {
        assertFalse(JackpotSettlementGate.blocks("", ""));
        assertFalse(JackpotSettlementGate.blocks(null, null));
        assertFalse(JackpotSettlementGate.blocks("DONE", "CLEARED"));
    }
}
