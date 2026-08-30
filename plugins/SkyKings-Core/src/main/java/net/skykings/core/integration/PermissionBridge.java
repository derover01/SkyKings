package net.skykings.core.integration;

import net.skykings.core.model.Rank;

import java.util.UUID;

/** Einseitige SkyKings -> externe Permission-Anbindung. */
public interface PermissionBridge {

    boolean isAvailable();

    void syncRank(UUID uuid, Rank rank);

    /** Gibt einem konfigurierten Server-Owner die Owner-Gruppe und Vollzugriff. */
    void grantOwner(UUID uuid);

    /** Vergibt eine einzelne, von SkyKings freigegebene Permission-Node. */
    default void grantPermission(UUID uuid, String permission) {
        // Default fuer einfache Test-/Fallback-Bridges; echte Bridges ueberschreiben diese Methode.
    }
}
