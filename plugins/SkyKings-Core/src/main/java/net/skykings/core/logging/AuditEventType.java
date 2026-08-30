package net.skykings.core.logging;

/**
 * Ereignistypen fuer den Core-Audit-Log (siehe docs/GAMEPLAY.md "Logging" und
 * CLAUDE.md Punkt 10: kritische Economy-/Rang-/Permission-Aktionen muessen nachvollziehbar sein).
 */
public enum AuditEventType {
    ECONOMY_DEPOSIT,
    ECONOMY_WITHDRAW,
    ECONOMY_SET,
    NETHERSTAR_DEPOSIT,
    NETHERSTAR_WITHDRAW,
    NETHERSTAR_SET,
    RANK_CHANGE,
    PERMISSION_GRANT,
    PROFILE_CREATED
}
