package net.skykings.core.integration;

import net.skykings.core.model.Rank;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Test-Double fuer PermissionBridge. */
public final class RecordingPermissionBridge implements PermissionBridge {

    public static final class SyncCall {
        public final UUID uuid;
        public final Rank rank;

        SyncCall(UUID uuid, Rank rank) {
            this.uuid = uuid;
            this.rank = rank;
        }
    }

    private final List<SyncCall> calls = new ArrayList<>();
    private final List<UUID> ownerGrants = new ArrayList<>();
    private final List<String> permissionGrants = new ArrayList<>();

    @Override public boolean isAvailable() { return true; }
    @Override public void syncRank(UUID uuid, Rank rank) { calls.add(new SyncCall(uuid, rank)); }
    @Override public void grantOwner(UUID uuid) { ownerGrants.add(uuid); }
    @Override public void grantPermission(UUID uuid, String permission) { permissionGrants.add(uuid + ":" + permission); }

    public List<SyncCall> getCalls() { return calls; }
    public List<UUID> getOwnerGrants() { return ownerGrants; }
    public List<String> getPermissionGrants() { return permissionGrants; }
}
