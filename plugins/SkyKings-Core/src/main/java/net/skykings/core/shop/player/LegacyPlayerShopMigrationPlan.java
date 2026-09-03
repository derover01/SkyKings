package net.skykings.core.shop.player;

/**
 * Pure decision helper for the old PlayerShop schema.
 *
 * Old shops stored one reusable sale definition plus a total item stock. The new model stores
 * at most nine one-purchase offers. Migration is therefore only automatic when the old stock can
 * be represented exactly as 0..9 complete old sales; otherwise staff review is required.
 */
final class LegacyPlayerShopMigrationPlan {
    private final boolean migratable;
    private final int offers;
    private final String reason;

    private LegacyPlayerShopMigrationPlan(boolean migratable, int offers, String reason) {
        this.migratable = migratable;
        this.offers = offers;
        this.reason = reason;
    }

    static LegacyPlayerShopMigrationPlan of(int amountPerSale, int stock) {
        if (stock < 0) return review("NEGATIVE_STOCK");
        if (stock == 0) return new LegacyPlayerShopMigrationPlan(true, 0, "EMPTY");
        if (amountPerSale < 1 || amountPerSale > 128) return review("INVALID_AMOUNT_PER_SALE");
        if (stock % amountPerSale != 0) return review("PARTIAL_LEGACY_SALE_REMAINS");

        int offers = stock / amountPerSale;
        if (offers < 1 || offers > PlayerShop.MAX_OFFERS) return review("TOO_MANY_LEGACY_SALES");
        return new LegacyPlayerShopMigrationPlan(true, offers, "EXACT");
    }

    boolean isMigratable() { return migratable; }
    int getOffers() { return offers; }
    String getReason() { return reason; }

    private static LegacyPlayerShopMigrationPlan review(String reason) {
        return new LegacyPlayerShopMigrationPlan(false, 0, reason);
    }
}
