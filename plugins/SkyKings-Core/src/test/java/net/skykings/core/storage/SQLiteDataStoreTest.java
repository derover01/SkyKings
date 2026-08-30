package net.skykings.core.storage;

import net.skykings.core.logging.AuditEvent;
import net.skykings.core.logging.AuditEventType;
import net.skykings.core.model.PlayerProfile;
import net.skykings.core.model.Rank;
import net.skykings.core.storage.sqlite.SQLiteDataStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifiziert Schema-Erzeugung und CRUD-Roundtrips gegen eine echte SQLite-Datei.
 * Dient als Ersatz fuer einen echten Client-Join (siehe Auftrag Punkt 13), der in dieser
 * Umgebung nicht automatisierbar ist.
 */
public class SQLiteDataStoreTest {

    private File databaseFile;
    private SQLiteDataStore dataStore;

    @Before
    public void setUp() throws IOException {
        databaseFile = File.createTempFile("skykings-core-test", ".db");
        databaseFile.delete();
        dataStore = new SQLiteDataStore(databaseFile, Logger.getLogger("SQLiteDataStoreTest"));
        dataStore.initialize();
    }

    @After
    public void tearDown() {
        dataStore.close();
        databaseFile.delete();
    }

    @Test
    public void profileNotFoundReturnsEmpty() {
        assertFalse(dataStore.loadProfile(UUID.randomUUID()).isPresent());
    }

    @Test
    public void profileRoundTripPreservesAllFields() {
        UUID uuid = UUID.randomUUID();
        PlayerProfile original = new PlayerProfile(uuid, "Marti", Rank.PHOENIX, 1500L, 42L, 1000L, 2000L);

        dataStore.saveProfile(original);
        Optional<PlayerProfile> loaded = dataStore.loadProfile(uuid);

        assertTrue(loaded.isPresent());
        PlayerProfile reloaded = loaded.get();
        assertEquals(uuid, reloaded.getUuid());
        assertEquals("Marti", reloaded.getLastKnownName());
        assertEquals(Rank.PHOENIX, reloaded.getRank());
        assertEquals(1500L, reloaded.getCoins());
        assertEquals(42L, reloaded.getNetherstars());
        assertEquals(1000L, reloaded.getCreatedAt());
        assertEquals(2000L, reloaded.getLastSeen());
        assertFalse("Standard-Konstruktor soll newbieProtectionDisabled=false setzen", reloaded.isNewbieProtectionDisabled());
    }

    @Test
    public void profileRoundTripPreservesNewbieProtectionDisabledFlag() {
        UUID uuid = UUID.randomUUID();
        PlayerProfile original = new PlayerProfile(uuid, "Marti", Rank.SPIELER, 0L, 0L, 1000L, 2000L, true);

        dataStore.saveProfile(original);
        PlayerProfile reloaded = dataStore.loadProfile(uuid).orElseThrow(AssertionError::new);

        assertTrue(reloaded.isNewbieProtectionDisabled());
    }

    @Test
    public void migrationAddsNewbieProtectionColumnToPreExistingDatabaseWithoutLosingData() throws Exception {
        // Simuliert eine Datenbank, die vor Phase 2 (ohne die neue Spalte) angelegt wurde.
        File legacyFile = File.createTempFile("skykings-core-legacy-test", ".db");
        legacyFile.delete();
        try {
            Class.forName("org.sqlite.JDBC");
            UUID uuid = UUID.randomUUID();
            try (Connection legacyConnection = DriverManager.getConnection("jdbc:sqlite:" + legacyFile.getAbsolutePath())) {
                try (Statement statement = legacyConnection.createStatement()) {
                    statement.executeUpdate("CREATE TABLE player_profiles ("
                            + "uuid TEXT PRIMARY KEY, last_known_name TEXT NOT NULL, rank TEXT NOT NULL, "
                            + "coins INTEGER NOT NULL DEFAULT 0, netherstars INTEGER NOT NULL DEFAULT 0, "
                            + "created_at INTEGER NOT NULL, last_seen INTEGER NOT NULL)");
                    statement.executeUpdate("INSERT INTO player_profiles "
                            + "(uuid, last_known_name, rank, coins, netherstars, created_at, last_seen) VALUES ("
                            + "'" + uuid + "', 'LegacyPlayer', 'KING', 500, 7, 111, 222)");
                }
            }

            SQLiteDataStore migrated = new SQLiteDataStore(legacyFile, Logger.getLogger("SQLiteDataStoreTest"));
            migrated.initialize();
            try {
                PlayerProfile reloaded = migrated.loadProfile(uuid).orElseThrow(AssertionError::new);
                assertEquals("LegacyPlayer", reloaded.getLastKnownName());
                assertEquals(Rank.KING, reloaded.getRank());
                assertEquals(500L, reloaded.getCoins());
                assertEquals(7L, reloaded.getNetherstars());
                assertFalse("Migrierte Alt-Zeilen muessen den sicheren Default false bekommen",
                        reloaded.isNewbieProtectionDisabled());

                // Migration muss idempotent sein (zweites initialize()/ALTER TABLE darf nicht fehlschlagen).
                reloaded.setNewbieProtectionDisabled(true);
                migrated.saveProfile(reloaded);
            } finally {
                migrated.close();
            }
        } finally {
            legacyFile.delete();
        }
    }

