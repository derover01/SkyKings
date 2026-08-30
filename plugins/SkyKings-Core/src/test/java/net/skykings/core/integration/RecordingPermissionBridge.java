package net.skykings.core.integration;

import net.skykings.core.model.Rank;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Test-Double: zeichnet syncRank()-Aufrufe auf, statt echtes LuckPerms anzusprechen. */
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

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void syncRank(UUID uuid, Rank rank) {
        calls.add(new SyncCall(uuid, rank));
    }

    public List<SyncCall> getCalls() {
        return calls;
    }
}
