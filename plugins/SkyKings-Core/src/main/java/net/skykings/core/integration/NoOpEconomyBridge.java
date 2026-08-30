package net.skykings.core.integration;

/** Standard-Bridge, solange keine echte Vault-Integration existiert. */
public final class NoOpEconomyBridge implements EconomyBridge {

    @Override
    public boolean isAvailable() {
        return false;
    }
}
