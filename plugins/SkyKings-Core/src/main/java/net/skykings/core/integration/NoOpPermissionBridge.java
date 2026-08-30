package net.skykings.core.integration;

import net.skykings.core.model.Rank;

import java.util.UUID;

/** Standard-Bridge, solange keine echte LuckPerms-Integration verfuegbar ist. */
public final class NoOpPermissionBridge implements PermissionBridge {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void syncRank(UUID uuid, Rank rank) {
        // absichtlich kein Effekt - kein LuckPerms vorhanden.
    }
}
