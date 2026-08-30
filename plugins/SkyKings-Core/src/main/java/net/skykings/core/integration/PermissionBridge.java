package net.skykings.core.integration;

/**
 * Erweiterungspunkt fuer eine externe Permission-Anbindung (z. B. LuckPerms).
 *
 * <p>Phase 1A bindet bewusst KEINE konkrete LuckPerms-Version ein (siehe Abschlussbericht,
 * "offene Punkte" - Legacy-1.8.8-kompatible Version muss erst separat recherchiert werden).
 * {@link NoOpPermissionBridge} sorgt dafuer, dass Core auch ohne LuckPerms startet.
 */
public interface PermissionBridge {

    boolean isAvailable();
}
