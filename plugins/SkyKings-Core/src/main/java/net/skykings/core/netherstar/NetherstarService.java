package net.skykings.core.netherstar;

import java.util.UUID;

/**
 * Nethersterne als PvP-Waehrung (siehe docs/GAMEPLAY.md "Nethersterne"). Reiner gespeicherter
 * Zahlenwert im {@code PlayerProfile} - noch KEINE physische Item-Waehrung.
 *
 * <p>Invarianten identisch zu {@code EconomyService}: Kontostaende sind nie negativ, Ein-/
 * Auszahlungsbetraege sind immer positiv, jede Aenderung wird ueber {@code LoggingService}
 * auditierbar geloggt. Bewusst KEINE Vault-Registrierung (Nethersterne sind keine Coins).
 */
public interface NetherstarService {

    long getBalance(UUID uuid);

    boolean has(UUID uuid, long amount);

    void setBalance(UUID uuid, long amount, String actor, String reason);

    void deposit(UUID uuid, long amount, String actor, String reason);

    boolean withdraw(UUID uuid, long amount, String actor, String reason);

    /**
     * Erzwingt fuer transaktionale Systeme einen synchronen Commit des zugrunde liegenden
     * PlayerProfiles. Normale Rewards koennen weiterhin den asynchronen Save-Pfad benutzen.
     */
    default boolean persistNow(UUID uuid) {
        return true;
    }

    default void setBalance(UUID uuid, long amount) {
        setBalance(uuid, amount, "SYSTEM", null);
    }

    default void deposit(UUID uuid, long amount) {
        deposit(uuid, amount, "SYSTEM", null);
    }

    default boolean withdraw(UUID uuid, long amount) {
        return withdraw(uuid, amount, "SYSTEM", null);
    }
}
