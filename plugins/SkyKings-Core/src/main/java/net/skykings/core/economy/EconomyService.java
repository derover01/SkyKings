package net.skykings.core.economy;

import java.util.UUID;

/**
 * Coins als zentrale Hauptwaehrung (siehe docs/GAMEPLAY.md). Netherstars sind als zweite
 * Waehrung nur im Datenmodell ({@code PlayerProfile}) vorbereitet - Vergabe-/Kill-Logik
 * ist Aufgabe von SkyKings-Combat in einer spaeteren Phase.
 *
 * <p>Invarianten: Kontostaende sind nie negativ, Ein-/Auszahlungsbetraege sind immer positiv.
 * Jede Aenderung wird ueber {@code LoggingService} auditierbar geloggt.
 */
public interface EconomyService {

    long getBalance(UUID uuid);

    boolean has(UUID uuid, long amount);

    void setBalance(UUID uuid, long amount, String actor, String reason);

    void deposit(UUID uuid, long amount, String actor, String reason);

    boolean withdraw(UUID uuid, long amount, String actor, String reason);

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