    @Test
    public void savingTwiceOverwritesInsteadOfDuplicating() {
        UUID uuid = UUID.randomUUID();
        dataStore.saveProfile(new PlayerProfile(uuid, "Old", Rank.SPIELER, 0L, 0L, 1L, 1L));
        dataStore.saveProfile(new PlayerProfile(uuid, "New", Rank.KING, 999L, 5L, 1L, 3L));

        PlayerProfile reloaded = dataStore.loadProfile(uuid).orElseThrow(AssertionError::new);
        assertEquals("New", reloaded.getLastKnownName());
        assertEquals(Rank.KING, reloaded.getRank());
        assertEquals(999L, reloaded.getCoins());
    }

    @Test
    public void cooldownRoundTripAndDeleteWorks() {
        UUID uuid = UUID.randomUUID();
        assertFalse(dataStore.loadCooldown(uuid, "pearl").isPresent());

        dataStore.saveCooldown(uuid, "pearl", 123456789L);
        Optional<Long> loaded = dataStore.loadCooldown(uuid, "pearl");
        assertTrue(loaded.isPresent());
        assertEquals(Long.valueOf(123456789L), loaded.get());

        dataStore.deleteCooldown(uuid, "pearl");
        assertFalse(dataStore.loadCooldown(uuid, "pearl").isPresent());
    }

    @Test
    public void loadCooldownsReturnsEmptyMapWhenNoneExist() {
        assertTrue(dataStore.loadCooldowns(UUID.randomUUID()).isEmpty());
    }

    @Test
    public void loadCooldownsReturnsAllKeysForPlayerAndIsScopedPerPlayer() {
        UUID uuid = UUID.randomUUID();
        UUID otherUuid = UUID.randomUUID();
        dataStore.saveCooldown(uuid, "pearl", 1000L);
        dataStore.saveCooldown(uuid, "kit-spieler", 2000L);
        dataStore.saveCooldown(otherUuid, "pearl", 9999L);

        java.util.Map<String, Long> loaded = dataStore.loadCooldowns(uuid);

        assertEquals(2, loaded.size());
        assertEquals(Long.valueOf(1000L), loaded.get("pearl"));
        assertEquals(Long.valueOf(2000L), loaded.get("kit-spieler"));
    }

    @Test
    public void appendAuditEventDoesNotThrow() {
        AuditEvent event = new AuditEvent(AuditEventType.ECONOMY_DEPOSIT, UUID.randomUUID(), "SYSTEM", 100L, "test");
        dataStore.appendAuditEvent(event);
    }

    @Test
    public void databasePersistsAcrossReopen() {
        UUID uuid = UUID.randomUUID();
        dataStore.saveProfile(new PlayerProfile(uuid, "Persisted", Rank.EXILE, 250L, 3L, 10L, 20L));
        dataStore.close();

        SQLiteDataStore reopened = new SQLiteDataStore(databaseFile, Logger.getLogger("SQLiteDataStoreTest"));
        reopened.initialize();
        try {
            PlayerProfile reloaded = reopened.loadProfile(uuid).orElseThrow(AssertionError::new);
            assertEquals("Persisted", reloaded.getLastKnownName());
            assertEquals(Rank.EXILE, reloaded.getRank());
        } finally {
            reopened.close();
        }
    }
}
