package net.skykings.core.kit;

/** Ergebnis einer Kit-Vergabe. */
public final class KitGrantResult {

    public enum Status {
        SUCCESS,
        NOT_FOUND,
        NO_PERMISSION,
        COOLDOWN,
        INVENTORY_FULL,
        PROFILE_NOT_LOADED,
        FAILED,
        REVIEW_REQUIRED
    }

    private final Status status;
    private final KitDefinition kit;
    private final long remainingMillis;

    private KitGrantResult(Status status, KitDefinition kit, long remainingMillis) {
        this.status = status;
        this.kit = kit;
        this.remainingMillis = remainingMillis;
    }

    public static KitGrantResult of(Status status, KitDefinition kit) {
        return new KitGrantResult(status, kit, 0L);
    }

    public static KitGrantResult cooldown(KitDefinition kit, long remainingMillis) {
        return new KitGrantResult(Status.COOLDOWN, kit, Math.max(0L, remainingMillis));
    }

    public Status getStatus() {
        return status;
    }

    public KitDefinition getKit() {
        return kit;
    }

    public long getRemainingMillis() {
        return remainingMillis;
    }
}
