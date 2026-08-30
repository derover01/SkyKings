package net.skykings.core.integration;

/**
 * Erweiterungspunkt fuer eine externe Economy-Bridge (z. B. Vault, damit Fremdplugins
 * SkyKings-Coins ueber die Vault-API lesen/aendern koennen).
 *
 * <p>Phase 1A bindet bewusst KEINE konkrete Vault-Version ein (siehe Abschlussbericht,
 * "offene Punkte"). {@link NoOpEconomyBridge} sorgt dafuer, dass Core auch ohne Vault startet.
 */
public interface EconomyBridge {

    boolean isAvailable();
}
