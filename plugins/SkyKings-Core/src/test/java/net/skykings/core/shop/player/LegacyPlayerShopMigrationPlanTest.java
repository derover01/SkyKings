package net.skykings.core.shop.player;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LegacyPlayerShopMigrationPlanTest {

    @Test
    public void exactLegacySalesMigrateLosslessly() {
        LegacyPlayerShopMigrationPlan plan = LegacyPlayerShopMigrationPlan.of(64, 64 * 9);
        assertTrue(plan.isMigratable());
        assertEquals(9, plan.getOffers());

        LegacyPlayerShopMigrationPlan single = LegacyPlayerShopMigrationPlan.of(128, 128);
        assertTrue(single.isMigratable());
        assertEquals(1, single.getOffers());
    }

    @Test
    public void emptyLegacyStockIsSafe() {
        LegacyPlayerShopMigrationPlan plan = LegacyPlayerShopMigrationPlan.of(64, 0);
        assertTrue(plan.isMigratable());
        assertEquals(0, plan.getOffers());
    }

    @Test
    public void partialOrOversizedLegacyStockRequiresReview() {
        assertFalse(LegacyPlayerShopMigrationPlan.of(64, 65).isMigratable());
        assertEquals("PARTIAL_LEGACY_SALE_REMAINS", LegacyPlayerShopMigrationPlan.of(64, 65).getReason());

        assertFalse(LegacyPlayerShopMigrationPlan.of(64, 64 * 10).isMigratable());
        assertEquals("TOO_MANY_LEGACY_SALES", LegacyPlayerShopMigrationPlan.of(64, 64 * 10).getReason());
    }

    @Test
    public void invalidLegacyAmountsRequireReview() {
        assertFalse(LegacyPlayerShopMigrationPlan.of(0, 64).isMigratable());
        assertFalse(LegacyPlayerShopMigrationPlan.of(129, 129).isMigratable());
        assertFalse(LegacyPlayerShopMigrationPlan.of(64, -1).isMigratable());
    }
}
