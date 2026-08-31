package net.skykings.core.shop;

/** Ergebnis einer atomaren Shop-Transaktion. */
public enum ShopPurchaseResult {
    SUCCESS,
    NOT_ENOUGH_MONEY,
    NOT_ENOUGH_NETHERSTARS,
    INVENTORY_FULL,
    INVALID_OFFER,
    FAILED
}
