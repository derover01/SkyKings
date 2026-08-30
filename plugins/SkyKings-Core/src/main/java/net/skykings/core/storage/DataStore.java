package net.skykings.core.storage;

import net.skykings.core.logging.AuditEvent;
import net.skykings.core.model.PlayerProfile;

import java.util.Optional;
import java.util.UUID;

/**
 * Storage-Abstraktion fuer alles, was SkyKings-Core persistiert.
 *
 * <p>Phase 1A liefert nur {@code SQLiteDataStore}. Ein spaeterer {@code MySQLDataStore}
 * implementiert dasselbe Interface, sodass PlayerProfileService/RankService/EconomyService/
 * CooldownService/LoggingService unveraendert bleiben (siehe docs/ARCHITECTURE.md,
 * Datenhaltung: "produktiv MySQL/MariaDB").
 */
public interface DataStore {

    void initialize();

    void close();

    Optional<PlayerProfile> loadProfile(UUID uuid);

    void saveProfile(PlayerProfile profile);

    Optional<Long> loadCooldown(UUID uuid, String key);

    void saveCooldown(UUID uuid, String key, long expiresAtMillis);

    void deleteCooldown(UUID uuid, String key);

    void appendAuditEvent(AuditEvent event);
}
