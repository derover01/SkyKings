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
    PROFILE_CREATED
}
