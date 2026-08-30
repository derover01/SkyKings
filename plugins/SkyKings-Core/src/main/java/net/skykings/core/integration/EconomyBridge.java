package net.skykings.core.integration;

/**
 * Erweiterungspunkt fuer die Registrierung von SkyKings-Coins als Vault/VaultUnlocked-
 * Economy-Provider. SkyKings-Coins ({@code EconomyService}) bleiben dabei die einzige
 * Source of Truth - diese Bridge macht sie nur zusaetzlich ueber die klassische
 * {@code net.milkbowl.vault.economy.Economy}-API fuer Fremdplugins nutzbar.
 *
 * <p>{@link NoOpEconomyBridge} sorgt dafuer, dass Core auch ohne Vault/VaultUnlocked startet.
 */
public interface EconomyBridge {

    boolean isAvailable();

    /** Ob SkyKings-Coins erfolgreich als Vault-Economy-Provider registriert wurden. */
    boolean isRegistered();
}
