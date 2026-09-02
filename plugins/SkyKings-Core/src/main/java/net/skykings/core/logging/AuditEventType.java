package net.skykings.core.logging;

/** Ereignistypen fuer den zentralen SkyKings-Audit-Log. */
public enum AuditEventType {
    ECONOMY_DEPOSIT,
    ECONOMY_WITHDRAW,
    ECONOMY_SET,
    NETHERSTAR_DEPOSIT,
    NETHERSTAR_WITHDRAW,
    NETHERSTAR_SET,
    RANK_CHANGE,
    PERMISSION_GRANT,
    VOUCHER_GENERATED,
    VOUCHER_REDEEMED,
    GIVEAWAY_WIN,
    SHOP_PURCHASE,
    TRADE_COMPLETE,
    PROFILE_CREATED
}
