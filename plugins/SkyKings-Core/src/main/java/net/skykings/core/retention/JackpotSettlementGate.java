package net.skykings.core.retention;

/** Pure fail-closed gate for jackpot settlement/recovery states. */
final class JackpotSettlementGate {
    private JackpotSettlementGate() { }

    static boolean blocks(String settlementStatus, String recoveryStatus) {
        return "PENDING".equalsIgnoreCase(normalize(settlementStatus))
                || "REVIEW_REQUIRED".equalsIgnoreCase(normalize(recoveryStatus));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
