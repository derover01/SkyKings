package net.skykings.core.integration;

/** Standard-Bridge, solange kein Vault/VaultUnlocked verfuegbar ist. */
public final class NoOpEconomyBridge implements EconomyBridge {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public boolean isRegistered() {
        return false;
    }
}
