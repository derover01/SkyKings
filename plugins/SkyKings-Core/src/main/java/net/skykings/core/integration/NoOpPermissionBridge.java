package net.skykings.core.integration;

/** Standard-Bridge, solange keine echte LuckPerms-Integration existiert. */
public final class NoOpPermissionBridge implements PermissionBridge {

    @Override
    public boolean isAvailable() {
        return false;
    }
}
