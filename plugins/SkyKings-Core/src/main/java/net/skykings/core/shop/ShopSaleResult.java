package net.skykings.core.shop;

/** Ergebnis eines fail-closed System-Shop-Verkaufs. */
public enum ShopSaleResult {
    SUCCESS,
    INVALID_SALE,
    BALANCE_OVERFLOW,
    STALE_INVENTORY,
    REVIEW_REQUIRED,
    FAILED
}
