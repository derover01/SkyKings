package net.skykings.core.integration;

import net.skykings.core.model.Rank;

import java.util.UUID;

/**
 * Erweiterungspunkt fuer eine externe Permission-Anbindung (LuckPerms).
 *
 * <p>Richtung ist bewusst einseitig: SkyKings-Core -&gt; LuckPerms. Die interne SkyKings-
 * Datenbank bleibt die Source of Truth fuer den Rang; {@link #syncRank(UUID, Rank)} spiegelt
 * nur die aktuelle Rang-Entscheidung nach aussen (Permission-/Prefix-/Group-Layer). Es wird
 * niemals ein Rang aus LuckPerms zurueck in SkyKings uebernommen.
 *
 * <p>{@link NoOpPermissionBridge} sorgt dafuer, dass Core auch ohne LuckPerms startet und
 * {@code syncRank} dann folgenlos ist.
 */
public interface PermissionBridge {

    boolean isAvailable();

    /** Synchronisiert die LuckPerms-Gruppe eines Spielers auf den uebergebenen internen Rang. */
    void syncRank(UUID uuid, Rank rank);
}
